package com.munichre.dfensportal.craft.health;


import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.Result.Status;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.munichre.dfensportal.azure.AzureException;
import com.munichre.dfensportal.azure.auth.AzureAuthenticationException;
import com.munichre.dfensportal.azure.config.AzureConfigurationException;
import com.munichre.dfensportal.craft.HealthcheckService;
import com.munichre.dfensportal.domain.model.craft.v1.HealthcheckResponse;

/**
 * Healthcheck for the {@link HealthcheckService}.
 */
@Component(service = HealthCheck.class, property = {
    HealthCheck.NAME + "=CraftHealthCheck",
    HealthCheck.TAGS + "=craft"
})

  public class CraftHealthCheck implements HealthCheck{

  @Reference
  private HealthcheckService healthcheckService;

  @Override
  public Result execute() {
    if (healthcheckService == null) {
      return new Result(Status.HEALTH_CHECK_ERROR, "Error occured");
    }

    try {
      HealthcheckResponse statusCheck = healthcheckService.healthcheck();
      statusCheck.getMessage();
      return new Result(Status.OK, "Status 200 success'" + statusCheck.getMessage());
    }
    catch (Exception ex) {
      return evaluateCause(ex);
    }
  }

  @NotNull
  private Result evaluateCause(@NotNull Exception ex) {
    Throwable cause = ex.getCause();
    if (cause != null) {
      if (cause instanceof AzureAuthenticationException) {
        return new Result(Status.CRITICAL, "Azure authorization failed", ex);
      }
      else if (cause instanceof AzureConfigurationException) {
        return new Result(Status.CRITICAL, "Azure configuration invalid", ex);
      }
      else if (cause instanceof AzureException) {
        return new Result(Status.CRITICAL, "Azure API error", ex);
      }
    }
    return new Result(Status.CRITICAL, "error", ex);
  }
}
