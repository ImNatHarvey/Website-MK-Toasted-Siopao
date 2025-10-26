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
		http.authorizeHttpRequests(auth -> auth
				// *** START PUBLIC ACCESS RULES ***
				.requestMatchers("/css/**", // Allow CSS files
						"/img/**", // Allow Image files
						"/", // Allow Homepage
						"/menu", // Allow Public Menu
						"/about", // Allow About Us
						"/order", // Allow Public Order Page (GET)
						"/login", // Allow Login Page (GET)
						"/signup", // *** ALLOW SIGNUP PAGE (GET) ***
						"/access-denied", // Allow Access Denied Page
						"/logout" // Allow Logout URL processing
				).permitAll() // *** END PUBLIC ACCESS RULES ***

				// Role rules for protected areas
				.requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/u/**").hasRole("CUSTOMER")

				// Any other request requires authentication
				.anyRequest().authenticated())
				// --- The rest should be correct ---
				.formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
						.successHandler(customerAuthenticationSuccessHandler).failureUrl("/login?error=true")
						.permitAll())
				.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
						.logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).deleteCookies("JSESSIONID")
						.clearAuthentication(true))
				.exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied"))
				.headers(headers -> headers.cacheControl(cache -> cache.disable())).csrf(csrf -> csrf.disable()); // Keep
																													// disabled
																													// for
																													// now

		return http.build();
	}

	// --- Authentication Manager Configuration (remains the same) ---
	@Bean
	public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = http
				.getSharedObject(AuthenticationManagerBuilder.class);
		authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
		return authenticationManagerBuilder.build();
	}
}
