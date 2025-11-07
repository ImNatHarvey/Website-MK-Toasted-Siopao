package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.dto.AdminUpdateDto;
import com.toastedsiopao.model.Permission; // NEW IMPORT
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

	/**
	 * Formats a display name (e.g., "Night Staff") into an internal role name
	 * (e.g., "ROLE_NIGHT_STAFF"). * @param displayName The name from the DTO.
	 * 
	 * @return The formatted internal role name.
	 */
	private String formatRoleName(String displayName) {
		if (!StringUtils.hasText(displayName)) {
			throw new IllegalArgumentException("Role name cannot be blank.");
		}
		String formattedName = "ROLE_" + displayName.toUpperCase().replaceAll("\\s+", "_");
		if (formattedName.equals(OWNER_ROLE_NAME) || formattedName.equals(CUSTOMER_ROLE_NAME)) {
			throw new IllegalArgumentException("Cannot create role with reserved name: " + displayName);
		}
		return formattedName;
	}

	/**
	 * Helper method to populate a Role entity with permissions based on DTO
	 * booleans. * @param role The Role entity to modify.
	 * 
	 * @param dto The DTO containing permission flags.
	 */
	private void addPermissionsToRole(Role role, AdminAccountCreateDto dto) {
		// Clear any existing permissions (for updates)
		role.getPermissions().clear();

		// Add Dashboard by default to all admins
		role.addPermission(Permission.VIEW_DASHBOARD.name());

		if (dto.isManageCustomers()) {
			role.addPermission(Permission.VIEW_CUSTOMERS.name());
			role.addPermission(Permission.ADD_CUSTOMERS.name());
			role.addPermission(Permission.EDIT_CUSTOMERS.name());
			role.addPermission(Permission.DELETE_CUSTOMERS.name());
		}
		if (dto.isManageAdmins()) {
			role.addPermission(Permission.VIEW_ADMINS.name());
			role.addPermission(Permission.ADD_ADMINS.name());
			role.addPermission(Permission.EDIT_ADMINS.name());
			role.addPermission(Permission.DELETE_ADMINS.name());
		}
		if (dto.isManageOrders()) {
			role.addPermission(Permission.VIEW_ORDERS.name());
			role.addPermission(Permission.EDIT_ORDERS.name());
		}
		if (dto.isManageProducts()) {
			role.addPermission(Permission.VIEW_PRODUCTS.name());
			role.addPermission(Permission.ADD_PRODUCTS.name());
			role.addPermission(Permission.EDIT_PRODUCTS.name());
			role.addPermission(Permission.DELETE_PRODUCTS.name());
			role.addPermission(Permission.ADJUST_PRODUCT_STOCK.name());
		}
		if (dto.isManageInventory()) {
			role.addPermission(Permission.VIEW_INVENTORY.name());
			role.addPermission(Permission.ADD_INVENTORY_ITEMS.name());
			role.addPermission(Permission.EDIT_INVENTORY_ITEMS.name());
			role.addPermission(Permission.DELETE_INVENTORY_ITEMS.name());
			role.addPermission(Permission.ADJUST_INVENTORY_STOCK.name());
			role.addPermission(Permission.MANAGE_INVENTORY_CATEGORIES.name());
			role.addPermission(Permission.MANAGE_UNITS.name());
		}
		if (dto.isManageTransactions()) {
			role.addPermission(Permission.VIEW_TRANSACTIONS.name());
		}
		if (dto.isManageSite()) {
			role.addPermission(Permission.EDIT_SITE_SETTINGS.name());
		}
		if (dto.isManageActivityLog()) {
			role.addPermission(Permission.VIEW_ACTIVITY_LOG.name());
		}
	}

	/**
	 * Overloaded helper method for the AdminUpdateDto. * @param role The Role
	 * entity to modify.
	 * 
	 * @param dto The DTO containing permission flags.
	 */
	private void addPermissionsToRole(Role role, AdminUpdateDto dto) {
		// Clear any existing permissions (for updates)
		role.getPermissions().clear();

		// Add Dashboard by default to all admins
		role.addPermission(Permission.VIEW_DASHBOARD.name());

		if (dto.isManageCustomers()) {
			role.addPermission(Permission.VIEW_CUSTOMERS.name());
			role.addPermission(Permission.ADD_CUSTOMERS.name());
			role.addPermission(Permission.EDIT_CUSTOMERS.name());
			role.addPermission(Permission.DELETE_CUSTOMERS.name());
		}
		if (dto.isManageAdmins()) {
			role.addPermission(Permission.VIEW_ADMINS.name());
			role.addPermission(Permission.ADD_ADMINS.name());
			role.addPermission(Permission.EDIT_ADMINS.name());
			role.addPermission(Permission.DELETE_ADMINS.name());
		}
		if (dto.isManageOrders()) {
			role.addPermission(Permission.VIEW_ORDERS.name());
			role.addPermission(Permission.EDIT_ORDERS.name());
		}
		if (dto.isManageProducts()) {
			role.addPermission(Permission.VIEW_PRODUCTS.name());
			role.addPermission(Permission.ADD_PRODUCTS.name());
			role.addPermission(Permission.EDIT_PRODUCTS.name());
			role.addPermission(Permission.DELETE_PRODUCTS.name());
			role.addPermission(Permission.ADJUST_PRODUCT_STOCK.name());
		}
		if (dto.isManageInventory()) {
			role.addPermission(Permission.VIEW_INVENTORY.name());
			role.addPermission(Permission.ADD_INVENTORY_ITEMS.name());
			role.addPermission(Permission.EDIT_INVENTORY_ITEMS.name());
			role.addPermission(Permission.DELETE_INVENTORY_ITEMS.name());
			role.addPermission(Permission.ADJUST_INVENTORY_STOCK.name());
			role.addPermission(Permission.MANAGE_INVENTORY_CATEGORIES.name());
			role.addPermission(Permission.MANAGE_UNITS.name());
		}
		if (dto.isManageTransactions()) {
			role.addPermission(Permission.VIEW_TRANSACTIONS.name());
		}
		if (dto.isManageSite()) {
			role.addPermission(Permission.EDIT_SITE_SETTINGS.name());
		}
		if (dto.isManageActivityLog()) {
			role.addPermission(Permission.VIEW_ACTIVITY_LOG.name());
		}
	}

	@Override
	public User createAccount(AdminAccountCreateDto userDto) { // UPDATED implementation
		// UPDATED: Calls to new service
		userValidationService.validateUsernameDoesNotExist(userDto.getUsername());
		userValidationService.validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());

		// --- NEW: Create Role from DTO ---
		String internalRoleName = formatRoleName(userDto.getRoleName());

		// Check if internal role name already exists
		if (roleRepository.findByName(internalRoleName).isPresent()) {
			throw new IllegalArgumentException("Role name '" + userDto.getRoleName() + "' already exists.");
		}

		Role newRole = new Role(internalRoleName);
		addPermissionsToRole(newRole, userDto); // Use helper
		roleRepository.save(newRole); // Save the new role
		// --- END NEW ---

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole(newRole); // Use new Role object
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
				&& !"ROLE_OWNER".equals(userToUpdate.getRole().getName())
				&& !userToUpdate.getRole().getName().startsWith("ROLE_"))) { // Allow custom roles
			throw new IllegalArgumentException("Cannot update non-admin user with this method.");
		}

		// --- NEW: Update Role from DTO ---
		Role roleToUpdate = userToUpdate.getRole();
		if (roleToUpdate == null) {
			throw new IllegalStateException(
					"User " + userToUpdate.getUsername() + " has a null role and cannot be updated.");
		}

		String newInternalRoleName = formatRoleName(userDto.getRoleName());

		// Check if name changed and if new name is taken
		if (!roleToUpdate.getName().equals(newInternalRoleName)) {
			if (roleRepository.findByName(newInternalRoleName).isPresent()) {
				throw new IllegalArgumentException("Role name '" + userDto.getRoleName() + "' already exists.");
			}
			roleToUpdate.setName(newInternalRoleName);
		}

		// Update permissions
		addPermissionsToRole(roleToUpdate, userDto); // Use helper
		roleRepository.save(roleToUpdate); // Save the updated role
		// --- END NEW ---

		// UPDATED: Calls to new service
		userValidationService.validateUsernameOnUpdate(userDto.getUsername(), userDto.getId());
		userValidationService.validateEmailOnUpdate(userDto.getEmail(), userDto.getId());

		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setEmail(userDto.getEmail());
		userToUpdate.setRole(roleToUpdate); // UPDATED

		return userRepository.save(userToUpdate);
	}

	@Override
	public User updateAdminProfile(AdminUpdateDto adminDto) {
		// This method is for an admin updating *themselves*
		User userToUpdate = userRepository.findById(adminDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + adminDto.getId()));

		// UPDATED: Calls to new service
		userValidationService.validateUsernameOnUpdate(adminDto.getUsername(), userDto.getId());
		userValidationService.validateEmailOnUpdate(adminDto.getEmail(), userDto.getId());

		userToUpdate.setFirstName(adminDto.getFirstName());
		userToUpdate.setLastName(adminDto.getLastName());
		userToUpdate.setUsername(adminDto.getUsername());
		userToUpdate.setEmail(adminDto.getEmail());

		// --- NEW: Handle role update for Owner ---
		// This logic is now safer. It doesn't use roleId anymore.
		// It just re-assigns the user's existing role, ignoring any
		// permission/roleName fields that might be on the DTO.
		if (OWNER_ROLE_NAME.equals(userToUpdate.getRole().getName())) {
			// This check is good, but the "Edit My Profile" form won't
			// even have the fields to trigger this.
			if (adminDto.isManageAdmins() || adminDto.isManageCustomers() /* etc */) {
				log.warn("Attempt by Owner ({}) to change their own role was blocked.", userToUpdate.getUsername());
				// We don't throw an error, we just ignore the changes.
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

		// --- NEW: Delete the associated role ---
		// We should only delete the role if it's not shared.
		// For now, we'll assume a 1-to-1 user-to-role mapping for custom roles.
		// A safer approach would be to check if any other user uses this role.
		Role roleToDelete = userToDelete.getRole();
		userRepository.deleteById(id); // Delete the user first

		// Check if any other user is using this role
		List<User> usersWithRole = userRepository.findByRole_Name(roleToDelete.getName());
		if (usersWithRole.isEmpty() && !roleToDelete.getName().equals("ROLE_ADMIN")) { // Don't delete the default
																						// "ROLE_ADMIN"
			log.info("Deleting orphaned custom role: {}", roleToDelete.getName());
			roleRepository.delete(roleToDelete);
		}
		// --- END NEW ---
	}

	@Override
	@Transactional(readOnly = true)
	public long countAllAdmins() {
		// --- THIS IS THE FIX for the error you screenshotted ---
		return userRepository.findAdminsBySearchKeyword("", Pageable.unpaged()).getTotalElements();
	}

	@Override
	@Transactional(readOnly = true)
	public long countActiveAdmins() {
		// Count active admins and active owners
		return userRepository.countByRole_NameAndStatus("ROLE_ADMIN", "ACTIVE") // UPDATED
				+ userRepository.countByRole_NameAndStatus("ROLE_OWNER", "ACTIVE"); // UPDATED
	}

	// --- METHOD REMOVED ---
	// @Override
	// @Transactional(readOnly = true)
	// public List<Role> findAllAdminRoles() { ... }
	// --- END METHOD REMOVED ---

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