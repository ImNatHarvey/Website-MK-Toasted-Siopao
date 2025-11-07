package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order; // NEW IMPORT
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.FileStorageService;
import com.toastedsiopao.service.OrderService; // NEW IMPORT
import com.toastedsiopao.service.SiteSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // NEW IMPORT
import org.springframework.data.domain.PageRequest; // NEW IMPORT
import org.springframework.data.domain.Pageable; // NEW IMPORT
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal; // NEW IMPORT
import java.math.RoundingMode; // NEW IMPORT
import java.security.Principal;
import java.util.function.BiConsumer;

/**
 * Controller to handle general admin pages like Settings, Transactions, and
 * Activity Log, separating them from the main dashboard logic.
 */
@Controller
@RequestMapping("/admin") // All methods in this class are under /admin
public class AdminSiteController {

	private static final Logger log = LoggerFactory.getLogger(AdminSiteController.class);

	@Autowired
	private ActivityLogService activityLogService;

	@Autowired
	private OrderService orderService; // NEW IMPORT

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private FileStorageService fileStorageService;

	@GetMapping("/transactions")
	public String viewTransactions(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		log.info("Accessing transaction history page. Keyword: {}, Page: {}", keyword, page);
		Pageable pageable = PageRequest.of(page, size);

		// Use the OrderService to find all orders (transactions), filtered by keyword
		// We pass 'null' for status to search all statuses
		Page<Order> transactionPage = orderService.searchOrders(keyword, null, pageable);

		// --- Get Metrics ---
		BigDecimal totalRevenue = orderService.getTotalRevenueAllTime();
		long totalTransactions = orderService.getTotalTransactionsAllTime();
		BigDecimal avgOrderValue = BigDecimal.ZERO;
		if (totalTransactions > 0) {
			avgOrderValue = totalRevenue.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP);
		}

		model.addAttribute("transactionPage", transactionPage);
		model.addAttribute("transactions", transactionPage.getContent());
		model.addAttribute("keyword", keyword);

		// --- Add Metrics to Model ---
		model.addAttribute("totalRevenue", totalRevenue);
		model.addAttribute("totalTransactions", totalTransactions);
		model.addAttribute("avgOrderValue", avgOrderValue);

		// --- Add Pagination Attributes ---
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", transactionPage.getTotalPages());
		model.addAttribute("totalItems", transactionPage.getTotalElements());
		model.addAttribute("size", size);

		return "admin/transactions"; // Renders transactions.html
	}

	@GetMapping("/settings")
	public String siteSettings(Model model) {
		log.info("Accessing site settings page");
		// Load the settings (or create defaults if not exist)
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
		return "admin/settings"; // Renders settings.html
	}

	/**
	 * Helper method to process an image upload for the site settings.
	 *
	 * @param file        The new file from the form.
	 * @param currentPath The existing image path from the database.
	 * @param pathSetter  A BiConsumer that takes the (SiteSettings, String) and
	 *                    sets the new path.
	 * @param settings    The SiteSettings entity being updated.
	 */
	private void processImageUpload(MultipartFile file, String currentPath, BiConsumer<SiteSettings, String> pathSetter,
			SiteSettings settings) {
		if (file != null && !file.isEmpty()) {
			// 1. New file was uploaded
			log.info("New file detected. Storing...");
			try {
				String newPath = fileStorageService.store(file);
				pathSetter.accept(settings, newPath); // Set the new path on the entity
				// 2. Delete the old file if it exists
				if (StringUtils.hasText(currentPath)) {
					fileStorageService.delete(currentPath);
				}
			} catch (Exception e) {
				log.error("Failed to store new image file: {}", e.getMessage(), e);
				// In a real app, you'd add this error to the BindingResult
			}
		} else {
			// 3. No new file, keep the old path
			pathSetter.accept(settings, currentPath);
		}
	}

	@PostMapping("/settings/update")
	public String updateSiteSettings(@ModelAttribute("siteSettings") SiteSettings formSettings,
			// --- All 9 image files ---
			@RequestParam("carouselImage1File") MultipartFile carouselImage1File,
			@RequestParam("carouselImage2File") MultipartFile carouselImage2File,
			@RequestParam("carouselImage3File") MultipartFile carouselImage3File,
			@RequestParam("featureCard1ImageFile") MultipartFile featureCard1ImageFile,
			@RequestParam("featureCard2ImageFile") MultipartFile featureCard2ImageFile,
			@RequestParam("featureCard3ImageFile") MultipartFile featureCard3ImageFile,
			@RequestParam("featureCard4ImageFile") MultipartFile featureCard4ImageFile,
			@RequestParam("whyUsImageFile") MultipartFile whyUsImageFile,
			@RequestParam("aboutImageFile") MultipartFile aboutImageFile, RedirectAttributes redirectAttributes,
			Principal principal) {

		log.info("Updating site settings...");

		// 1. Get the LIVE settings object from the database
		SiteSettings settingsToUpdate = siteSettingsService.getSiteSettings();

		// 2. Copy all text fields from the form object to the live object
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

		// 3. Process image files
		try {
			processImageUpload(carouselImage1File, settingsToUpdate.getCarouselImage1(),
					SiteSettings::setCarouselImage1, settingsToUpdate);
			processImageUpload(carouselImage2File, settingsToUpdate.getCarouselImage2(),
					SiteSettings::setCarouselImage2, settingsToUpdate);
			processImageUpload(carouselImage3File, settingsToUpdate.getCarouselImage3(),
					SiteSettings::setCarouselImage3, settingsToUpdate);
			processImageUpload(featureCard1ImageFile, settingsToUpdate.getFeatureCard1Image(),
					SiteSettings::setFeatureCard1Image, settingsToUpdate);
			processImageUpload(featureCard2ImageFile, settingsToUpdate.getFeatureCard2Image(),
					SiteSettings::setFeatureCard2Image, settingsToUpdate);
			processImageUpload(featureCard3ImageFile, settingsToUpdate.getFeatureCard3Image(),
					SiteSettings::setFeatureCard3Image, settingsToUpdate);
			processImageUpload(featureCard4ImageFile, settingsToUpdate.getFeatureCard4Image(),
					SiteSettings::setFeatureCard4Image, settingsToUpdate);
			processImageUpload(whyUsImageFile, settingsToUpdate.getWhyUsImage(), SiteSettings::setWhyUsImage,
					settingsToUpdate);
			processImageUpload(aboutImageFile, settingsToUpdate.getAboutImage(), SiteSettings::setAboutImage,
					settingsToUpdate);

			// 4. Save the updated entity
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

	@GetMapping("/activity-log")
	public String showActivityLog(Model model) {
		log.info("Fetching activity log"); // Add logging
		model.addAttribute("activityLogs", activityLogService.getAllLogs());
		return "admin/activity-log"; // Renders activity-log.html
	}
}