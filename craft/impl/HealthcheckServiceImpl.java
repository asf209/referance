package com.munichre.dfensportal.craft.impl;

import java.io.UncheckedIOException;

import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.munichre.dfensportal.azure.AzureConstants;
import com.munichre.dfensportal.azure.AzureException;
import com.munichre.dfensportal.azure.auth.AzureAuthenticationException;
import com.munichre.dfensportal.azure.client.AzureResponse;
import com.munichre.dfensportal.azure.client.AzureRestClient;
import com.munichre.dfensportal.azure.client.AzureRestClientFactory;
import com.munichre.dfensportal.azure.impl.util.JsonUtil;
import com.munichre.dfensportal.craft.HealthCheckServiceException;
import com.munichre.dfensportal.craft.HealthcheckService;
import com.munichre.dfensportal.domain.model.craft.v1.HealthcheckResponse;
import com.munichre.dfensportal.util.exception.AzureClientException;

/**
 * Default implementation for {@link HealthcheckService} using the {@link AzureRestClient} to load healthCheckResponse.
 */
@Component(service = HealthcheckService.class)
public class HealthcheckServiceImpl implements HealthcheckService {
  static final String OPERATION_CRAFT_HEALTH = "healthcheck";

  @Reference
  private AzureRestClientFactory clientFactory;

  @Override
  public @NotNull HealthcheckResponse healthcheck() throws AzureClientException {
    AzureResponse response = loadResponseFromClient();
    return parseStatus(response);
  }

  @NotNull
  private AzureResponse loadResponseFromClient() throws AzureClientException {
    AzureRestClient client = createClient();
    return executeClient(client, OPERATION_CRAFT_HEALTH);
  }

  @NotNull
  private AzureRestClient createClient() throws HealthCheckServiceException {
    try {
      return clientFactory.get(AzureConstants.PRODUCT_CRAFT_BACKEND_V1);
    }
    catch (AzureAuthenticationException ex) {
      throw new HealthCheckServiceException("Azure REST client factory can't create access token to authenticate", ex);
    }
    catch (AzureException ex) {
      throw new HealthCheckServiceException("Azure REST client factory can't create default client", ex);
    }
  }

  private AzureResponse executeClient(AzureRestClient client, String operation) throws AzureClientException {
    try {
      return client.get(operation, null, null);
    }
    catch (AzureException ex) {
      throw new AzureClientException("Azure REST client can't perform GET operation  on path '" + OPERATION_CRAFT_HEALTH, ex);
    }
  }

  @NotNull
  private HealthcheckResponse parseStatus(@NotNull AzureResponse response) throws HealthCheckServiceException {
    try {
      return JsonUtil.parse(response.getData(), HealthcheckResponse.class);
    }
    catch (UncheckedIOException ex) {
      throw new HealthCheckServiceException("Can't read data from JSON", ex);
    }
  }
}
