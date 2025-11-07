package com.toastedsiopao.service;

import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.repository.SiteSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SiteSettingsServiceImpl implements SiteSettingsService {

	private static final Logger log = LoggerFactory.getLogger(SiteSettingsServiceImpl.class);
	private static final Long SETTINGS_ID = 1L;

	@Autowired
	private SiteSettingsRepository settingsRepository;

	@Override
	public SiteSettings getSiteSettings() {
		// Try to find settings with ID 1, or create new default settings if not found
		return settingsRepository.findById(SETTINGS_ID).orElseGet(this::createDefaultSettings);
	}

	@Override
	public SiteSettings save(SiteSettings settings) {
		// Ensure the ID is always 1 when saving
		settings.setId(SETTINGS_ID);
		return settingsRepository.save(settings);
	}

	/**
	 * A private method to create and persist the default settings row if it doesn't
	 * exist on first load.
	 */
	private SiteSettings createDefaultSettings() {
		log.info("No site settings found with ID 1. Creating default settings...");
		SiteSettings defaultSettings = new SiteSettings();
		defaultSettings.setId(SETTINGS_ID); // Explicitly set ID 1
		return settingsRepository.save(defaultSettings);
	}
}