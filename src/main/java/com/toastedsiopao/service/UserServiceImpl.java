package com.toastedsiopao.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.toastedsiopao.dto.AdminAdminUpdateDto;
import com.toastedsiopao.dto.AdminCustomerUpdateDto;
import com.toastedsiopao.dto.AdminUserCreateDto;
import com.toastedsiopao.dto.UserDto;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	// Centralized validation for username uniqueness
	private void validateUsernameDoesNotExist(String username) {
		if (userRepository.findByUsername(username).isPresent()) {
			throw new IllegalArgumentException("Username already exists: " + username);
		}
	}

	// Centralized validation for username uniqueness during update
	private void validateUsernameOnUpdate(String username, Long userId) {
		Optional<User> userWithSameUsername = userRepository.findByUsername(username);
		if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(userId)) {
			throw new IllegalArgumentException("Username '" + username + "' already exists.");
		}
	}

	// Centralized password match validation
	private void validatePasswordConfirmation(String password, String confirmPassword) {
		if (!password.equals(confirmPassword)) {
			throw new IllegalArgumentException("Passwords do not match");
		}
	}

	@Override
	@Transactional
	public User saveCustomer(UserDto userDto) {
		// --- Moved Validations Here ---
		validateUsernameDoesNotExist(userDto.getUsername());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());
		// --- End Moved Validations ---

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setPhone(userDto.getPhone());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword())); // Encode password here
		newUser.setRole("ROLE_CUSTOMER"); // Set role explicitly

		// Map address fields
		newUser.setHouseNo(userDto.getHouseNo());
		newUser.setLotNo(userDto.getLotNo());
		newUser.setBlockNo(userDto.getBlockNo());
		newUser.setStreet(userDto.getStreet());
		newUser.setBarangay(userDto.getBarangay());
		newUser.setMunicipality(userDto.getMunicipality());
		newUser.setProvince(userDto.getProvince());

		return userRepository.save(newUser);
	}

	@Override
	@Transactional(readOnly = true)
	public User findByUsername(String username) {
		// findByUsername now returns Optional<User>
		return userRepository.findByUsername(username).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAllCustomers() {
		return userRepository.findByRole("ROLE_CUSTOMER");
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findUserById(Long id) {
		return userRepository.findById(id);
	}

	@Override
	@Transactional
	public User saveAdminUser(AdminUserCreateDto userDto, String role) {
		// --- Moved Validations Here ---
		validateUsernameDoesNotExist(userDto.getUsername());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());
		// --- End Moved Validations ---

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword())); // Encode password
		newUser.setRole(role); // Use provided role

		return userRepository.save(newUser);
	}

	@Override
	@Transactional
	public User updateCustomer(AdminCustomerUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		// Ensure the user being updated is actually a customer
		if (!"ROLE_CUSTOMER".equals(userToUpdate.getRole())) {
			throw new IllegalArgumentException("Cannot update non-customer user with this method.");
		}

		// --- Moved Validation Here ---
		validateUsernameOnUpdate(userDto.getUsername(), userDto.getId());
		// --- End Moved Validation ---

		// Map fields
		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setPhone(userDto.getPhone()); // Assumes DTO validation handles format if present
		// Map address fields
		userToUpdate.setHouseNo(userDto.getHouseNo());
		userToUpdate.setLotNo(userDto.getLotNo());
		userToUpdate.setBlockNo(userDto.getBlockNo());
		userToUpdate.setStreet(userDto.getStreet());
		userToUpdate.setBarangay(userDto.getBarangay());
		userToUpdate.setMunicipality(userDto.getMunicipality());
		userToUpdate.setProvince(userDto.getProvince());
		// Password is NOT updated here

		return userRepository.save(userToUpdate);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAllAdmins() {
		return userRepository.findByRole("ROLE_ADMIN");
	}

	@Override
	@Transactional
	public void deleteUserById(Long id) {
		// Consider adding checks here later, e.g., prevent deleting the last admin
		userRepository.deleteById(id);
	}

	@Override
	@Transactional
	public User updateAdmin(AdminAdminUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		// Ensure the user being updated is actually an admin
		if (!"ROLE_ADMIN".equals(userToUpdate.getRole())) {
			throw new IllegalArgumentException("Cannot update non-admin user with this method.");
		}

		// --- Moved Validation Here ---
		validateUsernameOnUpdate(userDto.getUsername(), userDto.getId());
		// --- End Moved Validation ---

		// Map fields
		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		// Password and Role are NOT updated here

		return userRepository.save(userToUpdate);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> searchCustomers(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return findAllCustomers();
		}
		return userRepository.findByRoleAndSearchKeyword(keyword.trim());
	}
}