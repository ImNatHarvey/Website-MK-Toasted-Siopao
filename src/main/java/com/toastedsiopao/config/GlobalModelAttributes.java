package com.toastedsiopao.config;

import com.toastedsiopao.model.CartItem; // --- ADDED ---
import com.toastedsiopao.model.Notification;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.CartService; // --- ADDED ---
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.NotificationService;
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal; // --- ADDED ---
import java.security.Principal;
import java.util.List;

@ControllerAdvice
public class GlobalModelAttributes {

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private CustomerService customerService;
	
	// --- ADDED ---
	@Autowired
	private CartService cartService;
	// --- END ADDED ---

	@ModelAttribute("siteSettings")
	public SiteSettings addSiteSettingsToModel() {
		return siteSettingsService.getSiteSettings();
	}

	@ModelAttribute
	public void addGlobalNotificationAttributes(Model model, Principal principal) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated() || principal == null) {
			// No user logged in, add empty/zero values to prevent Thymeleaf errors
			model.addAttribute("unreadAdminNotificationCount", 0L);
			model.addAttribute("unreadUserNotificationCount", 0L);
			model.addAttribute("adminNotifications", List.of());
			model.addAttribute("userNotifications", List.of());
			
			// --- ADDED: Empty cart for guests ---
			model.addAttribute("cartItems", List.of());
			model.addAttribute("cartTotal", BigDecimal.ZERO);
			model.addAttribute("cartItemCount", 0);
			// --- END ADDED ---
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
			
			model.addAttribute("userNotifications", List.of());
			model.addAttribute("unreadUserNotificationCount", 0L);
			
			// --- ADDED: Empty cart for admin ---
			model.addAttribute("cartItems", List.of());
			model.addAttribute("cartTotal", BigDecimal.ZERO);
			model.addAttribute("cartItemCount", 0);
			// --- END ADDED ---

		} else if (isCustomer) {
			// Customer is logged in
			User user = customerService.findByUsername(principal.getName());
			if (user != null) {
				List<Notification> userNotifications = notificationService.getUnreadUserNotifications(user, 5);
				long userCount = notificationService.countUnreadUserNotifications(user);
				model.addAttribute("userNotifications", userNotifications);
				model.addAttribute("unreadUserNotificationCount", userCount);
				
				// --- ADDED: Load persistent cart for the customer ---
				List<CartItem> cartItems = cartService.getCartForUser(user);
				BigDecimal cartTotal = cartService.getCartTotal(cartItems);
				int cartItemCount = cartService.getCartItemCount(cartItems);

				model.addAttribute("cartItems", cartItems);
				model.addAttribute("cartTotal", cartTotal);
				model.addAttribute("cartItemCount", cartItemCount);
				// --- END ADDED ---
				
			} else {
				model.addAttribute("userNotifications", List.of());
				model.addAttribute("unreadUserNotificationCount", 0L);
				// --- ADDED: Empty cart if user not found (safety) ---
				model.addAttribute("cartItems", List.of());
				model.addAttribute("cartTotal", BigDecimal.ZERO);
				model.addAttribute("cartItemCount", 0);
				// --- END ADDED ---
			}
			
			// Add empty admin attributes
			model.addAttribute("adminNotifications", List.of());
			model.addAttribute("unreadAdminNotificationCount", 0L);
		}
	}
}