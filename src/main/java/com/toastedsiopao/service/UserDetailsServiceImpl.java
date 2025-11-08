package com.toastedsiopao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
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
import java.util.HashSet; // ADDED
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

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

		Collection<? extends GrantedAuthority> authorities = getAuthorities(user);

		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				authorities);
	}

	private Collection<? extends GrantedAuthority> getAuthorities(User user) {
		// --- UPDATED FOR HYBRID PERMISSIONS ---
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

		// 3. Add all individual permission overrides from the User
		authorities.addAll(user.getPermissions().stream()
				.map(permissionString -> new SimpleGrantedAuthority(permissionString)).collect(Collectors.toSet()));

		log.debug("--- Assigning combined authorities for {}: {} ---", user.getUsername(), authorities);
		return authorities;
		// --- END UPDATE ---
	}
}