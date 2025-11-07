package com.toastedsiopao.service;

// --- NEW IMPORTS ---
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// --- END NEW IMPORTS ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.toastedsiopao.model.Permission; // NEW IMPORT
import com.toastedsiopao.model.User;
// We don't inject UserRepository directly, we use the service
import com.toastedsiopao.service.CustomerService; // UPDATED IMPORT

import java.util.Collection;
import java.util.Collections;
import java.util.Set; // NEW IMPORT
import java.util.stream.Collectors; // NEW IMPORT

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	// --- NEW: Added Logger ---
	private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
	// --- END NEW ---

	@Autowired
	private CustomerService customerService; // UPDATED INJECTION

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// --- UPDATED: Use logger ---
		log.debug("--- Attempting to load user by username: {} ---", username);

		if (username == null || username.contains(" ")) {
			log.warn("--- Login failed: Username contains whitespace. ---");
			// --- END UPDATE ---
			throw new BadCredentialsException("Username cannot contain spaces");
		}

		// FindByUsername now returns a User object (which can be null)
		User user = customerService.findByUsername(username); // UPDATED SERVICE CALL

		if (user == null) {
			// --- UPDATED: Use logger ---
			log.warn("--- User not found: {} ---", username);
			// --- END UPDATE ---
			throw new UsernameNotFoundException("User not found with username: " + username);
		}

		// --- UPDATED: Use logger ---
		log.info("--- User found: {} ---", user.getUsername());
		log.debug("--- Hashed Password from DB: [PROTECTED] ---"); // Don't log password
		if (user.getRole() != null) {
			log.info("--- Role from DB: {} ---", user.getRole().getName());
		} else {
			log.warn("--- User {} has a NULL role! ---", user.getUsername());
		}
		// --- END UPDATE ---

		// --- NEW: Get authorities from Role + Permissions ---
		Collection<? extends GrantedAuthority> authorities = getAuthorities(user);
		// --- END NEW ---

		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				authorities); // UPDATED
	}

	private Collection<? extends GrantedAuthority> getAuthorities(User user) {
		if (user.getRole() == null) {
			log.error("User {} has no role! Assigning no authorities.", user.getUsername());
			return Collections.emptyList();
		}

		// 1. Get all permissions from the role
		Set<GrantedAuthority> authorities = user.getRole().getPermissions().stream()
				.map(permission -> new SimpleGrantedAuthority(permission.name())).collect(Collectors.toSet());

		// 2. Add the role name itself (e.g., "ROLE_OWNER") as an authority
		// This is best practice for Spring Security
		authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));

		log.debug("--- Assigning authorities for {}: {} ---", user.getUsername(), authorities);
		return authorities;
	}
}