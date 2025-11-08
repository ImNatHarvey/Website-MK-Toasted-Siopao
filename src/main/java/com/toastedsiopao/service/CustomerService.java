package com.toastedsiopao.service;

import com.toastedsiopao.dto.AdminAccountCreateDto; 
import com.toastedsiopao.dto.CustomerSignUpDto;
import com.toastedsiopao.dto.CustomerUpdateDto;
import com.toastedsiopao.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomerService {

	User saveCustomer(CustomerSignUpDto customerDto); 
	
	User createCustomerFromAdmin(AdminAccountCreateDto userDto);

	User findByUsername(String username);

	Optional<User> findUserById(Long id); 

	Page<User> findAllCustomers(Pageable pageable);

	User updateCustomer(CustomerUpdateDto customerDto);

	Page<User> searchCustomers(String keyword, Pageable pageable);

	long countActiveCustomers();

	void deleteCustomerById(Long id); 

	void updateLastActivity(String username);

	void updateCustomerStatus(Long userId, String status);

	void checkForInactiveCustomers();

	long countNewCustomersThisMonth();
}