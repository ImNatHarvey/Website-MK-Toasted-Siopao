package com.toastedsiopao.controller;

import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.FileStorageService;
import com.toastedsiopao.service.SiteSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.Principal;
import java.util.function.BiConsumer;

@Controller
@RequestMapping("/admin") 
public class AdminSiteController {

	private static final Logger log = LoggerFactory.getLogger(AdminSiteController.class);

	@Autowired
	private ActivityLogService activityLogService;

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private FileStorageService fileStorageService;

	@GetMapping("/settings")
	@PreAuthorize("hasAuthority('EDIT_SITE_SETTINGS')") // **** ADDED ****
	public String siteSettings(Model model) {
		log.info("Accessing site settings page");
		// Load the settings (or create defaults if not exist)
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
		return "admin/settings"; // Renders settings.html
	}

	private void processImageUpload(MultipartFile file, boolean removeImage, String currentPath, String defaultPath,
			BiConsumer<SiteSettings, String> pathSetter, SiteSettings settings) {

		if (removeImage) {
			log.info("User requested image removal. Reverting to default path: {}", defaultPath);
			pathSetter.accept(settings, defaultPath); 
			if (StringUtils.hasText(currentPath) && !currentPath.equals(defaultPath)
					&& !currentPath.startsWith("/img/")) {
				log.info("Deleting old custom file: {}", currentPath);
				fileStorageService.delete(currentPath);
			}
		} else if (file != null && !file.isEmpty()) {
			log.info("New file detected. Storing...");
			try {
				String newPath = fileStorageService.store(file);
				pathSetter.accept(settings, newPath); 
				log.info("Set new path to: {}", newPath);
				if (StringUtils.hasText(currentPath) && !currentPath.equals(defaultPath)
						&& !currentPath.startsWith("/img/")) {
					log.info("Deleting old custom file: {}", currentPath);
					fileStorageService.delete(currentPath);
				}
			} catch (Exception e) {
				log.error("Failed to store new image file: {}. Reverting to default path.", e.getMessage(), e);
				pathSetter.accept(settings, defaultPath);
			}
		} else {
			log.debug("No image change. Keeping path: {}", currentPath);
			pathSetter.accept(settings, currentPath);
		}
	}

	@PostMapping("/settings/update")
	@PreAuthorize("hasAuthority('EDIT_SITE_SETTINGS')") 
	public String updateSiteSettings(@ModelAttribute("siteSettings") SiteSettings formSettings,
			@RequestParam("carouselImage1File") MultipartFile carouselImage1File,
			@RequestParam("carouselImage2File") MultipartFile carouselImage2File,
			@RequestParam("carouselImage3File") MultipartFile carouselImage3File,
			@RequestParam("featureCard1ImageFile") MultipartFile featureCard1ImageFile,
			@RequestParam("featureCard2ImageFile") MultipartFile featureCard2ImageFile,
			@RequestParam("featureCard3ImageFile") MultipartFile featureCard3ImageFile,
			@RequestParam("featureCard4ImageFile") MultipartFile featureCard4ImageFile,
			@RequestParam("whyUsImageFile") MultipartFile whyUsImageFile,
			@RequestParam("aboutImageFile") MultipartFile aboutImageFile,
			
			@RequestParam(value = "removeCarouselImage1", defaultValue = "false") boolean removeCarouselImage1,
			@RequestParam(value = "removeCarouselImage2", defaultValue = "false") boolean removeCarouselImage2,
			@RequestParam(value = "removeCarouselImage3", defaultValue = "false") boolean removeCarouselImage3,
			@RequestParam(value = "removeFeatureCard1Image", defaultValue = "false") boolean removeFeatureCard1Image,
			@RequestParam(value = "removeFeatureCard2Image", defaultValue = "false") boolean removeFeatureCard2Image,
			@RequestParam(value = "removeFeatureCard3Image", defaultValue = "false") boolean removeFeatureCard3Image,
			@RequestParam(value = "removeFeatureCard4Image", defaultValue = "false") boolean removeFeatureCard4Image,
			@RequestParam(value = "removeWhyUsImage", defaultValue = "false") boolean removeWhyUsImage,
			@RequestParam(value = "removeAboutImage", defaultValue = "false") boolean removeAboutImage,
			RedirectAttributes redirectAttributes, Principal principal) {

		log.info("Updating site settings...");

		SiteSettings settingsToUpdate = siteSettingsService.getSiteSettings();
		
		SiteSettings defaultSettings = new SiteSettings();

		settingsToUpdate.setWebsiteName(formSettings.getWebsiteName());
		settingsToUpdate.setFeaturedProductsTitle(formSettings.getFeaturedProductsTitle());
		settingsToUpdate.setFeatureCard1Title(formSettings.getFeatureCard1Title());
		settingsToUpdate.setFeatureCard1Text(formSettings.getFeatureCard1Text());
		settingsToUpdate.setFeatureCard2Title(formSettings.getFeatureCard2Title());
		settingsToUpdate.setFeatureCard2Text(formSettings.getFeatureCard2Text());
		settingsToUpdate.setFeatureCard3Title(formSettings.getFeatureCard3Title());
		settingsToUpdate.setFeatureCard3Text(formSettings.getFeatureCard3Text());
		settingsToUpdate.setFeatureCard4Title(formSettings.getFeatureCard4Title());
		settingsToUpdate.setFeatureCard4Text(formSettings.getFeatureCard4Text());
		settingsToUpdate.setPromoTitle(formSettings.getPromoTitle());
		settingsToUpdate.setPromoText(formSettings.getPromoText());
		settingsToUpdate.setWhyUsTitle(formSettings.getWhyUsTitle());
		settingsToUpdate.setWhyUsText(formSettings.getWhyUsText());
		settingsToUpdate.setAboutTitle(formSettings.getAboutTitle());
		settingsToUpdate.setAboutDescription1(formSettings.getAboutDescription1());
		settingsToUpdate.setAboutDescription2(formSettings.getAboutDescription2());
		settingsToUpdate.setContactFacebookName(formSettings.getContactFacebookName());
		settingsToUpdate.setContactFacebookUrl(formSettings.getContactFacebookUrl());
		settingsToUpdate.setContactPhoneName(formSettings.getContactPhoneName());
		settingsToUpdate.setContactPhoneUrl(formSettings.getContactPhoneUrl());

		try {
			processImageUpload(carouselImage1File, removeCarouselImage1, settingsToUpdate.getCarouselImage1(),
					defaultSettings.getCarouselImage1(), SiteSettings::setCarouselImage1, settingsToUpdate);

			processImageUpload(carouselImage2File, removeCarouselImage2, settingsToUpdate.getCarouselImage2(),
					defaultSettings.getCarouselImage2(), SiteSettings::setCarouselImage2, settingsToUpdate);

			processImageUpload(carouselImage3File, removeCarouselImage3, settingsToUpdate.getCarouselImage3(),
					defaultSettings.getCarouselImage3(), SiteSettings::setCarouselImage3, settingsToUpdate);

			processImageUpload(featureCard1ImageFile, removeFeatureCard1Image, settingsToUpdate.getFeatureCard1Image(),
					defaultSettings.getFeatureCard1Image(), SiteSettings::setFeatureCard1Image, settingsToUpdate);

			processImageUpload(featureCard2ImageFile, removeFeatureCard2Image, settingsToUpdate.getFeatureCard2Image(),
					defaultSettings.getFeatureCard2Image(), SiteSettings::setFeatureCard2Image, settingsToUpdate);

			processImageUpload(featureCard3ImageFile, removeFeatureCard3Image, settingsToUpdate.getFeatureCard3Image(),
					defaultSettings.getFeatureCard3Image(), SiteSettings::setFeatureCard3Image, settingsToUpdate);

			processImageUpload(featureCard4ImageFile, removeFeatureCard4Image, settingsToUpdate.getFeatureCard4Image(),
					defaultSettings.getFeatureCard4Image(), SiteSettings::setFeatureCard4Image, settingsToUpdate);

			processImageUpload(whyUsImageFile, removeWhyUsImage, settingsToUpdate.getWhyUsImage(),
					defaultSettings.getWhyUsImage(), SiteSettings::setWhyUsImage, settingsToUpdate);

			processImageUpload(aboutImageFile, removeAboutImage, settingsToUpdate.getAboutImage(),
					defaultSettings.getAboutImage(), SiteSettings::setAboutImage, settingsToUpdate);

			siteSettingsService.save(settingsToUpdate);

			activityLogService.logAdminAction(principal.getName(), "EDIT_SITE_SETTINGS",
					"Updated website content and settings.");
			redirectAttributes.addFlashAttribute("siteSuccess", "Site settings updated successfully!");

		} catch (Exception e) {
			log.error("Failed to update site settings: {}", e.getMessage(), e);
			redirectAttributes.addFlashAttribute("siteError", "Error updating settings: " + e.getMessage());
		}

		return "redirect:/admin/settings";
	}
}