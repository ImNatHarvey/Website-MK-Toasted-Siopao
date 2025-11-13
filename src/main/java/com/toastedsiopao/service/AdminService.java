package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAccountCreateDto;
import com.toastedsiopao.dto.AdminPasswordUpdateDto;
import com.toastedsiopao.dto.AdminProfileUpdateDto;
import com.toastedsiopao.dto.AdminUpdateDto;
import com.toastedsiopao.dto.RoleDto; // ADDED
import com.toastedsiopao.model.Role;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AdminService {

	Optional<User> findUserById(Long id);

	List<User> findAllAdmins();

	List<Role> findAllAdminRoles();

	Page<User> findAllAdmins(Pageable pageable);

	Page<User> searchAdmins(String keyword, Pageable pageable);

	User createAccount(AdminAccountCreateDto adminDto);

	User updateAdmin(AdminUpdateDto adminDto);

	User updateAdminProfile(AdminProfileUpdateDto adminDto);

	void deleteAdminById(Long id);

	long countAllAdmins();

	long countActiveAdmins();

	// --- ADDED ---
	long countInactiveAdmins();
	// --- END ADDED ---

	long countNewAdminsThisMonth();

	Optional<Role> findRoleById(Long id);

	Role createRole(RoleDto roleDto);

	Role updateRole(RoleDto roleDto);

	void deleteRole(Long id);

	// --- ADDED ---
	void updateAdminPassword(String currentUsername, AdminPasswordUpdateDto passwordDto);
}