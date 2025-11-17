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
import java.util.List;
import java.util.function.BiConsumer;

@Controller
@RequestMapping("/admin")
public class AdminSiteController {

	private static final Logger log = LoggerFactory.getLogger(AdminSiteController.class);

	private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif");

	@Autowired
	private ActivityLogService activityLogService;

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private FileStorageService fileStorageService;

	@GetMapping("/settings")
	@PreAuthorize("hasAuthority('EDIT_SITE_SETTINGS')")
	public String siteSettings(Model model) {
		log.info("Accessing site settings page");
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
		return "admin/settings";
	}

	private String handleImageUpload(MultipartFile file, boolean removeImage, String currentPath, String defaultPath) {
		String oldPathToDelete = null;
		String finalPath;

		if (removeImage) {
			log.info("User requested image removal. Reverting to default path: {}", defaultPath);
			finalPath = defaultPath;
			if (StringUtils.hasText(currentPath) && !currentPath.equals(defaultPath)
					&& !currentPath.startsWith("/img/")) {
				oldPathToDelete = currentPath;
			}
		} else if (file != null && !file.isEmpty()) {
			log.info("New file detected. Validating...");

			String contentType = file.getContentType();
			if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
				log.warn("Invalid file type uploaded: {}", contentType);
				throw new IllegalArgumentException("Invalid file type. Only JPG, PNG, or GIF are allowed.");
			}

			log.info("File type OK. Storing...");
			try {
				finalPath = fileStorageService.store(file);
				log.info("Set new path to: {}", finalPath);
				if (StringUtils.hasText(currentPath) && !currentPath.equals(defaultPath)
						&& !currentPath.startsWith("/img/")) {
					oldPathToDelete = currentPath;
				}
			} catch (Exception e) {
				log.error("Failed to store new image file: {}. Reverting to default path.", e.getMessage(), e);
				finalPath = defaultPath;
			}
		} else {
			log.debug("No image change. Keeping path: {}", currentPath);
			finalPath = currentPath;
		}

		if (oldPathToDelete != null) {
			log.info("Deleting old custom file: {}", oldPathToDelete);
			fileStorageService.delete(oldPathToDelete);
		}

		return finalPath;
	}

	@PostMapping("/settings/update")
	@PreAuthorize("hasAuthority('EDIT_SITE_SETTINGS')")
	public String updateSiteSettings(@ModelAttribute("siteSettings") SiteSettings formSettings,
			@RequestParam(value = "carouselImage1File", required = false) MultipartFile carouselImage1File,
			@RequestParam(value = "carouselImage2File", required = false) MultipartFile carouselImage2File,
			@RequestParam(value = "carouselImage3File", required = false) MultipartFile carouselImage3File,
			@RequestParam(value = "featureCard1ImageFile", required = false) MultipartFile featureCard1ImageFile,
			@RequestParam(value = "featureCard2ImageFile", required = false) MultipartFile featureCard2ImageFile,
			@RequestParam(value = "featureCard3ImageFile", required = false) MultipartFile featureCard3ImageFile,
			@RequestParam(value = "featureCard4ImageFile", required = false) MultipartFile featureCard4ImageFile,
			@RequestParam(value = "whyUsImageFile", required = false) MultipartFile whyUsImageFile,
			@RequestParam(value = "aboutImageFile", required = false) MultipartFile aboutImageFile,
			@RequestParam(value = "gcashQrCodeFile", required = false) MultipartFile gcashQrCodeFile,
			@RequestParam(value = "removeCarouselImage1", defaultValue = "false") boolean removeCarouselImage1,
			@RequestParam(value = "removeCarouselImage2", defaultValue = "false") boolean removeCarouselImage2,
			@RequestParam(value = "removeCarouselImage3", defaultValue = "false") boolean removeCarouselImage3,
			@RequestParam(value = "removeFeatureCard1Image", defaultValue = "false") boolean removeFeatureCard1Image,
			@RequestParam(value = "removeFeatureCard2Image", defaultValue = "false") boolean removeFeatureCard2Image,
			@RequestParam(value = "removeFeatureCard3Image", defaultValue = "false") boolean removeFeatureCard3Image,
			@RequestParam(value = "removeFeatureCard4Image", defaultValue = "false") boolean removeFeatureCard4Image,
			@RequestParam(value = "removeWhyUsImage", defaultValue = "false") boolean removeWhyUsImage,
			@RequestParam(value = "removeAboutImage", defaultValue = "false") boolean removeAboutImage,
			@RequestParam(value = "removeGcashQrCode", defaultValue = "false") boolean removeGcashQrCode,
			RedirectAttributes redirectAttributes, Principal principal) {

		log.info("Updating site settings...");

		SiteSettings settingsToUpdate = siteSettingsService.getSiteSettings();
		SiteSettings defaultSettings = new SiteSettings();

		try {
			settingsToUpdate.setWebsiteName(formSettings.getWebsiteName());
			settingsToUpdate.setGcashName(formSettings.getGcashName());
			settingsToUpdate.setGcashNumber(formSettings.getGcashNumber());
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
			settingsToUpdate.setFooterText(formSettings.getFooterText());
			settingsToUpdate.setGcashQrCodeImage(handleImageUpload(gcashQrCodeFile, removeGcashQrCode,
					settingsToUpdate.getGcashQrCodeImage(), defaultSettings.getGcashQrCodeImage()));
			settingsToUpdate.setCarouselImage1(handleImageUpload(carouselImage1File, removeCarouselImage1,
					settingsToUpdate.getCarouselImage1(), defaultSettings.getCarouselImage1()));
			settingsToUpdate.setCarouselImage2(handleImageUpload(carouselImage2File, removeCarouselImage2,
					settingsToUpdate.getCarouselImage2(), defaultSettings.getCarouselImage2()));
			settingsToUpdate.setCarouselImage3(handleImageUpload(carouselImage3File, removeCarouselImage3,
					settingsToUpdate.getCarouselImage3(), defaultSettings.getCarouselImage3()));
			settingsToUpdate.setFeatureCard1Image(handleImageUpload(featureCard1ImageFile, removeFeatureCard1Image,
					settingsToUpdate.getFeatureCard1Image(), defaultSettings.getFeatureCard1Image()));
			settingsToUpdate.setFeatureCard2Image(handleImageUpload(featureCard2ImageFile, removeFeatureCard2Image,
					settingsToUpdate.getFeatureCard2Image(), defaultSettings.getFeatureCard2Image()));
			settingsToUpdate.setFeatureCard3Image(handleImageUpload(featureCard3ImageFile, removeFeatureCard3Image,
					settingsToUpdate.getFeatureCard3Image(), defaultSettings.getFeatureCard3Image()));
			settingsToUpdate.setFeatureCard4Image(handleImageUpload(featureCard4ImageFile, removeFeatureCard4Image,
					settingsToUpdate.getFeatureCard4Image(), defaultSettings.getFeatureCard4Image()));
			settingsToUpdate.setWhyUsImage(handleImageUpload(whyUsImageFile, removeWhyUsImage,
					settingsToUpdate.getWhyUsImage(), defaultSettings.getWhyUsImage()));
			settingsToUpdate.setAboutImage(handleImageUpload(aboutImageFile, removeAboutImage,
					settingsToUpdate.getAboutImage(), defaultSettings.getAboutImage()));

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