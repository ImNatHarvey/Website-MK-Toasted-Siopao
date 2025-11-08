package com.toastedsiopao.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class LoginWhitespaceFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if ("/login".equals(request.getServletPath()) && "POST".equalsIgnoreCase(request.getMethod())) {
			String username = request.getParameter("username");
			String password = request.getParameter("password");

			boolean hasWhitespace = (username != null && username.contains(" "))
					|| (password != null && password.contains(" "));

			if (hasWhitespace) {
				logger.warn("Login failed: Username or password contains whitespace.");
				response.sendRedirect(request.getContextPath() + "/login?error=true");
				return;
			}
		}

		filterChain.doFilter(request, response);
	}
}