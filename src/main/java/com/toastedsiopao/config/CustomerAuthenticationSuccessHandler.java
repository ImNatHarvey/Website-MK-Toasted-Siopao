package com.toastedsiopao.config;

import com.toastedsiopao.service.CustomerService; // UPDATED IMPORT
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomerAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private static final Logger log = LoggerFactory.getLogger(CustomerAuthenticationSuccessHandler.class);
	private SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	@Autowired
	private CustomerService customerService; // UPDATED INJECTION

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
		String username = authentication.getName();

		if (roles.contains("ROLE_ADMIN")) {
			log.info("Admin user {} logged in via general path. Redirecting to /admin/dashboard", username);
			// We don't track admin activity on login, only customer
			response.sendRedirect("/admin/dashboard");
		} else if (roles.contains("ROLE_CUSTOMER")) {
			log.info("Customer user {} successfully logged in via general path. Redirecting to /u/dashboard", username);

			try {
				customerService.updateLastActivity(username); // UPDATED SERVICE CALL
			} catch (Exception e) {
				log.error("Failed to update last activity for user {} on login: {}", username, e.getMessage());
			}

			response.sendRedirect("/u/dashboard");
		} else {
			log.warn(
					"User {} authenticated via general path but has no recognized role. Invalidating session and redirecting.",
					username);
			this.logoutHandler.logout(request, response, authentication);
			response.sendRedirect("/login?error=role");
		}
	}
}