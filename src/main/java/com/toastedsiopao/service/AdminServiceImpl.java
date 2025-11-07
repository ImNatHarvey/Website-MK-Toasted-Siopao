package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.dto.AdminUpdateDto;
import com.toastedsiopao.model.Role; // NEW IMPORT
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.RoleRepository; // NEW IMPORT
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

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

	private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);
	private static final String OWNER_USERNAME = "mktoastedadmin"; // NEW: Owner protection
	private static final String OWNER_ROLE_NAME = "ROLE_OWNER"; // NEW: Owner role name
	private static final String CUSTOMER_ROLE_NAME = "ROLE_CUSTOMER"; // NEW: Customer role name

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository; // NEW INJECTION

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private Clock clock;

	// NEW: Injected validation service
	@Autowired
	private UserValidationService userValidationService;

	// --- Validation Helpers REMOVED ---

	// This helper remains as it's not in the new service
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
		// We now find both Owner and Admin roles
		List<User> owners = userRepository.findByRole_Name("ROLE_OWNER"); // UPDATED
		List<User> admins = userRepository.findByRole_Name("ROLE_ADMIN"); // UPDATED
		owners.addAll(admins);
		return owners;
	}

	@Override
	@Transactional(readOnly = true)
	public Page<User> findAllAdmins(Pageable pageable) {
		// This search is now handled by the custom query in the repository
		return userRepository.findAdminsBySearchKeyword("", pageable); // UPDATED
	}

	@Override
	@Transactional(readOnly = true)
	public Page<User> searchAdmins(String keyword, Pageable pageable) {
		if (!StringUtils.hasText(keyword)) {
			return findAllAdmins(pageable);
		}
		// This search is handled by the custom query
		return userRepository.findAdminsBySearchKeyword(keyword.trim(), pageable);
	}

	@Override
	public User createAccount(AdminAccountCreateDto userDto) { // UPDATED signature
		// UPDATED: Calls to new service
		userValidationService.validateUsernameDoesNotExist(userDto.getUsername());
		userValidationService.validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());

		// --- NEW: Find Role by ID from DTO ---
		Role roleToAssign = roleRepository.findById(userDto.getRoleId()).orElseThrow(
				() -> new RuntimeException("CRITICAL: Role with ID '" + userDto.getRoleId() + "' not found."));

		// Prevent assigning ROLE_OWNER or ROLE_CUSTOMER
		if (OWNER_ROLE_NAME.equals(roleToAssign.getName()) || CUSTOMER_ROLE_NAME.equals(roleToAssign.getName())) {
			throw new IllegalArgumentException("Cannot assign this role through the admin creation form.");
		}
		// --- END NEW ---

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole(roleToAssign); // Use Role object
		// Status, CreatedAt, and LastActivity are set by @PrePersist

		return userRepository.save(newUser);
	}

	@Override
	public User updateAdmin(AdminUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		// --- NEW: Owner Protection ---
		if (OWNER_USERNAME.equals(userToUpdate.getUsername())) {
			throw new IllegalArgumentException(
					"The Owner account ('" + OWNER_USERNAME + "') cannot be edited this way.");
		}
		// --- END NEW ---

		// Check if it's an admin/owner role
		if (userToUpdate.getRole() == null || (!"ROLE_ADMIN".equals(userToUpdate.getRole().getName())
				&& !"ROLE_OWNER".equals(userToUpdate.getRole().getName()))) {
			throw new IllegalArgumentException("Cannot update non-admin user with this method.");
		}

		// --- NEW: Find Role by ID from DTO ---
		Role roleToAssign = roleRepository.findById(userDto.getRoleId()).orElseThrow(
				() -> new RuntimeException("CRITICAL: Role with ID '" + userDto.getRoleId() + "' not found."));

		// Prevent assigning ROLE_OWNER to another user
		if (OWNER_ROLE_NAME.equals(roleToAssign.getName()) || CUSTOMER_ROLE_NAME.equals(roleToAssign.getName())) {
			throw new IllegalArgumentException("Cannot assign the Owner or Customer role to another user.");
		}
		// --- END NEW ---

		// UPDATED: Calls to new service
		userValidationService.validateUsernameOnUpdate(userDto.getUsername(), userDto.getId());
		userValidationService.validateEmailOnUpdate(userDto.getEmail(), userDto.getId());

		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setEmail(userDto.getEmail());
		userToUpdate.setRole(roleToAssign); // UPDATED

		return userRepository.save(userToUpdate);
	}

	@Override
	public User updateAdminProfile(AdminUpdateDto adminDto) {
		// This method is for an admin updating *themselves*
		User userToUpdate = userRepository.findById(adminDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + adminDto.getId()));

		// UPDATED: Calls to new service
		userValidationService.validateUsernameOnUpdate(adminDto.getUsername(), adminDto.getId());
		userValidationService.validateEmailOnUpdate(adminDto.getEmail(), adminDto.getId());

		userToUpdate.setFirstName(adminDto.getFirstName());
		userToUpdate.setLastName(adminDto.getLastName());
		userToUpdate.setUsername(adminDto.getUsername());
		userToUpdate.setEmail(adminDto.getEmail());

		// --- NEW: Handle role update for Owner ---
		// If the user is the Owner, we must ensure their roleId in the DTO
		// matches their actual role, otherwise validation fails.
		// We also re-assign their role to prevent it from being accidentally
		// set to null. This stops an Owner from accidentally de-assigning themselves.
		if (OWNER_ROLE_NAME.equals(userToUpdate.getRole().getName())) {
			if (!userToUpdate.getRole().getId().equals(adminDto.getRoleId())) {
				log.warn("Attempt by Owner ({}) to change their own role was blocked.", userToUpdate.getUsername());
				throw new IllegalArgumentException("Owner account cannot change its own role.");
			}
			userToUpdate.setRole(userToUpdate.getRole()); // Re-affirm the role
		} else {
			// For non-owners, they cannot change their role from this screen.
			// We just re-assign their existing role.
			userToUpdate.setRole(userToUpdate.getRole());
		}
		// --- END NEW ---

		return userRepository.save(userToUpdate);
	}

	@Override
	public void deleteAdminById(Long id) {
		// 1. Find the user
		User userToDelete = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));

		// --- NEW: Owner Protection ---
		if (OWNER_USERNAME.equals(userToDelete.getUsername())) {
			throw new RuntimeException("The Owner account ('" + OWNER_USERNAME + "') cannot be deleted.");
		}
		// --- END NEW ---

		// 2. Check if it's an admin
		if (userToDelete.getRole() == null || CUSTOMER_ROLE_NAME.equals(userToDelete.getRole().getName())) { // UPDATED
			throw new RuntimeException("Cannot delete non-admin user with this method.");
		}

		// 3. We could add logic to prevent deleting the last admin
		long adminCount = countAllAdmins(); // This now counts ADMIN + OWNER
		if (adminCount <= 1) {
			throw new RuntimeException("Cannot delete the last admin account.");
		}

		userRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public long countAllAdmins() {
		// We count all admins and owners
		return userRepository.countByRole_Name("ROLE_ADMIN") + userRepository.countByRole_Name("ROLE_OWNER"); // UPDATED
	}

	@Override
	@Transactional(readOnly = true)
	public long countActiveAdmins() {
		// Count active admins and active owners
		return userRepository.countByRole_NameAndStatus("ROLE_ADMIN", "ACTIVE") // UPDATED
				+ userRepository.countByRole_NameAndStatus("ROLE_OWNER", "ACTIVE"); // UPDATED
	}

	// --- NEW: Implementation for role dropdown ---
	@Override
	@Transactional(readOnly = true)
	public List<Role> findAllAdminRoles() {
		return roleRepository.findAll().stream().filter(role -> !CUSTOMER_ROLE_NAME.equals(role.getName())) // Exclude
																											// customer
																											// role
				.collect(Collectors.toList());
	}
	// --- END NEW ---

	// --- NEW: Dashboard Stats Implementation ---
	@Override
	@Transactional(readOnly = true)
	public long countNewAdminsThisMonth() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
		// This counts both new admins and new owners (though owners shouldn't be new)
		return userRepository.countByRole_NameAndCreatedAtBetween("ROLE_ADMIN", startOfMonth, now) // UPDATED
				+ userRepository.countByRole_NameAndCreatedAtBetween("ROLE_OWNER", startOfMonth, now); // UPDATED
	}
}