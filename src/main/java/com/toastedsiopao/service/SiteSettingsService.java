package com.toastedsiopao.service;

import com.toastedsiopao.model.SiteSettings;

public interface SiteSettingsService {

	/**
	 * Gets the site settings. If no settings exist (ID = 1), it creates and returns
	 * the default settings.
	 *
	 * @return The one and only SiteSettings entity.
	 */
	SiteSettings getSiteSettings();

	/**
	 * Saves the given SiteSettings entity.
	 *
	 * @param settings The entity to save.
	 * @return The saved entity.
	 */
	SiteSettings save(SiteSettings settings);
}