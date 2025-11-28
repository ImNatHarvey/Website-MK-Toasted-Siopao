package com.toastedsiopao.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toastedsiopao.dto.CustomerCreateDto;
import com.toastedsiopao.dto.CustomerPasswordDto;
import com.toastedsiopao.dto.CustomerProfileDto;
import com.toastedsiopao.dto.CustomerSignUpDto;
import com.toastedsiopao.dto.CustomerUpdateDto;
import com.toastedsiopao.dto.PasswordResetDto;
import com.toastedsiopao.model.CartItem;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.Role;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.CartItemRepository;
import com.toastedsiopao.repository.OrderRepository;
import com.toastedsiopao.repository.ProductRepository;
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

	@Autowired
	private EmailService emailService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CartItemRepository cartItemRepository;

	private static class CartItemDto {
		public String name;
		public double price;
		public String image;
		public int quantity;
	}

	private void validatePasswordConfirmation(String password, String confirmPassword) {
		if (!password.equals(confirmPassword)) {
			throw new IllegalArgumentException("Passwords do not match");
		}
	}

	@Override
	public User saveCustomer(CustomerSignUpDto userDto, String siteUrl) throws Exception {
		userValidationService.validateUsernameDoesNotExist(userDto.getUsername());
		userValidationService.validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());

		// --- FIX: Auto-create role if missing instead of crashing ---
		Role customerRole = roleRepository.findByName(CUSTOMER_ROLE_NAME).orElseGet(() -> {
			log.warn("Role '{}' not found during signup. Auto-creating it.", CUSTOMER_ROLE_NAME);
			return roleRepository.save(new Role(CUSTOMER_ROLE_NAME));
		});
		// ------------------------------------------------------------

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

		newUser.setStatus("PENDING");
		String token = UUID.randomUUID().toString();
		newUser.setVerificationToken(token);

		User savedUser = userRepository.save(newUser);

		// Send Verification Email with ID and Token
		String verifyLink = siteUrl + "/verify?id=" + savedUser.getId() + "&token=" + token;
		try {
			emailService.sendVerificationEmail(savedUser, verifyLink);
		} catch (Exception e) {
			log.error("Failed to send verification email to {}", savedUser.getEmail(), e);
			throw e;
		}

		// Cart Migration Logic
		if (StringUtils.hasText(userDto.getCartDataJson())) {
			log.info("Migrating guest cart for new user: {}", savedUser.getUsername());
			try {
				TypeReference<Map<Long, CartItemDto>> typeRef = new TypeReference<>() {
				};
				Map<Long, CartItemDto> cart = objectMapper.readValue(userDto.getCartDataJson(), typeRef);

				if (!cart.isEmpty()) {
					for (Map.Entry<Long, CartItemDto> entry : cart.entrySet()) {
						Long productId = entry.getKey();
						int quantity = entry.getValue().quantity;

						Optional<Product> productOpt = productRepository.findById(productId);
						if (productOpt.isPresent() && quantity > 0) {
							Product product = productOpt.get();
							if ("ACTIVE".equals(product.getProductStatus()) && product.getCurrentStock() >= quantity) {
								Optional<CartItem> existingItemOpt = cartItemRepository.findByUserAndProduct(savedUser,
										product);
								if (existingItemOpt.isEmpty()) {
									CartItem newCartItem = new CartItem(savedUser, product, quantity);
									cartItemRepository.save(newCartItem);
									log.info("Migrated product {} (Qty: {}) to new user {}'s cart.", product.getName(),
											quantity, savedUser.getUsername());
								}
							} else {
								log.warn("Skipping migration for product ID {}: Not active or insufficient stock.",
										productId);
							}
						}
					}
				}
			} catch (Exception e) {
				log.error("Failed to migrate guest cart for user {}: {}", savedUser.getUsername(), e.getMessage(), e);
			}
		}

		return savedUser;
	}

	@Override
	public String verifyAccount(Long userId, String token) {
		Optional<User> userOpt = userRepository.findById(userId);

		if (userOpt.isEmpty()) {
			return "INVALID";
		}

		User user = userOpt.get();

		if ("ACTIVE".equals(user.getStatus())) {
			return "ALREADY_VERIFIED";
		}

		if (token != null && token.equals(user.getVerificationToken())) {
			user.setStatus("ACTIVE");
			user.setVerificationToken(null);
			// --- UPDATED: Use saveAndFlush to ensure DB is updated before redirect ---
			userRepository.saveAndFlush(user);
			log.info("User {} successfully verified via email.", user.getUsername());
			return "SUCCESS";
		}

		return "INVALID";
	}

	@Override
	public User createCustomerFromAdmin(CustomerCreateDto userDto) {
		userValidationService.validateUsernameDoesNotExist(userDto.getUsername());
		userValidationService.validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());

		// --- FIX: Auto-create role if missing ---
		Role customerRole = roleRepository.findByName(CUSTOMER_ROLE_NAME).orElseGet(() -> {
			log.warn("Role '{}' not found during admin creation. Auto-creating it.", CUSTOMER_ROLE_NAME);
			return roleRepository.save(new Role(CUSTOMER_ROLE_NAME));
		});
		// ----------------------------------------

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole(customerRole);

		newUser.setStatus("ACTIVE");

		return userRepository.save(newUser);
	}

// ... (Rest of the methods remain exactly as they were) ...
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
		cartItemRepository.deleteByUser(user);
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
				}
				userRepository.save(user);
			}
		} catch (Exception e) {
			log.error("Error updating last activity", e);
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
	}

	@Override
	public void checkForInactiveCustomers() {
		LocalDateTime cutoffDate = LocalDateTime.now(clock).minusMonths(INACTIVITY_PERIOD_MONTHS);
		List<User> activeCustomers = userRepository.findByRole_NameAndStatus(CUSTOMER_ROLE_NAME, "ACTIVE");

		for (User customer : activeCustomers) {
			LocalDateTime lastActivity = customer.getLastActivity();
			if (lastActivity == null) {
				lastActivity = customer.getCreatedAt();
			}

			if (lastActivity.isBefore(cutoffDate)) {
				customer.setStatus("INACTIVE");
				userRepository.save(customer);
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public long countNewCustomersThisMonth() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
		return userRepository.countByRole_NameAndCreatedAtBetween(CUSTOMER_ROLE_NAME, startOfMonth, now);
	}

	@Override
	public void processPasswordForgotRequest(String email, String resetUrlBase) throws Exception {
		Optional<User> userOpt = userRepository.findByEmail(email);
		if (userOpt.isEmpty())
			return;

		User user = userOpt.get();
		String token = UUID.randomUUID().toString();

		user.setResetPasswordToken(token);
		user.setResetPasswordTokenExpiry(LocalDateTime.now(clock).plusHours(1));
		userRepository.save(user);

		String resetUrl = resetUrlBase + "/reset-password?token=" + token;
		emailService.sendPasswordResetEmail(user, token, resetUrl);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean validatePasswordResetToken(String token) {
		if (!StringUtils.hasText(token))
			return false;

		Optional<User> userOpt = userRepository.findByResetPasswordToken(token);
		if (userOpt.isEmpty())
			return false;

		User user = userOpt.get();
		return !user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now(clock));
	}

	@Override
	public void resetPassword(PasswordResetDto resetDto) {
		validatePasswordConfirmation(resetDto.getPassword(), resetDto.getConfirmPassword());

		if (!validatePasswordResetToken(resetDto.getToken())) {
			throw new IllegalArgumentException("Invalid or expired password reset token.");
		}

		User user = userRepository.findByResetPasswordToken(resetDto.getToken())
				.orElseThrow(() -> new IllegalArgumentException("Invalid token."));

		user.setPassword(passwordEncoder.encode(resetDto.getPassword()));
		user.setResetPasswordToken(null);
		user.setResetPasswordTokenExpiry(null);
		userRepository.save(user);
	}

	@Override
	public void updateCustomerProfile(String currentUsername, CustomerProfileDto profileDto) {
		User userToUpdate = userRepository.findByUsername(currentUsername)
				.orElseThrow(() -> new RuntimeException("Current user not found."));

		userValidationService.validateUsernameOnUpdate(profileDto.getUsername(), userToUpdate.getId());
		userValidationService.validateEmailOnUpdate(profileDto.getEmail(), userToUpdate.getId());

		userToUpdate.setFirstName(profileDto.getFirstName());
		userToUpdate.setLastName(profileDto.getLastName());
		userToUpdate.setUsername(profileDto.getUsername());
		userToUpdate.setEmail(profileDto.getEmail());
		userToUpdate.setPhone(profileDto.getPhone());
		userToUpdate.setHouseNo(profileDto.getHouseNo());
		userToUpdate.setLotNo(profileDto.getLotNo());
		userToUpdate.setBlockNo(profileDto.getBlockNo());
		userToUpdate.setStreet(profileDto.getStreet());
		userToUpdate.setBarangay(profileDto.getBarangay());
		userToUpdate.setMunicipality(profileDto.getMunicipality());
		userToUpdate.setProvince(profileDto.getProvince());

		userRepository.save(userToUpdate);
	}

	@Override
	public void updateCustomerPassword(String currentUsername, CustomerPasswordDto passwordDto) {
		User userToUpdate = userRepository.findByUsername(currentUsername)
				.orElseThrow(() -> new RuntimeException("Current user not found."));

		if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), userToUpdate.getPassword())) {
			throw new IllegalArgumentException("Current password does not match.");
		}

		validatePasswordConfirmation(passwordDto.getNewPassword(), passwordDto.getConfirmPassword());

		userToUpdate.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));
		userRepository.save(userToUpdate);
	}
}