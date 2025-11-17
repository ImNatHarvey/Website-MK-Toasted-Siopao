package com.toastedsiopao.config;

import com.toastedsiopao.dto.IssueReportDto; // --- ADDED ---
import com.toastedsiopao.model.CartItem; 
import com.toastedsiopao.model.Notification;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.CartService; 
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.NotificationService;
import com.toastedsiopao.service.SiteSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.math.BigDecimal; 
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
	
	@Autowired
	private CartService cartService;

	// --- START: NEW METHOD ---
	@ModelAttribute("issueReportDto")
	public IssueReportDto issueReportDto() {
		return new IssueReportDto();
	}
	// --- END: NEW METHOD ---

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
			
			model.addAttribute("cartItems", List.of());
			model.addAttribute("cartTotal", BigDecimal.ZERO);
			model.addAttribute("cartItemCount", 0);
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
			
			model.addAttribute("cartItems", List.of());
			model.addAttribute("cartTotal", BigDecimal.ZERO);
			model.addAttribute("cartItemCount", 0);

		} else if (isCustomer) {
			// Customer is logged in
			User user = customerService.findByUsername(principal.getName());
			if (user != null) {
				List<Notification> userNotifications = notificationService.getUnreadUserNotifications(user, 5);
				long userCount = notificationService.countUnreadUserNotifications(user);
				model.addAttribute("userNotifications", userNotifications);
				model.addAttribute("unreadUserNotificationCount", userCount);
				
				List<CartItem> cartItems = cartService.getCartForUser(user);
				BigDecimal cartTotal = cartService.getCartTotal(cartItems);
				int cartItemCount = cartService.getCartItemCount(cartItems);

				model.addAttribute("cartItems", cartItems);
				model.addAttribute("cartTotal", cartTotal);
				model.addAttribute("cartItemCount", cartItemCount);
				
			} else {
				model.addAttribute("userNotifications", List.of());
				model.addAttribute("unreadUserNotificationCount", 0L);
				model.addAttribute("cartItems", List.of());
				model.addAttribute("cartTotal", BigDecimal.ZERO);
				model.addAttribute("cartItemCount", 0);
			}
			
			model.addAttribute("adminNotifications", List.of());
			model.addAttribute("unreadAdminNotificationCount", 0L);
		}
	}
}