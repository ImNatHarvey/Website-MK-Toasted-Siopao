package com.toastedsiopao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
// --- IMPORT REMOVED ---
// import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.toastedsiopao.model.Permission;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.CustomerService;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
	private static final String CUSTOMER_ROLE_NAME = "ROLE_CUSTOMER";

	@Autowired
	private CustomerService customerService;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		log.debug("--- Attempting to load user by username: {} ---", username);

		if (username == null || username.contains(" ")) {
			log.warn("--- Login failed: Username contains whitespace. ---");
			throw new BadCredentialsException("Username cannot contain spaces");
		}

		User user = customerService.findByUsername(username);

		if (user == null) {
			log.warn("--- User not found: {} ---", username);
			throw new UsernameNotFoundException("User not found with username: " + username);
		}

		log.info("--- User found: {} ---", user.getUsername());
		log.debug("--- Hashed Password from DB: [PROTECTED] ---");
		if (user.getRole() != null) {
			log.info("--- Role from DB: {} ---", user.getRole().getName());
		} else {
			log.warn("--- User {} has a NULL role! ---", user.getUsername());
		}
		
		// --- START: MODIFICATION FOR INACTIVE ADMINS ---
		boolean enabled = true;
		String roleName = (user.getRole() != null) ? user.getRole().getName() : "";

		// Check if the user is an admin (not a customer) AND their status is INACTIVE
		if (!CUSTOMER_ROLE_NAME.equals(roleName) && "INACTIVE".equals(user.getStatus())) {
			enabled = false;
			log.warn("--- User {} is an admin and is set to INACTIVE. Marking as disabled. ---", user.getUsername());
		} else if (CUSTOMER_ROLE_NAME.equals(roleName) && "INACTIVE".equals(user.getStatus())) {
			log.info("--- Inactive customer {} logging in. Will be reactivated by success handler. ---", user.getUsername());
			// We still allow them to log in, so 'enabled' remains true.
		}
		
		Collection<? extends GrantedAuthority> authorities = getAuthorities(user);

		// Use the full constructor to pass the 'enabled' status
		// This allows DaoAuthenticationProvider to throw the DisabledException itself.
		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				enabled, true, true, true, authorities);
		// --- END: MODIFICATION FOR INACTIVE ADMINS ---
	}

	private Collection<? extends GrantedAuthority> getAuthorities(User user) {
		// --- UPDATED FOR ROLE-BASED ONLY PERMISSIONS ---
		Set<GrantedAuthority> authorities = new HashSet<>();

		// 1. Add the Role name itself (e.g., "ROLE_STAFF")
		if (user.getRole() != null) {
			authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));

			// 2. Add all permissions from the Role
			authorities.addAll(user.getRole().getPermissions().stream()
					.map(permissionString -> new SimpleGrantedAuthority(permissionString)).collect(Collectors.toSet()));
		} else {
			log.error("User {} has no role! Assigning no authorities.", user.getUsername());
		}

		// 3. REMOVED: Individual permission overrides

		log.debug("--- Assigning role-based authorities for {}: {} ---", user.getUsername(), authorities);
		return authorities;
		// --- END UPDATE ---
	}
}