package com.toastedsiopao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.toastedsiopao.dto.UserDto;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public User saveCustomer(UserDto userDto) {
		User user = new User();
		user.setUsername(userDto.getUsername());
		// Encode the password before saving!
		user.setPassword(passwordEncoder.encode(userDto.getPassword()));
		user.setRole("ROLE_CUSTOMER"); // Assign customer role
		user.setFirstName(userDto.getFirstName());
		user.setLastName(userDto.getLastName());
		user.setPhone(userDto.getPhone());
		// You might want to add address fields from UserDto to User entity later

		return userRepository.save(user);
	}

	@Override
	public User findByUsername(String username) {
		// Return null if not found (or handle Optional differently)
		return userRepository.findByUsername(username).orElse(null);
	}
}
