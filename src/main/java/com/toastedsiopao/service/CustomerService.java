package com.toastedsiopao.service;

import com.toastedsiopao.dto.CustomerCreateDto;
import com.toastedsiopao.dto.CustomerSignUpDto;
import com.toastedsiopao.dto.CustomerUpdateDto;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomerService {

	User saveCustomer(CustomerSignUpDto customerDto);

	User createCustomerFromAdmin(CustomerCreateDto userDto);

	User findByUsername(String username);

	Optional<User> findUserById(Long id);

	Page<User> findAllCustomers(Pageable pageable);

	User updateCustomer(CustomerUpdateDto customerDto);

	Page<User> searchCustomers(String keyword, Pageable pageable);

	long countActiveCustomers();

	long countInactiveCustomers();

	void deleteCustomerById(Long id);

	void updateLastActivity(String username);

	void updateCustomerStatus(Long userId, String status);

	void checkForInactiveCustomers();

	long countNewCustomersThisMonth();

	/**
	 * Finds a user by email, generates a password reset token, saves it, and
	 * triggers the password reset email. * @param email The email address to look
	 * up.
	 * 
	 * @param resetUrlBase The base URL (e.g., "http://localhost:8080")
	 * @throws Exception if email sending fails.
	 */
	void processPasswordForgotRequest(String email, String resetUrlBase) throws Exception;
}