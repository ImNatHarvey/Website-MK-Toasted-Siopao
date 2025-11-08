package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.dto.AdminUpdateDto;
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
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

	private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);
	private static final String OWNER_USERNAME = "mktoastedadmin"; 
	private static final String OWNER_ROLE_NAME = "ROLE_OWNER";
	private static final String CUSTOMER_ROLE_NAME = "ROLE_CUSTOMER"; 

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
	public List<User> findAllAdmins() {
		return userRepository.findByRole_NameNot(CUSTOMER_ROLE_NAME);
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
		if (formattedName.equals(OWNER_ROLE_NAME) || formattedName.equals(CUSTOMER_ROLE_NAME)) {
			throw new IllegalArgumentException("Cannot create role with reserved name: " + displayName);
		}
		return formattedName;
	}

	private void addPermissionsToRole(Role role, AdminAccountCreateDto dto) {
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

	private void addPermissionsToRole(Role role, AdminUpdateDto dto) {
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
			throw new IllegalArgumentException("Role name cannot be blank.");
		}
		if (!userDto.getRoleName().matches("^[a-zA-Z0-9 ]+$")) {
			throw new IllegalArgumentException("Role name can only contain letters, numbers, and spaces");
		}
		if (userDto.getRoleName().length() > 40) {
			throw new IllegalArgumentException("Role name cannot exceed 40 characters");
		}
		
		String internalRoleName = formatRoleName(userDto.getRoleName());

		if (roleRepository.findByName(internalRoleName).isPresent()) {
			throw new IllegalArgumentException("Role name '" + userDto.getRoleName() + "' already exists.");
		}

		Role newRole = new Role(internalRoleName);
		addPermissionsToRole(newRole, userDto);
		roleRepository.save(newRole); 

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setEmail(userDto.getEmail());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole(newRole); 

		return userRepository.save(newUser);
	}

	@Override
	public User updateAdmin(AdminUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		if (OWNER_USERNAME.equals(userToUpdate.getUsername())) {
			throw new IllegalArgumentException(
					"The Owner account ('" + OWNER_USERNAME + "') cannot be edited this way.");
		}
		
		if (userToUpdate.getRole() == null || CUSTOMER_ROLE_NAME.equals(userToUpdate.getRole().getName())) {
			throw new IllegalArgumentException("Cannot update non-admin user with this method.");
		}

		Role roleToUpdate = userToUpdate.getRole();
		if (roleToUpdate == null) {
			throw new IllegalStateException(
					"User " + userToUpdate.getUsername() + " has a null role and cannot be updated.");
		}

		if (OWNER_ROLE_NAME.equals(roleToUpdate.getName())) {
			throw new IllegalArgumentException("The Owner's role cannot be modified.");
		}

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isOwner = authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals(OWNER_ROLE_NAME));

		if (isOwner) {
			
			String newInternalRoleName = formatRoleName(userDto.getRoleName());

			if (!roleToUpdate.getName().equals(newInternalRoleName)) {
				if (roleRepository.findByName(newInternalRoleName).isPresent()) {
					throw new IllegalArgumentException("Role name '" + userDto.getRoleName() + "' already exists.");
				}
				roleToUpdate.setName(newInternalRoleName);
			}

			addPermissionsToRole(roleToUpdate, userDto);
			roleRepository.save(roleToUpdate); 
		} else {
			
			if (!userDto.getRoleName().equals(roleToUpdate.getName().replace("ROLE_", ""))) {
				log.warn("Non-Owner user {} tried to change role name for {}. Blocked.", authentication.getName(),
						userToUpdate.getUsername());
			}
			
		}
		
		userValidationService.validateUsernameOnUpdate(userDto.getUsername(), userDto.getId());
		userValidationService.validateEmailOnUpdate(userDto.getEmail(), userDto.getId());

		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setEmail(userDto.getEmail());
		userToUpdate.setRole(roleToUpdate); 

		return userRepository.save(userToUpdate);
	}

	@Override
	public User updateAdminProfile(AdminUpdateDto adminDto) {
		
		User userToUpdate = userRepository.findById(adminDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + adminDto.getId()));

		userValidationService.validateUsernameOnUpdate(adminDto.getUsername(), adminDto.getId());
		userValidationService.validateEmailOnUpdate(adminDto.getEmail(), adminDto.getId());

		userToUpdate.setFirstName(adminDto.getFirstName());
		userToUpdate.setLastName(adminDto.getLastName());
		userToUpdate.setUsername(adminDto.getUsername());
		userToUpdate.setEmail(adminDto.getEmail());

		if (OWNER_ROLE_NAME.equals(userToUpdate.getRole().getName())) {
			if (adminDto.isManageAdmins() || adminDto.isManageCustomers()) {
				log.warn("Attempt by Owner ({}) to change their own role was blocked.", userToUpdate.getUsername());
			}
			userToUpdate.setRole(userToUpdate.getRole()); 
		} else {
			userToUpdate.setRole(userToUpdate.getRole());
		}
		return userRepository.save(userToUpdate);
	}

	@Override
	public void deleteAdminById(Long id) {
		// 1. Find the user
		User userToDelete = userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found with id: " + id));

		if (OWNER_USERNAME.equals(userToDelete.getUsername())) {
			throw new RuntimeException("The Owner account ('" + OWNER_USERNAME + "') cannot be deleted.");
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

		List<User> usersWithRole = userRepository.findByRole_Name(roleToDelete.getName());
		if (usersWithRole.isEmpty() && !roleToDelete.getName().equals("ROLE_ADMIN")) { 
			log.info("Deleting orphaned custom role: {}", roleToDelete.getName());
			roleRepository.delete(roleToDelete);
		}
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
	public long countNewAdminsThisMonth() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
		return userRepository.countByRole_NameAndCreatedAtBetween("ROLE_ADMIN", startOfMonth, now) 
				+ userRepository.countByRole_NameAndCreatedAtBetween("ROLE_OWNER", startOfMonth, now); 
	}
}