package com.toastedsiopao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException; // **** ADDED IMPORT ****
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;

import java.util.Collection;
import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// --- ADD LOGGING ---
		System.out.println("--- Attempting to load user by username: " + username + " ---");
		// --- END LOGGING ---

		// **** ADDED WHITESPACE CHECK ****
		// This prevents logins with " user " or "user name"
		if (username == null || username.contains(" ")) {
			System.out.println("--- Login failed: Username contains whitespace. ---");
			// Throw BadCredentialsException so it's handled as a generic login failure
			throw new BadCredentialsException("Username cannot contain spaces");
		}
		// **** END ADDED CHECK ****

		User user = userRepository.findByUsername(username).orElseThrow(() -> {
			// --- ADD LOGGING ---
			System.out.println("--- User not found: " + username + " ---");
			// --- END LOGGING ---
			return new UsernameNotFoundException("User not found with username: " + username);
		});

		// --- ADD LOGGING ---
		System.out.println("--- User found: " + user.getUsername() + " ---");
		System.out.println("--- Hashed Password from DB: " + user.getPassword() + " ---");
		System.out.println("--- Role from DB: " + user.getRole() + " ---");
		// --- END LOGGING ---

		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				getAuthorities(user.getRole()));
	}

	private Collection<? extends GrantedAuthority> getAuthorities(String role) {
		// --- ADD LOGGING ---
		System.out.println("--- Assigning authority: " + role + " ---");
		// --- END LOGGING ---
		return Collections.singletonList(new SimpleGrantedAuthority(role));
	}
}