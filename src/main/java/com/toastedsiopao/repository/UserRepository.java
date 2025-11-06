package com.toastedsiopao.repository;

import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import com.toastedsiopao.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	// This one is for non-paginated lists
	List<User> findByRole(String role);

	// This one is for paginated lists
	Page<User> findByRole(String role, Pageable pageable);

	// --- Search customers ---
	@Query("SELECT u FROM User u WHERE u.role = 'ROLE_CUSTOMER' AND ("
			+ "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "u.phone LIKE CONCAT('%', :keyword, '%'))")
	Page<User> findByRoleAndSearchKeyword(@Param("keyword") String keyword, Pageable pageable);

	// --- Search admins ---
	@Query("SELECT u FROM User u WHERE u.role = 'ROLE_ADMIN' AND ("
			+ "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
	Page<User> findAdminsBySearchKeyword(@Param("keyword") String keyword, Pageable pageable);

	// --- For inactivity check ---
	List<User> findByRoleAndStatus(String role, String status);

	// --- For stats cards ---
	long countByRoleAndStatus(String role, String status);

	long countByRole(String role); // NEW: Fixes compile error

}