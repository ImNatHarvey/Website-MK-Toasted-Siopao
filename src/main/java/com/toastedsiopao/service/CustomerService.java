package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAccountCreateDto; // NEW IMPORT
import com.toastedsiopao.dto.CustomerSignUpDto;
import com.toastedsiopao.dto.CustomerUpdateDto;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomerService {

	User saveCustomer(CustomerSignUpDto customerDto); // For public signup

	// --- NEW: For admin panel creation ---
	User createCustomerFromAdmin(AdminAccountCreateDto userDto);
	// --- END NEW ---

	User findByUsername(String username);

	Optional<User> findUserById(Long id); // Kept for generic ID lookups

	// --- Customer Management (by Admin) ---
	Page<User> findAllCustomers(Pageable pageable);

	User updateCustomer(CustomerUpdateDto customerDto);

	Page<User> searchCustomers(String keyword, Pageable pageable);

	long countActiveCustomers();

	void deleteCustomerById(Long id); // NEW: Fixes delete error

	// --- Activity Tracking Methods ---
	void updateLastActivity(String username);

	void updateCustomerStatus(Long userId, String status);

	void checkForInactiveCustomers();

	// --- NEW: For Dashboard Stats ---
	/**
	 * Counts new customers registered this month. * @return Count of new customers.
	 */
	long countNewCustomersThisMonth();
}