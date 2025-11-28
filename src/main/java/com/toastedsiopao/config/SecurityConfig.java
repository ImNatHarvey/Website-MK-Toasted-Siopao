package com.toastedsiopao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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

	@Autowired
	private AuthenticationFailureHandler customAuthenticationFailureHandler;

	@Autowired
	private CustomOAuth2UserService customOAuth2UserService;

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

				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/css/**", "/img/**", "/js/**", "/img/uploads/**", "/favicon.ico", "/",
								"/about", "/login", "/access-denied", "/logout", "/forgot-password", "/reset-password",
								"/verify") // Added /verify here
						.permitAll().requestMatchers("/menu", "/order", "/signup").access((authentication, context) -> {
							boolean isAnonymous = !authentication.get().isAuthenticated() || authentication.get()
									.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ANONYMOUS"));
							boolean isCustomer = authentication.get().getAuthorities().stream()
									.anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
							return (isAnonymous || isCustomer) ? new AuthorizationDecision(true)
									: new AuthorizationDecision(false);
						})

						.requestMatchers("/admin/**").hasAuthority("VIEW_DASHBOARD").requestMatchers("/u/**")
						.hasRole("CUSTOMER")

						.anyRequest().authenticated())
				.formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
						.successHandler(customerAuthenticationSuccessHandler)
						.failureHandler(customAuthenticationFailureHandler).permitAll())

				// --- ADDED: OAuth2 Login Configuration ---
				.oauth2Login(oauth2 -> oauth2.loginPage("/login") // Reuse existing login page
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService) // Use our logic to
																									// create/load users
						).successHandler(customerAuthenticationSuccessHandler) // Redirect to dashboard/order after
																				// Google login
				)
				// ----------------------------------------

				.rememberMe(rememberMe -> rememberMe.key("a-very-secret-key-for-mk-toasted-siopao-remember-me")
						.tokenValiditySeconds(14 * 24 * 60 * 60).userDetailsService(userDetailsService)
						.rememberMeParameter("remember-me"))

				.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
						.logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true)
						.deleteCookies("JSESSIONID", "remember-me").clearAuthentication(true))
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