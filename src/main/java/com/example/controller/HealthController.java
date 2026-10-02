package com.example.controller;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;

import java.util.HashMap;
import java.util.Map;

/**
 * A minimal, always-anonymous liveness/readiness check for Kubernetes probes.
 * Mapped to /healthz (not /health) because /health is already served by
 * Micronaut's built-in management HealthEndpoint (enabled via
 * `endpoints.health` in application.yml) - two controllers mapped to the
 * same path is an ambiguous-route bug.
 */
@Controller("/healthz")
@Secured(SecurityRule.IS_ANONYMOUS)
public class HealthController {

    @Get(produces = MediaType.APPLICATION_JSON)
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "micronaut-demo");
        return response;
    }
}