package com.toastedsiopao.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.toastedsiopao.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	// Spring Data JPA will automatically implement this method
	// based on the method name: findBy + fieldName (Username)
	Optional<User> findByUsername(String username);

}