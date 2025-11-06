package com.toastedsiopao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.toastedsiopao.model.User;
// We don't inject UserRepository directly, we use the service
import com.toastedsiopao.service.CustomerService; // UPDATED IMPORT

import java.util.Collection;
import java.util.Collections;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	@Autowired
	private CustomerService customerService; // UPDATED INJECTION

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		System.out.println("--- Attempting to load user by username: " + username + " ---");

		if (username == null || username.contains(" ")) {
			System.out.println("--- Login failed: Username contains whitespace. ---");
			throw new BadCredentialsException("Username cannot contain spaces");
		}

		// FindByUsername now returns a User object (which can be null)
		User user = customerService.findByUsername(username); // UPDATED SERVICE CALL

		if (user == null) {
			System.out.println("--- User not found: " + username + " ---");
			throw new UsernameNotFoundException("User not found with username: " + username);
		}

		System.out.println("--- User found: " + user.getUsername() + " ---");
		System.out.println("--- Hashed Password from DB: " + user.getPassword() + " ---");
		System.out.println("--- Role from DB: " + user.getRole() + " ---");

		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
				getAuthorities(user.getRole()));
	}

	private Collection<? extends GrantedAuthority> getAuthorities(String role) {
		System.out.println("--- Assigning authority: " + role + " ---");
		return Collections.singletonList(new SimpleGrantedAuthority(role));
	}
}