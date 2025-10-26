package com.toastedsiopao.service;

import com.toastedsiopao.dto.UserDto;
import com.toastedsiopao.model.User;

public interface UserService {
	/**
	 * Saves a new customer user based on the signup DTO. Includes password encoding
	 * and role assignment.
	 * 
	 * @param userDto Data from the signup form.
	 * @return The newly saved User entity.
	 * @throws IllegalArgumentException if username already exists.
	 */
	User saveCustomer(UserDto userDto);

	/**
	 * Finds a user by their username.
	 * 
	 * @param username The username to search for.
	 * @return The User entity if found, null otherwise.
	 */
	User findByUsername(String username);
}
