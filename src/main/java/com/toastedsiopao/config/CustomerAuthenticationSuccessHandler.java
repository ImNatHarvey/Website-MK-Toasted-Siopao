package com.toastedsiopao.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler; // Import for logout
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomerAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomerAuthenticationSuccessHandler.class);
    // Helper to perform logout
    private SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        // Allow ADMINs to also log in via the customer page, redirecting them correctly.
        if (roles.contains("ROLE_ADMIN")) {
             log.info("Admin user {} logged in via general path. Redirecting to /admin/dashboard", authentication.getName());
             response.sendRedirect("/admin/dashboard");
        } else if (roles.contains("ROLE_CUSTOMER")) {
            log.info("Customer user {} successfully logged in via general path. Redirecting to /u/dashboard", authentication.getName());
            response.sendRedirect("/u/dashboard");
        } else {
            // If user has neither role (unlikely), invalidate session and redirect.
            log.warn("User {} authenticated via general path but has no recognized role. Invalidating session and redirecting.", authentication.getName());
            // --- ADD LOGOUT CALL ---
            this.logoutHandler.logout(request, response, authentication); // Perform logout
            // --- END LOGOUT CALL ---
            response.sendRedirect("/login?error=role"); // Redirect with specific error
        }
    }
}
