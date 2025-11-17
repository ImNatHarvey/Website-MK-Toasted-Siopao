package com.toastedsiopao.service;

import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.repository.SiteSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class SiteSettingsServiceImpl implements SiteSettingsService {

	private static final Logger log = LoggerFactory.getLogger(SiteSettingsServiceImpl.class);
	private static final Long SETTINGS_ID = 1L;

	@Autowired
	private SiteSettingsRepository settingsRepository;

	@Override
	public SiteSettings getSiteSettings() {
		SiteSettings settings = settingsRepository.findById(SETTINGS_ID).orElseGet(this::createDefaultSettings);

		if (!StringUtils.hasText(settings.getFooterText())) {
			log.warn("Footer text was null/empty in database, setting default.");
			settings.setFooterText("© 2025 MK Toasted Siopao | All Rights Reserved");
		}

		return settings;
	}

	@Override
	public SiteSettings save(SiteSettings settings) {
		settings.setId(SETTINGS_ID);
		return settingsRepository.save(settings);
	}

	private SiteSettings createDefaultSettings() {
		log.info("No site settings found with ID 1. Creating default settings...");
		SiteSettings defaultSettings = new SiteSettings();
		defaultSettings.setId(SETTINGS_ID);
		return settingsRepository.save(defaultSettings);
	}
}