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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.toastedsiopao.dto.CustomerCreateDto;
import com.toastedsiopao.dto.CustomerSignUpDto;
import com.toastedsiopao.dto.CustomerUpdateDto;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.Role;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.OrderRepository;
import com.toastedsiopao.repository.RoleRepository;
import com.toastedsiopao.repository.UserRepository;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

	private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

	private static final int INACTIVITY_PERIOD_MONTHS = 1;
	private static final String CUSTOMER_ROLE_NAME = "ROLE_CUSTOMER";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private Clock clock;

	@Autowired
	private UserValidationService userValidationService;

	private void validatePasswordConfirmation(String password, String confirmPassword) {
		if (!password.equals(confirmPassword)) {
			throw new IllegalArgumentException("Passwords do not match");
		}
	}

	@Override
	public User saveCustomer(CustomerSignUpDto userDto) {
		userValidationService.validateUsernameDoesNotExist(userDto.getUsername());
		userValidationService.validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());

		Role customerRole = roleRepository.findByName(CUSTOMER_ROLE_NAME)
				.orElseThrow(() -> new RuntimeException("CRITICAL: 'ROLE_CUSTOMER' not found in database."));

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPhone(userDto.getPhone());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole(customerRole);

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
	public User createCustomerFromAdmin(CustomerCreateDto userDto) {
		userValidationService.validateUsernameDoesNotExist(userDto.getUsername());
		userValidationService.validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());

		Role customerRole = roleRepository.findByName(CUSTOMER_ROLE_NAME)
				.orElseThrow(() -> new RuntimeException("CRITICAL: 'ROLE_CUSTOMER' not found in database."));

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole(customerRole);

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
		return userRepository.findByRole_Name(CUSTOMER_ROLE_NAME, pageable);
	}

	@Override
	public User updateCustomer(CustomerUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		if (userToUpdate.getRole() == null || !CUSTOMER_ROLE_NAME.equals(userToUpdate.getRole().getName())) {
			throw new IllegalArgumentException("Cannot update non-customer user with this method.");
		}

		userValidationService.validateUsernameOnUpdate(userDto.getUsername(), userDto.getId());
		userValidationService.validateEmailOnUpdate(userDto.getEmail(), userDto.getId());

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

		User user = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));

		if (user.getRole() == null || !CUSTOMER_ROLE_NAME.equals(user.getRole().getName())) {
			throw new RuntimeException("Cannot delete non-customer user with this method.");
		}

		Page<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(user, PageRequest.of(0, 1));
		if (!orders.isEmpty()) {
			throw new RuntimeException("Cannot delete customer '" + user.getUsername() + "'. They have "
					+ orders.getTotalElements() + " order(s) associated with their account.");
		}

		userRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public long countActiveCustomers() {
		return userRepository.countByRole_NameAndStatus(CUSTOMER_ROLE_NAME, "ACTIVE");
	}

	@Override
	@Transactional(readOnly = true)
	public long countInactiveCustomers() {
		return userRepository.countByRole_NameAndStatus(CUSTOMER_ROLE_NAME, "INACTIVE");
	}

	@Override
	public void updateLastActivity(String username) {
		try {
			User user = findByUsername(username);
			if (user != null) {
				user.setLastActivity(LocalDateTime.now(clock));
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

		if (user.getRole() == null || !CUSTOMER_ROLE_NAME.equals(user.getRole().getName())) {
			throw new IllegalArgumentException("Can only update status for customers.");
		}

		user.setStatus(status);
		userRepository.save(user);
		log.info("Manually updated status for user ID {} to {}", userId, status);
	}

	@Override
	public void checkForInactiveCustomers() {
		log.info("--- Running scheduled check for inactive customers... ---");
		LocalDateTime cutoffDate = LocalDateTime.now(clock).minusMonths(INACTIVITY_PERIOD_MONTHS);
		log.info("Inactivity cutoff date: {}", cutoffDate);

		List<User> activeCustomers = userRepository.findByRole_NameAndStatus(CUSTOMER_ROLE_NAME, "ACTIVE");
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

	@Override
	@Transactional(readOnly = true)
	public long countNewCustomersThisMonth() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
		return userRepository.countByRole_NameAndCreatedAtBetween(CUSTOMER_ROLE_NAME, startOfMonth, now);
	}
}