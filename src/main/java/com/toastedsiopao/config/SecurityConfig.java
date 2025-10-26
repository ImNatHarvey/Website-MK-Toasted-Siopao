package com.toastedsiopao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler; // Correct import if using interface
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler; // Use the concrete class

	@Bean
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
				// --- Authorization Rules ---
				.authorizeHttpRequests(auth -> auth
						// Public access rules MUST come first
						.requestMatchers("/css/**", "/img/**", "/", "/menu", "/about", "/order", "/login", "/signup",
								"/admin/login", "/access-denied")
						.permitAll()
						// Role rules for protected areas
						.requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/u/**").hasRole("CUSTOMER")
						// Any other request must be authenticated
						.anyRequest().authenticated())
				// --- SINGLE Login Configuration ---
				.formLogin(form -> form.loginPage("/login") // Use /login as the single entry point
						.loginProcessingUrl("/login") // Spring handles POST to /login
						.failureUrl("/login?error=true").successHandler(customAuthenticationSuccessHandler) // Use our
																											// handler
						.permitAll())
				// --- Logout Configuration ---
				.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
						.logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true) // Ensure session is
																							// invalidated
						.deleteCookies("JSESSIONID") // Ensure cookie is deleted
						.permitAll())
				// --- Access Denied Handling ---
				.exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied"))
				// --- ADD CACHE CONTROL ---
				.headers(headers -> headers.cacheControl(cache -> cache.disable()) // Disable caching for secured pages
				);
		// --- END CACHE CONTROL ---

		// --- CSRF Disable (Temporary for testing) ---
		http.csrf(csrf -> csrf.disable());

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
