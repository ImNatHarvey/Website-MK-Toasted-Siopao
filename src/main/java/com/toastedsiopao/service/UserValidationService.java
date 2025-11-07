package com.toastedsiopao.service;

import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service dedicated to handling user-related validation logic, primarily
 * checking for duplicate usernames and emails.
 */
@Service
@Transactional(readOnly = true) // Most methods are read-only
public class UserValidationService {

	@Autowired
	private UserRepository userRepository;

	/**
	 * Checks if a username already exists. Throws an exception if it does. * @param
	 * username The username to check.
	 * 
	 * @throws IllegalArgumentException if the username is already taken.
	 */
	public void validateUsernameDoesNotExist(String username) {
		if (userRepository.findByUsername(username).isPresent()) {
			throw new IllegalArgumentException("Username already exists: " + username);
		}
	}

	/**
	 * Checks if an email already exists. Throws an exception if it does. * @param
	 * email The email to check.
	 * 
	 * @throws IllegalArgumentException if the email is already taken.
	 */
	public void validateEmailDoesNotExist(String email) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("Email already exists: " + email);
		}
	}

	/**
	 * Checks if a username is being updated to a value that already exists on
	 * *another* user. * @param username The new username.
	 * 
	 * @param userId The ID of the user being updated.
	 * @throws IllegalArgumentException if the username is already taken by a
	 *                                  different user.
	 */
	public void validateUsernameOnUpdate(String username, Long userId) {
		Optional<User> userWithSameUsername = userRepository.findByUsername(username);
		if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(userId)) {
			throw new IllegalArgumentException("Username '" + username + "' already exists.");
		}
	}

	/**
	 * Checks if an email is being updated to a value that already exists on
	 * *another* user. * @param email The new email.
	 * 
	 * @param userId The ID of the user being updated.
	 * @throws IllegalArgumentException if the email is already taken by a different
	 *                                  user.
	 */
	public void validateEmailOnUpdate(String email, Long userId) {
		Optional<User> userWithSameEmail = userRepository.findByEmail(email);
		if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(userId)) {
			throw new IllegalArgumentException("Email '" + email + "' already exists.");
		}
	}
}