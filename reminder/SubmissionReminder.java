package com.munichre.dfensportal.model;

import javax.annotation.PostConstruct;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.munichre.dfensportal.components.basics.UserProvider;
import com.munichre.dfensportal.domain.model.backend.v2.SubType;
import com.munichre.dfensportal.reminder.ReminderService;
import com.munichre.dfensportal.reminder.impl.dto.ReminderInfoDto;

/**
 * Model for creating Monitoring data.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class SubmissionReminder {

  private ReminderInfoDto reminderInfo;
  @OSGiService
  private ReminderService reminderService;
  @Self
  private UserProvider userProvider;
  private int lateDays;

  @PostConstruct
  private void activate() {

    if (userProvider.isFstProvider()) {
      String companyId = userProvider.getUser().getCompany().getId();
      SubType subType = SubType.ONGOING;
      reminderInfo = reminderService.getReminderForCompany(companyId, subType);
      lateDays = (int)reminderInfo.getLateDays();
    }
  }

  public int getLateDays() {
    return this.lateDays;
  }
}
