package com.munichre.dfensportal.reminder.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.munichre.dfensportal.domain.model.backend.v2.DocumentProviderPermission;
import com.munichre.dfensportal.domain.model.backend.v2.FileFeed;
import com.munichre.dfensportal.domain.model.backend.v2.FileType;
import com.munichre.dfensportal.domain.model.backend.v2.SubType;
import com.munichre.dfensportal.domain.model.backend.v2.SubmissionPeriod;
import com.munichre.dfensportal.permissions.DocumentProviderPermissionService;
import com.munichre.dfensportal.permissions.exception.DocumentProviderPermissionServiceException;
import com.munichre.dfensportal.reminder.ReminderService;
import com.munichre.dfensportal.reminder.impl.dto.ReminderInfoDto;

/**
 * Default implementation of the {@link ReminderService} using the {@link ReminderService} to get the data from the
 * ProviderRight api.
 */
@Component(
    service = ReminderService.class,
    property = Constants.SERVICE_RANKING + ":Integer=10")
public class ReminderServiceImpl implements ReminderService {

  private static final Logger log = LoggerFactory.getLogger(ReminderServiceImpl.class);
  @Reference
  private DocumentProviderPermissionService documentProviderPermissionService;

  @Override
  public ReminderInfoDto getReminderForCompany(String companyId, SubType subType) {
    log.debug("SubType and  {} Company ID '{}'", subType.name(), companyId);
    LocalDate expectedDate = null;
    int lateDays;
    SubmissionPeriod oldestPeriod;
    DocumentProviderPermission companyPermission;
    try {
      List<DocumentProviderPermission> permissions = documentProviderPermissionService.getDocumentProviderPermissions(companyId, FileType.FST);
      companyPermission = permissions.stream()
          .filter(Objects::nonNull)
          .filter(
              (company) -> companyId.equals(company.getCompanyId())
                  && company.getAllowedFeeds().stream().anyMatch(feed -> subType.equals(feed.getSubType())))
          .findFirst()
          .orElse(null);
      if (companyPermission == null) {
        return null;
      }
      FileFeed sPeriod = companyPermission.getAllowedFeeds().stream()
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
      if (sPeriod == null) {
        return null;
      }
      oldestPeriod = sPeriod.getSubmissionPeriods().stream()
          .filter(Objects::nonNull)
          .sorted(Comparator.comparing(SubmissionPeriod::getPeriod))
          .findFirst()
          .orElse(null);
      if (oldestPeriod == null) {
        return null;
      }
      expectedDate = oldestPeriod.getExpectedDate();
      lateDays = calculateDelay(expectedDate);
    }
    catch (DocumentProviderPermissionServiceException ex) {
      log.error("Error while fetching the API response!", ex);
      return null;
    }
    return new ReminderInfoDto(oldestPeriod.getExpectedDate(), companyPermission.getCompanyName(), oldestPeriod.getPeriod(), lateDays);

  }

  private int calculateDelay(LocalDate expectedDate) {
    int lateDays = 0;
    LocalDate currentTime = null;
      LocalDateTime cTime = LocalDateTime.now();
      currentTime = cTime.toLocalDate();
      if (currentTime.isAfter(expectedDate)) {
        lateDays = (int)ChronoUnit.DAYS.between(expectedDate, currentTime);
        log.debug("Submission Late by  " + lateDays);
      }
    return lateDays;
  }
}
