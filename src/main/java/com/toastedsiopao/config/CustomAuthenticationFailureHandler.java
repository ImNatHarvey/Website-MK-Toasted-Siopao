package com.toastedsiopao.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils; // --- IMPORT ADDED ---

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    public CustomAuthenticationFailureHandler() {
        // Set a default failure URL just in case
        super("/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        // --- MODIFICATION: Build failureUrl properly ---
        String errorType = "true"; // Default fallback
        String username = request.getParameter("username");

        // Check the type of exception
        Throwable cause = exception.getCause();

        if (exception instanceof BadCredentialsException) {
            log.warn("Login failure: Bad credentials for user '{}'", username);
            errorType = "credentials";
        } else if (exception instanceof DisabledException || (cause != null && cause instanceof DisabledException)) {
            // This catches both the direct exception and the wrapped one
            log.warn("Login failure: Account for user '{}' is disabled/inactive.", username);
            errorType = "disabled";
        } else {
            // All other errors (InternalAuthenticationServiceException, etc.)
            log.error("Internal authentication error for user '{}'", username, exception);
            errorType = "internal";
        }

        // --- NEW LOGIC: Preserve the 'source' query parameter ---
        String source = request.getParameter("source");
        String failureUrl = "/login?error=" + errorType;
        
        if (StringUtils.hasText(source)) {
            failureUrl += "&source=" + source;
        }
        // --- END MODIFICATION ---

        // Use the strategy to redirect
        getRedirectStrategy().sendRedirect(request, response, failureUrl);
    }
}