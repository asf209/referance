package com.munichre.dfensportal.reminder;

import com.munichre.dfensportal.domain.model.backend.v2.SubType;
import com.munichre.dfensportal.reminder.impl.dto.ReminderInfoDto;

/**
 * Service to load the data from ProviderRight API.
 */
public interface ReminderService {

  /**
   * Loads all company details.
   * @param companyId Company Id details.
   * @param subType SubType to filter data
   * @return reminderInfo details.
   */

  ReminderInfoDto getReminderForCompany(String companyId, SubType subType);

}
