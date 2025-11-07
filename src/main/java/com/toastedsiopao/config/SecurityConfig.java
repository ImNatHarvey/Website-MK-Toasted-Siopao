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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // **** KEPT IMPORT ****
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private CustomerAuthenticationSuccessHandler customerAuthenticationSuccessHandler;

	@Bean
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// **** ADDED BEAN FOR OUR NEW (CORRECT) FILTER ****
	/**
	 * Creates the custom whitespace check filter. This filter is simple and doesn't
	 * need the AuthenticationManager.
	 */
	@Bean
	public LoginWhitespaceFilter loginWhitespaceFilter() {
		return new LoginWhitespaceFilter();
	}
	// **** END ADDED BEAN ****

	// --- SINGLE Security Filter Chain ---
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, LoginWhitespaceFilter loginWhitespaceFilter)
			throws Exception { // **** INJECTED NEW FILTER ****
		http
				// **** ADDED NEW FILTER ****
				// This runs our custom parameter check *before* the real login filter
				.addFilterBefore(loginWhitespaceFilter, UsernamePasswordAuthenticationFilter.class)

				.authorizeHttpRequests(auth -> auth
						// *** START PUBLIC ACCESS RULES ***
						.requestMatchers("/css/**", // Allow CSS files
								"/img/**", // Allow Image files
								"/js/**", // **** NEW: Allow JavaScript files ****
								"/img/uploads/**", // **** NEW: Allow uploaded images ****
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
				// --- This configuration is now safe and will not be broken ---
				.formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
						.successHandler(customerAuthenticationSuccessHandler).failureUrl("/login?error=true")
						.permitAll())
				.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
						.logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).deleteCookies("JSESSIONID")
						.clearAuthentication(true))
				.exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied"))
				.headers(headers -> headers.cacheControl(cache -> cache.disable()));

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