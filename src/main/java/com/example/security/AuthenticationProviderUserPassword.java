package com.example.security;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationFailureReason;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Singleton;
import java.util.List;

/**
 * Micronaut Security 4.x replaced the old reactive, single-argument
 * AuthenticationProvider with a request-aware, generic interface.
 * HttpRequestAuthenticationProvider<B> is the imperative equivalent of the
 * Micronaut 3.x AuthenticationProvider this class used to implement.
 */
@Singleton
@Requires(property = "micronaut.security.enabled", value = "true")
public class AuthenticationProviderUserPassword<B> implements HttpRequestAuthenticationProvider<B> {

    @Override
    public AuthenticationResponse authenticate(@Nullable HttpRequest<B> requestContext,
                                                @NonNull AuthenticationRequest<String, String> authenticationRequest) {
        String username = authenticationRequest.getIdentity();
        String password = authenticationRequest.getSecret();

        // NOTE: hard-coded credentials are for local/demo use only. In production,
        // delegate to a UserRepository + password encoder (e.g. BCrypt).
        if ("admin".equals(username) && "password".equals(password)) {
            return AuthenticationResponse.success(username, List.of("ADMIN"));
        } else if ("user".equals(username) && "password".equals(password)) {
            return AuthenticationResponse.success(username, List.of("USER"));
        }

        return AuthenticationResponse.failure(AuthenticationFailureReason.CREDENTIALS_DO_NOT_MATCH);
    }
}