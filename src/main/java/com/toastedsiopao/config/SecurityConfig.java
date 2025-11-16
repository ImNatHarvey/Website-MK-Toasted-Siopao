package com.toastedsiopao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision; // --- IMPORT ADDED ---
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// --- IMPORT ADDED ---
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private CustomerAuthenticationSuccessHandler customerAuthenticationSuccessHandler;

	// --- ADDED ---
	@Autowired
	private AuthenticationFailureHandler customAuthenticationFailureHandler;
	// --- END ADDED ---

	@Bean
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public LoginWhitespaceFilter loginWhitespaceFilter() {
		return new LoginWhitespaceFilter();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, LoginWhitespaceFilter loginWhitespaceFilter)
			throws Exception {
		http.addFilterBefore(loginWhitespaceFilter, UsernamePasswordAuthenticationFilter.class)

				// --- REMOVED: .csrf(csrf -> csrf.disable()) ---
				// CSRF protection is now ENABLED by default.

				.authorizeHttpRequests(auth -> auth
						// --- MODIFIED: Split permitAll() rule ---
						// Rule 1: Static assets and core pages (login, about, home, etc.) are still permitAll
						.requestMatchers("/css/**", "/img/**", "/js/**", "/img/uploads/**", "/favicon.ico", "/",
								"/about", "/login", "/access-denied", "/logout",
								"/forgot-password", "/reset-password")
						.permitAll()

						// Rule 2: NEW RULE for pages for guests OR customers, but NOT admins
						.requestMatchers("/menu", "/order", "/signup")
						.access((authentication, context) -> {
							// Check if user is anonymous OR has CUSTOMER role
							boolean isAnonymous = !authentication.get().isAuthenticated() || authentication.get()
									.getAuthorities().stream()
									.anyMatch(a -> a.getAuthority().equals("ROLE_ANONYMOUS"));
							boolean isCustomer = authentication.get().getAuthorities().stream()
									.anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
							
							// Grant access if (anonymous OR customer), deny otherwise (i.e., for admins)
							return (isAnonymous || isCustomer) ? new AuthorizationDecision(true)
									: new AuthorizationDecision(false);
						})
						// --- END MODIFICATION ---

						.requestMatchers("/admin/**").hasAuthority("VIEW_DASHBOARD").requestMatchers("/u/**")
						.hasRole("CUSTOMER")

						.anyRequest().authenticated())
				.formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
						.successHandler(customerAuthenticationSuccessHandler)
						// --- MODIFIED ---
						.failureHandler(customAuthenticationFailureHandler)
						// --- END MODIFIED ---
						.permitAll())

				// --- MODIFICATION: Added Remember Me Configuration ---
				.rememberMe(rememberMe -> rememberMe.key("a-very-secret-key-for-mk-toasted-siopao-remember-me") // A
																												// secret
																												// key
																												// for
																												// hashing
						.tokenValiditySeconds(14 * 24 * 60 * 60) // 14 days
						.userDetailsService(userDetailsService) // Re-uses your existing user details service
						.rememberMeParameter("remember-me") // This matches the login.html form
				)
				// --- END MODIFICATION ---

				.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
						.logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true)
						.deleteCookies("JSESSIONID", "remember-me") // --- MODIFIED: Also delete remember-me cookie
						.clearAuthentication(true))
				.exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied"))
				.headers(headers -> headers.cacheControl(cache -> cache.disable()));

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
		return authenticationManagerBuilder.build();
	}
}