package com.toastedsiopao.service;

import com.toastedsiopao.dto.CustomerCreateDto;
import com.toastedsiopao.dto.CustomerPasswordDto;
import com.toastedsiopao.dto.CustomerProfileDto;
import com.toastedsiopao.dto.CustomerSignUpDto;
import com.toastedsiopao.dto.CustomerUpdateDto;
import com.toastedsiopao.dto.PasswordResetDto;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomerService {

	User saveCustomer(CustomerSignUpDto customerDto, String siteUrl) throws Exception;

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

	void processPasswordForgotRequest(String email, String resetUrlBase) throws Exception;

	boolean validatePasswordResetToken(String token);

	void resetPassword(PasswordResetDto resetDto);

	void updateCustomerProfile(String currentUsername, CustomerProfileDto profileDto);

	void updateCustomerPassword(String currentUsername, CustomerPasswordDto passwordDto);

	// --- UPDATED: Now takes ID and returns specific status string ---
	String verifyAccount(Long userId, String token);
}