package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAdminUpdateDto;
import com.toastedsiopao.dto.AdminCustomerUpdateDto;
import com.toastedsiopao.dto.AdminUserCreateDto;
import com.toastedsiopao.dto.UserDto;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable

import java.util.List;
import java.util.Optional;

public interface UserService {

	User saveCustomer(UserDto userDto);

	User findByUsername(String username);

	// **** UPDATED METHOD ****
	Page<User> findAllCustomers(Pageable pageable);

	Optional<User> findUserById(Long id);

	User saveAdminUser(AdminUserCreateDto userDto, String role);

	User updateCustomer(AdminCustomerUpdateDto userDto);

	List<User> findAllAdmins();

	void deleteUserById(Long id);

	User updateAdmin(AdminAdminUpdateDto userDto);

	// --- NEW METHOD ---
	// **** UPDATED METHOD ****
	/**
	 * Searches for customers based on a keyword matching name, username, or phone.
	 * * @param keyword The search term. * @return A list of matching customer User
	 * entities.
	 */
	Page<User> searchCustomers(String keyword, Pageable pageable);

	// --- NEW: Methods for activity tracking ---
	/**
	 * Updates the lastActivity timestamp for a user. * @param username The username
	 * of the user to update.
	 */
	void updateLastActivity(String username);

	/**
	 * Manually updates the status of a user. * @param userId The ID of the user.
	 * * @param status The new status ("ACTIVE" or "INACTIVE").
	 */
	void updateCustomerStatus(Long userId, String status);

	/**
	 * Iterates through all customers and marks them as INACTIVE if they haven't had
	 * activity in a defined period (e.g., 1 month).
	 */
	void checkForInactiveCustomers();
	// --- END NEW ---
}