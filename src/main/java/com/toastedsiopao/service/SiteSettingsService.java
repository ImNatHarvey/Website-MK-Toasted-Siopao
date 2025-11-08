package com.toastedsiopao.service;

import com.toastedsiopao.model.SiteSettings;

public interface SiteSettingsService {

	SiteSettings getSiteSettings();

	SiteSettings save(SiteSettings settings);
}