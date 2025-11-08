package com.toastedsiopao.repository;

import com.toastedsiopao.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

	Optional<Role> findByName(String name);

	Optional<Role> findByNameIgnoreCase(String name);

	List<Role> findByNameNot(String name);
}