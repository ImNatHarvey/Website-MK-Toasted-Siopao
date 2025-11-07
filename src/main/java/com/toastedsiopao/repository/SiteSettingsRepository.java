package com.toastedsiopao.repository;

import com.toastedsiopao.model.SiteSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteSettingsRepository extends JpaRepository<SiteSettings, Long> {

	// We'll always be looking for the settings with ID = 1
	Optional<SiteSettings> findById(Long id);
}