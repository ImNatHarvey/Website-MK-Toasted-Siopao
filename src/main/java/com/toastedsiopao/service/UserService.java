package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAdminUpdateDto;
import com.toastedsiopao.dto.AdminCustomerUpdateDto;
import com.toastedsiopao.dto.AdminUserCreateDto;
import com.toastedsiopao.dto.UserDto;
import com.toastedsiopao.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

	User saveCustomer(UserDto userDto);

	User findByUsername(String username);

	List<User> findAllCustomers();

	Optional<User> findUserById(Long id);

	User saveAdminUser(AdminUserCreateDto userDto, String role);

	User updateCustomer(AdminCustomerUpdateDto userDto);

	List<User> findAllAdmins();

	void deleteUserById(Long id);

	User updateAdmin(AdminAdminUpdateDto userDto);

	// --- NEW METHOD ---
	/**
	 * Searches for customers based on a keyword matching name, username, or phone.
	 * * @param keyword The search term.
	 * 
	 * @return A list of matching customer User entities.
	 */
	List<User> searchCustomers(String keyword);
}