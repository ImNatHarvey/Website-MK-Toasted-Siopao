package com.toastedsiopao.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Removed @Order import
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
// Kept CustomerAuthenticationSuccessHandler import

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

	// Inject only the Customer/RoleBased handler
	@Autowired
	private CustomerAuthenticationSuccessHandler customerAuthenticationSuccessHandler;
	// Removed AdminAuthenticationSuccessHandler injection

	@Bean
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// --- SINGLE Security Filter Chain ---
	@Bean
	// Removed @Order annotation
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// No securityMatcher needed - applies to all requests
				.authorizeHttpRequests(auth -> auth
						// Public access rules (CSS, images, public pages, logout)
						.requestMatchers("/css/**", "/img/**", "/", "/menu", "/about", "/order", "/login", "/signup",
								"/access-denied", "/logout")
						.permitAll()
						// Role rules for protected areas
						.requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/u/**").hasRole("CUSTOMER")
						// Any other request requires authentication
						.anyRequest().authenticated())
				.formLogin(form -> form // SINGLE login config
						.loginPage("/login") // Use /login as the single entry point
						.loginProcessingUrl("/login") // Spring handles POST to /login
						// Use the CUSTOMER/RoleBased success handler
						.successHandler(customerAuthenticationSuccessHandler) // <--- Use the remaining handler
						.failureUrl("/login?error=true").permitAll())
				.logout(logout -> logout // SINGLE logout config
						.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
						.logoutSuccessUrl("/login?logout=true") // Go back to customer login page
						.invalidateHttpSession(true).deleteCookies("JSESSIONID").clearAuthentication(true))
				.exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied"))
				.headers(headers -> headers.cacheControl(cache -> cache.disable()) // Keep cache control
				).csrf(csrf -> csrf.disable()); // Keep disabled for testing

		return http.build();
	}
	// --- REMOVED adminSecurityFilterChain Bean ---

	// --- Authentication Manager Configuration (remains the same) ---
	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
		return authenticationManagerBuilder.build();
	}
}
