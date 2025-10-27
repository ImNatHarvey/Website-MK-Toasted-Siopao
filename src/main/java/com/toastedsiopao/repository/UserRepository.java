package com.toastedsiopao.repository;

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

	List<User> findByRole(String role);

	// --- NEW: Search customers by keyword across multiple fields ---
	@Query("SELECT u FROM User u WHERE u.role = 'ROLE_CUSTOMER' AND ("
			+ "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "u.phone LIKE CONCAT('%', :keyword, '%'))")
	List<User> findByRoleAndSearchKeyword(@Param("keyword") String keyword);

}