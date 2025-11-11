package com.toastedsiopao.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam; // ADDED
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.toastedsiopao.dto.CustomerSignUpDto;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.SiteSettingsService;

import jakarta.servlet.http.HttpServletRequest; // ADDED
import jakarta.validation.Valid;

@Controller
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);

	@Autowired
	private CustomerService customerService;

	@Autowired
	private SiteSettingsService siteSettingsService;

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/login")
	public String showLoginForm() {
		return "login";
	}

	@GetMapping("/signup")
	public String showSignupForm(Model model) {
		if (!model.containsAttribute("customerSignUpDto")) {
			model.addAttribute("customerSignUpDto", new CustomerSignUpDto());
		}
		return "signup";
	}

	@PostMapping("/signup")
	public String processSignup(@Valid @ModelAttribute("customerSignUpDto") CustomerSignUpDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			log.warn("Signup form validation failed (DTO level). Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerSignUpDto",
					result);
			redirectAttributes.addFlashAttribute("customerSignUpDto", userDto);
			return "redirect:/signup";
		}

		try {
			customerService.saveCustomer(userDto);
			log.info("Signup successful for username: {}", userDto.getUsername());
			redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
			return "redirect:/login";

		} catch (IllegalArgumentException e) {
			log.warn("Signup failed (Service level validation): {}", e.getMessage());

			if (e.getMessage().contains("Username already exists")) {
				result.rejectValue("username", "customerSignUpDto.username", e.getMessage());
			} else if (e.getMessage().contains("Email already exists")) {
				result.rejectValue("email", "customerSignUpDto.email", e.getMessage());
			} else if (e.getMessage().contains("Passwords do not match")) {
				result.rejectValue("confirmPassword", "customerSignUpDto.confirmPassword", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
			}

			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerSignUpDto",
					result);
			redirectAttributes.addFlashAttribute("customerSignUpDto", userDto);
			return "redirect:/signup";

		} catch (Exception e) {
			log.error("Unexpected error during signup for username {}: {}", userDto.getUsername(), e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"An unexpected error occurred during registration. Please try again later.");
			redirectAttributes.addFlashAttribute("customerSignUpDto", userDto);
			return "redirect:/signup";
		}
	}

	// --- ADDED: Method to handle the "Forgot Password" modal submission ---
	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam("email") String email, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {

		try {
			// 1. Get the base URL (e.g., "http://localhost:8080")
			String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

			// 2. Call service to find user, generate token, and send email
			customerService.processPasswordForgotRequest(email, baseUrl);

			// 3. Set a generic success message (for security, don't confirm if email
			// exists)
			redirectAttributes.addFlashAttribute("successMessage",
					"If an account with that email exists, a password reset link has been sent.");

		} catch (Exception e) {
			// This is likely a mail server error (e.g., bad credentials, server down)
			log.error("Error processing password reset for email {}: {}", email, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"Error sending reset email. Please try again later or contact support.");
		}

		return "redirect:/login";
	}
}