package com.toastedsiopao.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component // Make this a Spring bean so we can inject it
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private RequestCache requestCache = new HttpSessionRequestCache();

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		// Check if the user was trying to access a specific page before logging in
		SavedRequest savedRequest = requestCache.getRequest(request, response);
		if (savedRequest != null) {
			String targetUrl = savedRequest.getRedirectUrl();
			requestCache.removeRequest(request, response); // Clean up cache
			response.sendRedirect(targetUrl);
			return; // Exit after redirecting to original target
		}

		// If no saved request, redirect based on role
		String redirectURL = determineTargetUrl(authentication);
		response.sendRedirect(redirectURL);
	}

	protected String determineTargetUrl(Authentication authentication) {
		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

		for (GrantedAuthority grantedAuthority : authorities) {
			if (grantedAuthority.getAuthority().equals("ROLE_ADMIN")) {
				return "/admin/dashboard"; // Admin dashboard URL
			} else if (grantedAuthority.getAuthority().equals("ROLE_CUSTOMER")) {
				return "/u/dashboard"; // Customer dashboard URL
			}
		}
		// Fallback if no specific role matches (shouldn't happen with our setup)
		return "/";
	}
}
