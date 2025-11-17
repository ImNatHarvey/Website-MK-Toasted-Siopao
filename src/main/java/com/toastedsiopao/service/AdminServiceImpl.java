package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.dto.AdminPasswordUpdateDto;
import com.toastedsiopao.dto.AdminProfileUpdateDto;
import com.toastedsiopao.dto.AdminUpdateDto;
import com.toastedsiopao.dto.RoleDto;
import com.toastedsiopao.model.Permission;
import com.toastedsiopao.model.Role;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.RoleRepository;
import com.toastedsiopao.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

	private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);
	private static final String OWNER_USERNAME = "mktoastedadmin";
	private static final String OWNER_ROLE_NAME = "ROLE_OWNER";
	private static final String CUSTOMER_ROLE_NAME = "ROLE_CUSTOMER";

	private static final Set<String> DEFAULT_ROLES = Set.of(OWNER_ROLE_NAME, CUSTOMER_ROLE_NAME);

	@Autowired
	private UserRepository userRepository;

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
	@Transactional(readOnly = true)
	public Optional<User> findUserById(Long id) {
		return userRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Role> findRoleById(Long id) {
		return roleRepository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAllAdmins() {
		return userRepository.findByRole_NameNot(CUSTOMER_ROLE_NAME);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Role> findAllAdminRoles() {

		return roleRepository.findByNameNotIn(List.of(CUSTOMER_ROLE_NAME, OWNER_ROLE_NAME));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<User> findAllAdmins(Pageable pageable) {
		return userRepository.findAdminsBySearchKeyword("", pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<User> searchAdmins(String keyword, Pageable pageable) {
		if (!StringUtils.hasText(keyword)) {
			return findAllAdmins(pageable);
		}
		return userRepository.findAdminsBySearchKeyword(keyword.trim(), pageable);
	}

	private String formatRoleName(String displayName) {
		if (!StringUtils.hasText(displayName)) {
			throw new IllegalArgumentException("Role name cannot be blank.");
		}
		String formattedName = "ROLE_" + displayName.toUpperCase().replaceAll("\\s+", "_");
		if (DEFAULT_ROLES.contains(formattedName)) {
			throw new IllegalArgumentException("Cannot create role with reserved name: " + displayName);
		}
		return formattedName;
	}

	private void mapPermissionsToRole(Role role, RoleDto dto) {
		role.getPermissions().clear();

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
	public User createAccount(AdminAccountCreateDto userDto) {
		userValidationService.validateUsernameDoesNotExist(userDto.getUsername());
		userValidationService.validateEmailDoesNotExist(userDto.getEmail());
		validatePasswordConfirmation(userDto.getPassword(), userDto.getConfirmPassword());

		if (!StringUtils.hasText(userDto.getRoleName())) {
			throw new IllegalArgumentException("A role must be selected.");
		}

		String internalRoleName = userDto.getRoleName();

		Role roleToAssign = roleRepository.findByName(internalRoleName)
				.orElseThrow(() -> new IllegalArgumentException("Selected role not found: " + internalRoleName));

		if (roleToAssign.getName().equals(OWNER_ROLE_NAME)) {
			throw new IllegalArgumentException("Cannot create another Owner account.");
		}

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole(roleToAssign);

		return userRepository.save(newUser);
	}

	@Override
	public User updateAdmin(AdminUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		if (userToUpdate.getRole() != null && OWNER_ROLE_NAME.equals(userToUpdate.getRole().getName())) {
			throw new IllegalArgumentException("The Owner account cannot be edited.");
		}

		if (userToUpdate.getRole() == null || CUSTOMER_ROLE_NAME.equals(userToUpdate.getRole().getName())) {
			throw new IllegalArgumentException("Cannot update non-admin user with this method.");
		}

		Role roleToUpdate = userToUpdate.getRole();
		if (roleToUpdate == null) {
			throw new IllegalStateException(
					"User " + userToUpdate.getUsername() + " has a null role and cannot be updated.");
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isOwner = authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals(OWNER_ROLE_NAME));

		if (isOwner) {
			String newInternalRoleName = userDto.getRoleName();

			if (!roleToUpdate.getName().equals(newInternalRoleName)) {

				log.info("Admin {} role change: from {} to {}", userToUpdate.getUsername(), roleToUpdate.getName(),
						newInternalRoleName);
				Role newRole = roleRepository.findByName(newInternalRoleName)
						.orElseThrow(() -> new IllegalArgumentException(
								"Role name '" + userDto.getRoleName() + "' does not exist."));

				if (newRole.getName().equals(OWNER_ROLE_NAME)) {
					throw new IllegalArgumentException("Cannot assign Owner role.");
				}

				userToUpdate.setRole(newRole);
				roleToUpdate = newRole;
			}
		} else {
			if (!userDto.getRoleName().equals(roleToUpdate.getName())) {
				log.warn("Non-Owner user {} tried to change role for {}. Blocked.", authentication.getName(),
						userToUpdate.getUsername());
			}
		}

		userValidationService.validateUsernameOnUpdate(userDto.getUsername(), userDto.getId());
		userValidationService.validateEmailOnUpdate(userDto.getEmail(), userDto.getId());

		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setEmail(userDto.getEmail());

		if (isOwner) {
			if (StringUtils.hasText(userDto.getStatus())
					&& (userDto.getStatus().equals("ACTIVE") || userDto.getStatus().equals("INACTIVE"))) {
				userToUpdate.setStatus(userDto.getStatus());
			} else {
				throw new IllegalArgumentException("Invalid status value provided.");
			}
		} else {
			if (!userDto.getStatus().equals(userToUpdate.getStatus())) {
				log.warn("Non-Owner user {} tried to change status for {}. Blocked.", authentication.getName(),
						userToUpdate.getUsername());
			}
		}

		return userRepository.save(userToUpdate);
	}

	@Override
	public User updateAdminProfile(AdminProfileUpdateDto adminDto) {

		User userToUpdate = userRepository.findById(adminDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + adminDto.getId()));

		userValidationService.validateUsernameOnUpdate(adminDto.getUsername(), adminDto.getId());
		userValidationService.validateEmailOnUpdate(adminDto.getEmail(), adminDto.getId());

		userToUpdate.setFirstName(adminDto.getFirstName());
		userToUpdate.setLastName(adminDto.getLastName());
		userToUpdate.setUsername(adminDto.getUsername());
		userToUpdate.setEmail(adminDto.getEmail());

		return userRepository.save(userToUpdate);
	}

	@Override
	public void deleteAdminById(Long id) {

		User userToDelete = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));

		if (userToDelete.getRole() != null && OWNER_ROLE_NAME.equals(userToDelete.getRole().getName())) {
			throw new RuntimeException("The Owner account cannot be deleted.");
		}

		if (userToDelete.getRole() == null || CUSTOMER_ROLE_NAME.equals(userToDelete.getRole().getName())) {
			throw new RuntimeException("Cannot delete non-admin user with this method.");
		}

		long adminCount = countAllAdmins();
		if (adminCount <= 1) {
			throw new RuntimeException("Cannot delete the last admin account.");
		}

		Role roleToDelete = userToDelete.getRole();
		userRepository.deleteById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public long countAllAdmins() {
		return userRepository.findAdminsBySearchKeyword("", Pageable.unpaged()).getTotalElements();
	}

	@Override
	@Transactional(readOnly = true)
	public long countActiveAdmins() {
		return userRepository.countActiveAdmins();
	}

	@Override
	@Transactional(readOnly = true)
	public long countInactiveAdmins() {
		return userRepository.countInactiveAdmins();
	}

	@Override
	@Transactional(readOnly = true)
	public long countNewAdminsThisMonth() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);

		return userRepository.countByRole_NameNotAndCreatedAtBetween(CUSTOMER_ROLE_NAME, startOfMonth, now);
	}

	@Override
	public Role createRole(RoleDto roleDto) {
		String formattedName = formatRoleName(roleDto.getName());
		Optional<Role> existing = roleRepository.findByName(formattedName);
		if (existing.isPresent()) {
			throw new IllegalArgumentException("Role name '" + formattedName + "' already exists.");
		}

		Role newRole = new Role(formattedName);
		mapPermissionsToRole(newRole, roleDto);

		log.info("Creating new role: {}", formattedName);
		return roleRepository.save(newRole);
	}

	@Override
	public Role updateRole(RoleDto roleDto) {
		Long roleId = roleDto.getId();
		if (roleId == null) {
			throw new IllegalArgumentException("Role ID is required for an update.");
		}
		Role roleToUpdate = roleRepository.findById(roleId)
				.orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

		if (DEFAULT_ROLES.contains(roleToUpdate.getName())) {
			throw new IllegalArgumentException("Cannot edit the default role: " + roleToUpdate.getName());
		}

		String newFormattedName = formatRoleName(roleDto.getName());
		Optional<Role> existing = roleRepository.findByName(newFormattedName);
		if (existing.isPresent() && !existing.get().getId().equals(roleId)) {
			throw new IllegalArgumentException("Role name '" + newFormattedName + "' already exists.");
		}

		roleToUpdate.setName(newFormattedName);
		mapPermissionsToRole(roleToUpdate, roleDto);

		log.info("Updating role: {}", newFormattedName);
		return roleRepository.save(roleToUpdate);
	}

	@Override
	public void deleteRole(Long id) {
		Role roleToDelete = roleRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Role not found with id: " + id));

		if (DEFAULT_ROLES.contains(roleToDelete.getName())) {
			throw new RuntimeException("Cannot delete a default system role: " + roleToDelete.getName());
		}

		log.info("Deleting role: {}", roleToDelete.getName());
		roleRepository.delete(roleToDelete);
	}

	@Override
	public void updateAdminPassword(String currentUsername, AdminPasswordUpdateDto passwordDto) {
		User userToUpdate = userRepository.findByUsername(currentUsername)
				.orElseThrow(() -> new RuntimeException("Current user not found."));

		if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), userToUpdate.getPassword())) {
			throw new IllegalArgumentException("Current password does not match.");
		}

		validatePasswordConfirmation(passwordDto.getNewPassword(), passwordDto.getConfirmPassword());

		userToUpdate.setPassword(passwordEncoder.encode(passwordDto.getNewPassword()));
		userRepository.save(userToUpdate);
		log.info("User password updated for: {}", currentUsername);
	}
	
	@Override
	@Transactional(readOnly = true)
	public boolean validateOwnerPassword(String password) {
		if (!StringUtils.hasText(password)) {
			return false;
		}
		
		User owner = userRepository.findByUsername(OWNER_USERNAME) 
				.orElse(null);
		
		if (owner == null) {
			 log.error("CRITICAL: The Owner account '{}' could not be found for password validation.", OWNER_USERNAME);
			 return false;
		}

		if (passwordEncoder.matches(password, owner.getPassword())) {
			return true;
		}
		
		return false;
	}
}