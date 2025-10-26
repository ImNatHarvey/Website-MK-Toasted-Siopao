package com.toastedsiopao.service;

import com.toastedsiopao.dto.UserDto; // We will create this DTO next
import com.toastedsiopao.model.User;

public interface UserService {
	User saveCustomer(UserDto userDto);

	User findByUsername(String username); // Add this for checking duplicates
}
