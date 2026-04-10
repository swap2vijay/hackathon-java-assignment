package com.fulfilment.application.monolith.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Readiness health check that verifies database connectivity.
 * Accessible at /q/health/ready.
 */
@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

  @Inject
  EntityManager em;

  @Override
  public HealthCheckResponse call() {
    try {
      em.createNativeQuery("SELECT 1").getSingleResult();
      return HealthCheckResponse.up("Database connection");
    } catch (Exception e) {
      return HealthCheckResponse.down("Database connection");
    }
  }
}
