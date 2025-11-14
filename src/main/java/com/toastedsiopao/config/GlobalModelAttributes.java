package com.toastedsiopao.config;

import com.toastedsiopao.model.Notification; // ADDED
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User; // ADDED
import com.toastedsiopao.service.CustomerService; // ADDED
import com.toastedsiopao.service.NotificationService; // ADDED
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // ADDED
import org.springframework.security.core.context.SecurityContextHolder; // ADDED
import org.springframework.ui.Model; // ADDED
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal; // ADDED
import java.util.List; // ADDED

@ControllerAdvice
public class GlobalModelAttributes {

	@Autowired
	private SiteSettingsService siteSettingsService;

	// --- ADDED ---
	@Autowired
	private NotificationService notificationService;

	@Autowired
	private CustomerService customerService;
	// --- END ADDED ---

	@ModelAttribute("siteSettings")
	public SiteSettings addSiteSettingsToModel() {
		return siteSettingsService.getSiteSettings();
	}

	// --- ADDED: New method for notifications ---
	@ModelAttribute
	public void addGlobalNotificationAttributes(Model model, Principal principal) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || principal == null) {
			// No user logged in, add empty/zero values to prevent Thymeleaf errors
			model.addAttribute("unreadAdminNotificationCount", 0L);
			model.addAttribute("unreadUserNotificationCount", 0L);
			model.addAttribute("adminNotifications", List.of());
			model.addAttribute("userNotifications", List.of());
			return;
		}

		boolean isAdmin = authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("VIEW_DASHBOARD"));

		boolean isCustomer = authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

		if (isAdmin) {
			// Admin is logged in
			List<Notification> adminNotifications = notificationService.getUnreadAdminNotifications(5);
			long adminCount = notificationService.countUnreadAdminNotifications();
			model.addAttribute("adminNotifications", adminNotifications);
			model.addAttribute("unreadAdminNotificationCount", adminCount);
			
			// Add empty user attributes to prevent errors if an admin somehow lands on a customer page
			model.addAttribute("userNotifications", List.of());
			model.addAttribute("unreadUserNotificationCount", 0L);

		} else if (isCustomer) {
			// Customer is logged in
			User user = customerService.findByUsername(principal.getName());
			if (user != null) {
				List<Notification> userNotifications = notificationService.getUnreadUserNotifications(user, 5);
				long userCount = notificationService.countUnreadUserNotifications(user);
				model.addAttribute("userNotifications", userNotifications);
				model.addAttribute("unreadUserNotificationCount", userCount);
			} else {
				model.addAttribute("userNotifications", List.of());
				model.addAttribute("unreadUserNotificationCount", 0L);
			}
			
			// Add empty admin attributes
			model.addAttribute("adminNotifications", List.of());
			model.addAttribute("unreadAdminNotificationCount", 0L);
		}
	}
	// --- END ADDED ---
}