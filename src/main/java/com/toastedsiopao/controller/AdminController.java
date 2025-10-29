package com.toastedsiopao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * DEPRECATED - All functionality moved to more specific controllers: -
 * AdminDashboardController - AdminProductController - AdminInventoryController
 * - AdminUserController
 *
 * This class can eventually be deleted. It is kept temporarily with a redirect
 * to ensure any old bookmarks to /admin still work.
 */
@Controller
@RequestMapping("/admin")
@Deprecated // Mark as deprecated
public class AdminController {

	/**
	 * Redirects the base /admin path to the admin dashboard.
	 *
	 * @return A redirect string to the dashboard.
	 */
	@GetMapping
	public String redirectToDashboard() {
		return "redirect:/admin/dashboard";
	}

	// All other methods have been moved to the new controllers.
}