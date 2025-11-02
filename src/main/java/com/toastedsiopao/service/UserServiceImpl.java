package com.toastedsiopao.service;

import java.time.LocalDateTime; // Import LocalDateTime
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
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

	// --- NEW: Logger ---
	private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	// --- NEW: Inactivity Period (1 month) ---
	private static final int INACTIVITY_PERIOD_MONTHS = 1;

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

	// Centralized validation for email uniqueness
	private void validateEmailDoesNotExist(String email) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("Email already exists: " + email);
		}
	}

	// Centralized validation for username uniqueness during update
	private void validateUsernameOnUpdate(String username, Long userId) {
		Optional<User> userWithSameUsername = userRepository.findByUsername(username);
		if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(userId)) {
			throw new IllegalArgumentException("Username '" + username + "' already exists.");
		}
	}

	// Centralized validation for email uniqueness during update
	private void validateEmailOnUpdate(String email, Long userId) {
		Optional<User> userWithSameEmail = userRepository.findByEmail(email);
		if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(userId)) {
			throw new IllegalArgumentException("Email '" + email + "' already exists.");
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
		validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());
		// --- End Moved Validations ---

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPhone(userDto.getPhone());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword())); // Encode password here
		newUser.setRole("ROLE_CUSTOMER"); // Set role explicitly
		// Note: Status, CreatedAt, and LastActivity are set by @PrePersist in User
		// model

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
		validateEmailDoesNotExist(userDto.getEmail()); // **** ADDED EMAIL CHECK ****
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());
		// --- End Moved Validations ---

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail()); // **** ADDED EMAIL MAPPING ****
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword())); // Encode password
		newUser.setRole(role); // Use provided role
		// Note: Status, CreatedAt, and LastActivity are set by @PrePersist in User
		// model
		// If it's an admin, status will default to "ACTIVE" but inactivity check won't
		// run on them.

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
		validateEmailOnUpdate(userDto.getEmail(), userDto.getId());
		// --- End Moved Validation ---

		// Map fields
		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setEmail(userDto.getEmail());
		userToUpdate.setPhone(userDto.getPhone());

		// --- NEW: Map Status ---
		if (StringUtils.hasText(userDto.getStatus())
				&& (userDto.getStatus().equals("ACTIVE") || userDto.getStatus().equals("INACTIVE"))) {
			userToUpdate.setStatus(userDto.getStatus());
		} else {
			// This should be caught by DTO validation, but as a safeguard:
			throw new IllegalArgumentException("Invalid status value provided.");
		}
		// --- END NEW ---

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
		validateEmailOnUpdate(userDto.getEmail(), userDto.getId()); // **** ADDED EMAIL UPDATE CHECK ****
		// --- End Moved Validation ---

		// Map fields
		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setEmail(userDto.getEmail()); // **** ADDED EMAIL UPDATE MAPPING ****
		// Password and Role are NOT updated here
		// Status is not managed for admins in this way

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

	// --- NEW: Implementation of activity tracking methods ---

	@Override
	@Transactional
	public void updateLastActivity(String username) {
		try {
			User user = findByUsername(username);
			if (user != null) {
				user.setLastActivity(LocalDateTime.now());
				// Also ensure they are marked ACTIVE on login
				if ("INACTIVE".equals(user.getStatus())) {
					user.setStatus("ACTIVE");
					log.info("User '{}' was INACTIVE, setting to ACTIVE upon login.", username);
				}
				userRepository.save(user);
			} else {
				log.warn("Could not update last activity: User '{}' not found.", username);
			}
		} catch (Exception e) {
			log.error("Error updating last activity for user '{}': {}", username, e.getMessage());
		}
	}

	@Override
	@Transactional
	public void updateCustomerStatus(Long userId, String status) {
		if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
			throw new IllegalArgumentException("Status must be either ACTIVE or INACTIVE.");
		}
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

		if (!"ROLE_CUSTOMER".equals(user.getRole())) {
			throw new IllegalArgumentException("Can only update status for customers.");
		}

		user.setStatus(status);
		userRepository.save(user);
		log.info("Manually updated status for user ID {} to {}", userId, status);
	}

	@Override
	@Transactional
	public void checkForInactiveCustomers() {
		log.info("--- Running scheduled check for inactive customers... ---");
		LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(INACTIVITY_PERIOD_MONTHS);
		log.info("Inactivity cutoff date: {}", cutoffDate);

		List<User> activeCustomers = userRepository.findByRoleAndStatus("ROLE_CUSTOMER", "ACTIVE");
		int markedInactive = 0;

		for (User customer : activeCustomers) {
			LocalDateTime lastActivity = customer.getLastActivity();
			if (lastActivity == null) {
				// If lastActivity is null, use createdAt as the reference
				lastActivity = customer.getCreatedAt();
			}

			if (lastActivity.isBefore(cutoffDate)) {
				customer.setStatus("INACTIVE");
				userRepository.save(customer);
				markedInactive++;
				log.info("Marked user '{}' (ID: {}) as INACTIVE. Last activity: {}", customer.getUsername(),
						customer.getId(), lastActivity);
			}
		}

		if (markedInactive > 0) {
			log.info("--- Finished inactivity check. Marked {} customer(s) as INACTIVE. ---", markedInactive);
		} else {
			log.info("--- Finished inactivity check. No customers required status change. ---");
		}
	}
	// --- END NEW ---
}