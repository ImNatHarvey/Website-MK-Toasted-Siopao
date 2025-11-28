package com.toastedsiopao.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;

import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);

	@Autowired
	private UserRepository userRepository;

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
			// --- UPDATED: Distinguish between Banned (INACTIVE) and Unverified (PENDING)
			// ---
			errorType = "disabled"; // Default to disabled

			if (StringUtils.hasText(username)) {
				Optional<User> userOpt = userRepository.findByUsername(username);
				if (userOpt.isPresent() && "PENDING".equals(userOpt.get().getStatus())) {
					log.warn("Login failure: User '{}' is PENDING verification.", username);
					errorType = "unverified";
				} else {
					log.warn("Login failure: Account for user '{}' is disabled/inactive.", username);
				}
			}
			// --- END UPDATE ---
		} else if (exception instanceof OAuth2AuthenticationException) {
			log.warn("OAuth2 Login failed: {}", exception.getMessage());
			errorType = "oauth_cancelled";
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