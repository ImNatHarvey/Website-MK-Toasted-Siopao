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
		
		boolean enabled = true;
		String roleName = (user.getRole() != null) ? user.getRole().getName() : "";

		if (!CUSTOMER_ROLE_NAME.equals(roleName) && "INACTIVE".equals(user.getStatus())) {
			enabled = false;
			log.warn("--- User {} is an admin and is set to INACTIVE. Marking as disabled. ---", user.getUsername());
		} else if (CUSTOMER_ROLE_NAME.equals(roleName) && "INACTIVE".equals(user.getStatus())) {
			log.info("--- Inactive customer {} logging in. Will be reactivated by success handler. ---", user.getUsername());
		}
		
		Collection<? extends GrantedAuthority> authorities = getAuthorities(user);

		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				enabled, true, true, true, authorities);
	}

	private Collection<? extends GrantedAuthority> getAuthorities(User user) {
		Set<GrantedAuthority> authorities = new HashSet<>();

		if (user.getRole() != null) {
			authorities.add(new SimpleGrantedAuthority(user.getRole().getName()));

			authorities.addAll(user.getRole().getPermissions().stream()
					.map(permissionString -> new SimpleGrantedAuthority(permissionString)).collect(Collectors.toSet()));
		} else {
			log.error("User {} has no role! Assigning no authorities.", user.getUsername());
		}

		log.debug("--- Assigning role-based authorities for {}: {} ---", user.getUsername(), authorities);
		return authorities;
	}
}