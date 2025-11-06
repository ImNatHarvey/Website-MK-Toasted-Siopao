package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.dto.AdminUpdateDto;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

	private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	// --- Validation Helpers ---
	private void validateUsernameDoesNotExist(String username) {
		if (userRepository.findByUsername(username).isPresent()) {
			throw new IllegalArgumentException("Username already exists: " + username);
		}
	}

	private void validateEmailDoesNotExist(String email) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("Email already exists: " + email);
		}
	}

	private void validateUsernameOnUpdate(String username, Long userId) {
		Optional<User> userWithSameUsername = userRepository.findByUsername(username);
		if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(userId)) {
			throw new IllegalArgumentException("Username '" + username + "' already exists.");
		}
	}

	private void validateEmailOnUpdate(String email, Long userId) {
		Optional<User> userWithSameEmail = userRepository.findByEmail(email);
		if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(userId)) {
			throw new IllegalArgumentException("Email '" + email + "' already exists.");
		}
	}

	private void validatePasswordConfirmation(String password, String confirmPassword) {
		if (!password.equals(confirmPassword)) {
			throw new IllegalArgumentException("Passwords do not match");
		}
	}
	// --- End Validation Helpers ---

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findUserById(Long id) {
		return userRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAllAdmins() {
		return userRepository.findByRole("ROLE_ADMIN");
	}

	@Override
	@Transactional(readOnly = true)
	public Page<User> findAllAdmins(Pageable pageable) {
		return userRepository.findByRole("ROLE_ADMIN", pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<User> searchAdmins(String keyword, Pageable pageable) {
		if (!StringUtils.hasText(keyword)) {
			return findAllAdmins(pageable);
		}
		return userRepository.findAdminsBySearchKeyword(keyword.trim(), pageable);
	}

	@Override
	public User createAccount(AdminAccountCreateDto userDto, String role) {
		validateUsernameDoesNotExist(userDto.getUsername());
		validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole(role); // Use provided role
		// Status, CreatedAt, and LastActivity are set by @PrePersist

		return userRepository.save(newUser);
	}

	@Override
	public User updateAdmin(AdminUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		if (!"ROLE_ADMIN".equals(userToUpdate.getRole())) {
			throw new IllegalArgumentException("Cannot update non-admin user with this method.");
		}

		validateUsernameOnUpdate(userDto.getUsername(), userDto.getId());
		validateEmailOnUpdate(userDto.getEmail(), userDto.getId());

		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setEmail(userDto.getEmail());
		// Password and Role are NOT updated here

		return userRepository.save(userToUpdate);
	}

	@Override
	public User updateAdminProfile(AdminUpdateDto adminDto) {
		// This method is identical to updateAdmin for now
		// We can add password changing logic here later
		return updateAdmin(adminDto);
	}

	@Override
	public void deleteAdminById(Long id) {
		// We could add logic to prevent deleting the last admin
		long adminCount = countAllAdmins();
		if (adminCount <= 1) {
			throw new RuntimeException("Cannot delete the last admin account.");
		}
		userRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public long countAllAdmins() {
		// We count all admins regardless of status
		return userRepository.countByRole("ROLE_ADMIN");
	}

	@Override
	@Transactional(readOnly = true)
	public long countActiveAdmins() {
		return userRepository.countByRoleAndStatus("ROLE_ADMIN", "ACTIVE");
	}
}