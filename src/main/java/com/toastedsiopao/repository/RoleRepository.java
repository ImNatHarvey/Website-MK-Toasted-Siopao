package com.toastedsiopao.repository;

import com.toastedsiopao.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

	// Find a role by its name (e.g., "Owner")
	Optional<Role> findByName(String name);

	Optional<Role> findByNameIgnoreCase(String name);
}