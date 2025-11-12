package com.toastedsiopao.config;

import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

	@Autowired
	private SiteSettingsService siteSettingsService;

	@ModelAttribute("siteSettings")
	public SiteSettings addSiteSettingsToModel() {
		return siteSettingsService.getSiteSettings();
	}
}