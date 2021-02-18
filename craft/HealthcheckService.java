package com.munichre.dfensportal.craft;

import org.jetbrains.annotations.NotNull;

import com.munichre.dfensportal.domain.model.craft.v1.HealthcheckResponse;


public interface HealthcheckService {

  /**
   * @return Status only
   * @throws HealthCheckServiceException error executing a REST client request
   */
  @NotNull
  HealthcheckResponse healthcheck() throws HealthCheckServiceException;

}
