package com.toastedsiopao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	@Transactional // Ensures the save operation is atomic
	public User saveCustomer(UserDto userDto) {
		// Double-check username doesn't exist (though controller should also check)
		if (findByUsername(userDto.getUsername()) != null) {
			// Or throw a custom exception
			throw new IllegalArgumentException("Username already exists: " + userDto.getUsername());
		}

		User newUser = new User();
		// Basic Info
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setPhone(userDto.getPhone());
		// Password & Role
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole("ROLE_CUSTOMER"); // Assign customer role

		// --- MAP ADDRESS FIELDS ---
		newUser.setHouseNo(userDto.getHouseNo());
		newUser.setLotNo(userDto.getLotNo());
		newUser.setBlockNo(userDto.getBlockNo());
		newUser.setStreet(userDto.getStreet());
		newUser.setBarangay(userDto.getBarangay());
		newUser.setMunicipality(userDto.getMunicipality());
		newUser.setProvince(userDto.getProvince());
		// --- END MAP ADDRESS ---

		return userRepository.save(newUser);
	}

	@Override
	@Transactional(readOnly = true)
	public User findByUsername(String username) {
		return userRepository.findByUsername(username).orElse(null);
	}
}
