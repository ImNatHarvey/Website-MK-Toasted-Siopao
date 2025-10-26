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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler; // <-- IMPORT
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Autowired
	private UserDetailsService userDetailsService;

	// --- Inject the Custom Success Handler ---
	@Autowired
	private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
	// --- End Injection ---

	@Bean
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http.authorizeHttpRequests(auth -> auth
				// Public access rules...
				.requestMatchers("/css/**", "/img/**", "/", "/menu", "/about", "/order", "/login", "/signup",
						"/admin/login")
				.permitAll()
				// Role rules...
				.requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/u/**").hasRole("CUSTOMER").anyRequest()
				.authenticated())
				.formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login").failureUrl("/login?error=true")
						// --- Use the Custom Success Handler ---
						.successHandler(customAuthenticationSuccessHandler) // <-- USE HANDLER
						// --- Removed defaultSuccessUrl ---
						.permitAll())
				// Logout and Exception Handling remain the same...
				.logout(logout -> logout.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
						.logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).deleteCookies("JSESSIONID")
						.permitAll())
				.exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access-denied") // Define later
				);

		// --- CSRF Disable (Temporary for easier testing, enable in production) ---
		http.csrf(csrf -> csrf.disable()); // <-- UNCOMMENTED

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
