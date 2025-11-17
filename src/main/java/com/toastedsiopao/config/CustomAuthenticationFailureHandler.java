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
import org.springframework.util.StringUtils; 

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

    public CustomAuthenticationFailureHandler() {
        super("/login?error=true");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String errorType = "true"; 
        String username = request.getParameter("username");

        Throwable cause = exception.getCause();

        if (exception instanceof BadCredentialsException) {
            log.warn("Login failure: Bad credentials for user '{}'", username);
            errorType = "credentials";
        } else if (exception instanceof DisabledException || (cause != null && cause instanceof DisabledException)) {
            log.warn("Login failure: Account for user '{}' is disabled/inactive.", username);
            errorType = "disabled";
        } else {
            log.error("Internal authentication error for user '{}'", username, exception);
            errorType = "internal";
        }

        String source = request.getParameter("source");
        String failureUrl = "/login?error=" + errorType;
        
        if (StringUtils.hasText(source)) {
            failureUrl += "&source=" + source;
        }
        
        getRedirectStrategy().sendRedirect(request, response, failureUrl);
    }
}