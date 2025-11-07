package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.dto.AdminUpdateDto;
import com.toastedsiopao.model.Role; // NEW IMPORT
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AdminService {

	Optional<User> findUserById(Long id);

	List<User> findAllAdmins(); // For non-paginated lists

	Page<User> findAllAdmins(Pageable pageable); // For paginated lists

	Page<User> searchAdmins(String keyword, Pageable pageable);

	User createAccount(AdminAccountCreateDto adminDto); // UPDATED: Role is now in the DTO

	User updateAdmin(AdminUpdateDto adminDto);

	User updateAdminProfile(AdminUpdateDto adminDto); // For an admin updating themselves

	void deleteAdminById(Long id);

	long countAllAdmins();

	long countActiveAdmins(); // NEW: Fixes stats error

	// --- NEW: For Admin page dropdowns ---
	/**
	 * Finds all roles that can be assigned to an admin (e.g., "Owner", "Admin").
	 * Excludes "Customer". * @return A list of admin-level roles.
	 */
	List<Role> findAllAdminRoles();
	// --- END NEW ---

	// --- NEW: For Dashboard Stats ---
	/**
	 * Counts new admins registered this month. * @return Count of new admins.
	 */
	long countNewAdminsThisMonth();

}