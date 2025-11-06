package com.toastedsiopao.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.toastedsiopao.dto.CustomerSignUpDto;
import com.toastedsiopao.dto.CustomerUpdateDto;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;

@Service
@Transactional // Make the whole service transactional
public class CustomerServiceImpl implements CustomerService {

	private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

	private static final int INACTIVITY_PERIOD_MONTHS = 1;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private Clock clock; // NEW: Injected clock

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
	public User saveCustomer(CustomerSignUpDto userDto) {
		validateUsernameDoesNotExist(userDto.getUsername());
		validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPhone(userDto.getPhone());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole("ROLE_CUSTOMER");

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
		return userRepository.findByUsername(username).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findUserById(Long id) {
		return userRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<User> findAllCustomers(Pageable pageable) {
		return userRepository.findByRole("ROLE_CUSTOMER", pageable);
	}

	@Override
	public User updateCustomer(CustomerUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		if (!"ROLE_CUSTOMER".equals(userToUpdate.getRole())) {
			throw new IllegalArgumentException("Cannot update non-customer user with this method.");
		}

		validateUsernameOnUpdate(userDto.getUsername(), userDto.getId());
		validateEmailOnUpdate(userDto.getEmail(), userDto.getId());

		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setEmail(userDto.getEmail());
		userToUpdate.setPhone(userDto.getPhone());

		if (StringUtils.hasText(userDto.getStatus())
				&& (userDto.getStatus().equals("ACTIVE") || userDto.getStatus().equals("INACTIVE"))) {
			userToUpdate.setStatus(userDto.getStatus());
		} else {
			throw new IllegalArgumentException("Invalid status value provided.");
		}

		userToUpdate.setHouseNo(userDto.getHouseNo());
		userToUpdate.setLotNo(userDto.getLotNo());
		userToUpdate.setBlockNo(userDto.getBlockNo());
		userToUpdate.setStreet(userDto.getStreet());
		userToUpdate.setBarangay(userDto.getBarangay());
		userToUpdate.setMunicipality(userDto.getMunicipality());
		userToUpdate.setProvince(userDto.getProvince());

		return userRepository.save(userToUpdate);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<User> searchCustomers(String keyword, Pageable pageable) {
		if (!StringUtils.hasText(keyword)) {
			return findAllCustomers(pageable);
		}
		return userRepository.findByRoleAndSearchKeyword(keyword.trim(), pageable);
	}

	@Override
	public void deleteCustomerById(Long id) {
		// We already confirmed this is a customer in the controller
		userRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public long countActiveCustomers() {
		return userRepository.countByRoleAndStatus("ROLE_CUSTOMER", "ACTIVE");
	}

	// --- Activity Tracking Methods ---

	@Override
	public void updateLastActivity(String username) {
		try {
			User user = findByUsername(username);
			if (user != null) {
				user.setLastActivity(LocalDateTime.now(clock)); // UPDATED: Use clock
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
	public void checkForInactiveCustomers() {
		log.info("--- Running scheduled check for inactive customers... ---");
		LocalDateTime cutoffDate = LocalDateTime.now(clock).minusMonths(INACTIVITY_PERIOD_MONTHS); // UPDATED: Use clock
		log.info("Inactivity cutoff date: {}", cutoffDate);

		List<User> activeCustomers = userRepository.findByRoleAndStatus("ROLE_CUSTOMER", "ACTIVE");
		int markedInactive = 0;

		for (User customer : activeCustomers) {
			LocalDateTime lastActivity = customer.getLastActivity();
			if (lastActivity == null) {
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

	// --- NEW: Dashboard Stats Implementation ---
	@Override
	@Transactional(readOnly = true)
	public long countNewCustomersThisMonth() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
		return userRepository.countByRoleAndCreatedAtBetween("ROLE_CUSTOMER", startOfMonth, now);
	}
}