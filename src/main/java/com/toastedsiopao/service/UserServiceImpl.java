package com.toastedsiopao.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Import StringUtils

import com.toastedsiopao.dto.AdminAdminUpdateDto;
import com.toastedsiopao.dto.AdminCustomerUpdateDto;
import com.toastedsiopao.dto.AdminUserCreateDto;
import com.toastedsiopao.dto.UserDto;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public User saveCustomer(UserDto userDto) {
		if (findByUsername(userDto.getUsername()) != null) {
			throw new IllegalArgumentException("Username already exists: " + userDto.getUsername());
		}

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setPhone(userDto.getPhone());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole("ROLE_CUSTOMER");

		newUser.setHouseNo(userDto.getHouseNo());
		newUser.setLotNo(userDto.getLotNo());
		newUser.setBlockNo(userDto.getBlockNo());
		newUser.setStreet(userDto.getStreet());
		newUser.setBarangay(userDto.getBarangay());
		newUser.setMunicipality(userDto.getMunicipality());
		newUser.setProvince(userDto.getProvince());

		return userRepository.save(newUser);
	}

	@Override
	@Transactional(readOnly = true)
	public User findByUsername(String username) {
		return userRepository.findByUsername(username).orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAllCustomers() {
		return userRepository.findByRole("ROLE_CUSTOMER");
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findUserById(Long id) {
		return userRepository.findById(id);
	}

	@Override
	@Transactional
	public User saveAdminUser(AdminUserCreateDto userDto, String role) {
		if (findByUsername(userDto.getUsername()) != null) {
			throw new IllegalArgumentException("Username already exists: " + userDto.getUsername());
		}

		User newUser = new User();
		newUser.setFirstName(userDto.getFirstName());
		newUser.setLastName(userDto.getLastName());
		newUser.setUsername(userDto.getUsername());
		newUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
		newUser.setRole(role);

		return userRepository.save(newUser);
	}

	@Override
	@Transactional
	public User updateCustomer(AdminCustomerUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		Optional<User> userWithSameUsername = userRepository.findByUsername(userDto.getUsername());
		if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(userDto.getId())) {
			throw new IllegalArgumentException("Username '" + userDto.getUsername() + "' already exists.");
		}

		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());
		userToUpdate.setPhone(userDto.getPhone());
		userToUpdate.setHouseNo(userDto.getHouseNo());
		userToUpdate.setLotNo(userDto.getLotNo());
		userToUpdate.setBlockNo(userDto.getBlockNo());
		userToUpdate.setStreet(userDto.getStreet());
		userToUpdate.setBarangay(userDto.getBarangay());
		userToUpdate.setMunicipality(userDto.getMunicipality());
		userToUpdate.setProvince(userDto.getProvince());

		return userRepository.save(userToUpdate);
	}

	@Override
	@Transactional(readOnly = true)
	public List<User> findAllAdmins() {
		return userRepository.findByRole("ROLE_ADMIN");
	}

	@Override
	@Transactional
	public void deleteUserById(Long id) {
		userRepository.deleteById(id);
	}

	@Override
	@Transactional
	public User updateAdmin(AdminAdminUpdateDto userDto) {
		User userToUpdate = userRepository.findById(userDto.getId())
				.orElseThrow(() -> new RuntimeException("User not found with id: " + userDto.getId()));

		Optional<User> userWithSameUsername = userRepository.findByUsername(userDto.getUsername());
		if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(userDto.getId())) {
			throw new IllegalArgumentException("Username '" + userDto.getUsername() + "' already exists.");
		}

		userToUpdate.setFirstName(userDto.getFirstName());
		userToUpdate.setLastName(userDto.getLastName());
		userToUpdate.setUsername(userDto.getUsername());

		return userRepository.save(userToUpdate);
	}

	// --- NEW: Implementation for searching customers ---
	@Override
	@Transactional(readOnly = true)
	public List<User> searchCustomers(String keyword) {
		// Use StringUtils to safely handle null or empty/whitespace keywords
		if (!StringUtils.hasText(keyword)) {
			return findAllCustomers(); // If no keyword, return all
		}
		return userRepository.findByRoleAndSearchKeyword(keyword.trim());
	}
}