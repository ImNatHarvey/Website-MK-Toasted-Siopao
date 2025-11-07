package com.toastedsiopao.repository;

import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable; // Import Pageable
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import Query
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import com.toastedsiopao.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	// This one is for non-paginated lists
	List<User> findByRole_Name(String roleName); // UPDATED

	// This one is for paginated lists
	Page<User> findByRole_Name(String roleName, Pageable pageable); // UPDATED

	// --- NEW: Find by role name NOT ---
	List<User> findByRole_NameNot(String roleName);

	// --- Search customers ---
	@Query("SELECT u FROM User u WHERE u.role.name = 'ROLE_CUSTOMER' AND (" // UPDATED
			+ "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "u.phone LIKE CONCAT('%', :keyword, '%'))")
	Page<User> findByRoleAndSearchKeyword(@Param("keyword") String keyword, Pageable pageable);

	// --- Search admins ---
	@Query("SELECT u FROM User u WHERE u.role.name != 'ROLE_CUSTOMER' AND (" // UPDATED: Changed logic
			+ "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
	Page<User> findAdminsBySearchKeyword(@Param("keyword") String keyword, Pageable pageable);

	// --- For inactivity check ---
	List<User> findByRole_NameAndStatus(String roleName, String status); // UPDATED

	// --- For stats cards ---
	long countByRole_NameAndStatus(String roleName, String status); // UPDATED

	long countByRole_Name(String roleName); // NEW: Fixes compile error (was countByRole)

	// --- NEW: Count active admins ---
	@Query("SELECT COUNT(u) FROM User u WHERE u.role.name != 'ROLE_CUSTOMER' AND u.status = 'ACTIVE'")
	long countActiveAdmins();

	// --- NEW: For Dashboard Stats ---

	/**
	 * Counts users of a specific role created between two dates. * @param roleName
	 * The role name to check (e.g., "ROLE_CUSTOMER"). * @param start The start
	 * timestamp. * @param end The end timestamp.
	 * 
	 * @return The count of new users.
	 */
	long countByRole_NameAndCreatedAtBetween(String roleName, @Param("start") LocalDateTime start, // UPDATED
			@Param("end") LocalDateTime end);

}