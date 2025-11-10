package com.toastedsiopao.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.toastedsiopao.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.role r WHERE u.username = :username")
	Optional<User> findByUsername(@Param("username") String username);

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.role r WHERE u.email = :email")
	Optional<User> findByEmail(@Param("email") String email);

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.role r WHERE r.name = :roleName")
	List<User> findByRole_Name(@Param("roleName") String roleName);

	@Query(value = "SELECT u FROM User u LEFT JOIN FETCH u.role r WHERE r.name = :roleName", countQuery = "SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
	Page<User> findByRole_Name(@Param("roleName") String roleName, Pageable pageable);

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.role r WHERE r.name != :roleName")
	List<User> findByRole_NameNot(@Param("roleName") String roleName);

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.role r WHERE r.name = 'ROLE_CUSTOMER' AND ("
			+ "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "u.phone LIKE CONCAT('%', :keyword, '%'))")
	Page<User> findByRoleAndSearchKeyword(@Param("keyword") String keyword, Pageable pageable);

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.role r WHERE r.name != 'ROLE_CUSTOMER' AND ("
			+ "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
			+ "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
	Page<User> findAdminsBySearchKeyword(@Param("keyword") String keyword, Pageable pageable);

	@Query("SELECT u FROM User u LEFT JOIN FETCH u.role r WHERE r.name = :roleName AND u.status = :status")
	List<User> findByRole_NameAndStatus(@Param("roleName") String roleName, @Param("status") String status);

	long countByRole_NameAndStatus(String roleName, String status);

	long countByRole_Name(String roleName);

	@Query("SELECT COUNT(u) FROM User u WHERE u.role.name != 'ROLE_CUSTOMER' AND u.status = 'ACTIVE'")
	long countActiveAdmins();

	long countByRole_NameAndCreatedAtBetween(String roleName, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

	long countByRole_NameNotAndCreatedAtBetween(String roleName, @Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end);

}