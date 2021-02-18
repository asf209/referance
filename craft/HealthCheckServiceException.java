package com.munichre.dfensportal.craft;


public class HealthCheckServiceException extends RuntimeException {

  private static final long serialVersionUID = -4198170285002414127L;
  /**
   * @param message error message
   */
  public HealthCheckServiceException(String message) {
    super(message);
  }

  /**
   * @param message error message
   * @param cause error cause
   */
  public HealthCheckServiceException(String message, Throwable cause) {
    super(message, cause);
  }

}
