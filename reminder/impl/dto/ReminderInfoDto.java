package com.munichre.dfensportal.reminder.impl.dto;

import java.time.LocalDate;

/**
 * Data transfer object for reminder info.
 */
public class ReminderInfoDto {

  private final LocalDate expectedDate;
  private final String companyName;
  private final LocalDate submittedDate;
  private int lateDays;

  /**
   * Constructor to create a valid instance of this type.
   * @param expectedDate To retrieve Expected from API
   * @param companyName To retrieve Company details from API
   * @param submittedDate To retrieve submittedDate from API
   * @param lateDays To retrieve lateDays from API
   */
  public ReminderInfoDto(LocalDate expectedDate, String companyName, LocalDate submittedDate, int lateDays) {

    this.expectedDate = expectedDate;
    this.companyName = companyName;
    this.submittedDate = submittedDate;
    this.lateDays = lateDays;
  }

  public LocalDate getExpectedDate() {
    return this.expectedDate;
  }

  public String getCompanyName() {
    return this.companyName;
  }

  public LocalDate getSubmittedDate() {
    return this.submittedDate;
  }

  public long getLateDays() {
    return this.lateDays;
  }

  public void setLateDays(long lateDays) {
    this.lateDays = (int)lateDays;
  }

}
