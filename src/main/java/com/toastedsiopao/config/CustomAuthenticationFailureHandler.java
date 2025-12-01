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
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
			errorType = "disabled";

			if (StringUtils.hasText(username)) {
				Optional<User> userOpt = userRepository.findByUsername(username);
				if (userOpt.isPresent()) {
					User user = userOpt.get();
					if ("PENDING".equals(user.getStatus())) {
						log.warn("Login failure: User '{}' is PENDING verification.", username);
						errorType = "unverified";
					} else if ("DISABLED".equals(user.getStatus())) {
						log.warn("Login failure: User '{}' is explicitly DISABLED.", username);
						errorType = "account_disabled";
					} else {
						log.warn("Login failure: Account for user '{}' is inactive/disabled.", username);
						errorType = "disabled";
					}
				}
			}
		} else if (exception instanceof OAuth2AuthenticationException) {
			OAuth2AuthenticationException oauthEx = (OAuth2AuthenticationException) exception;
			OAuth2Error error = oauthEx.getError();
			log.warn("OAuth2 Login failed: {} (Error Code: {})", exception.getMessage(), error.getErrorCode());

			if ("account_disabled".equals(error.getErrorCode())) {
				errorType = "account_disabled";
			} else {
				errorType = "oauth_cancelled";
			}
		} else {
			log.error("Internal authentication error for user '{}'", username, exception);
			errorType = "internal";
		}

		String source = request.getParameter("source");
		String failureUrl = "/login?error=" + errorType;

		if (StringUtils.hasText(source)) {
			failureUrl += "&source=" + source;
		}

		// --- NEW: Pass username back if unverified so we can resend email ---
		if ("unverified".equals(errorType) && StringUtils.hasText(username)) {
			failureUrl += "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
		}
		// --------------------------------------------------------------------

		getRedirectStrategy().sendRedirect(request, response, failureUrl);
	}
}