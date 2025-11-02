package com.toastedsiopao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * A custom filter that runs before the Spring Security authentication filter.
 * It checks the raw "username" and "password" parameters for any whitespace. If
 * whitespace is found, it aborts the login attempt and redirects to the login
 * error page.
 */
public class LoginWhitespaceFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// We only care about POST requests to the /login URL
		if ("/login".equals(request.getServletPath()) && "POST".equalsIgnoreCase(request.getMethod())) {
			String username = request.getParameter("username");
			String password = request.getParameter("password");

			boolean hasWhitespace = (username != null && username.contains(" "))
					|| (password != null && password.contains(" "));

			if (hasWhitespace) {
				// If whitespace is found, reject the login
				// We redirect to the failure URL defined in SecurityConfig
				logger.warn("Login failed: Username or password contains whitespace.");
				response.sendRedirect(request.getContextPath() + "/login?error=true");
				return; // Stop the filter chain
			}
		}

		// If no whitespace (or not a login attempt), continue to the next filter
		filterChain.doFilter(request, response);
	}
}