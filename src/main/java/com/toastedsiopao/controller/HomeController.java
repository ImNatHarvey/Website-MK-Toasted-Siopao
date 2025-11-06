package com.toastedsiopao.controller;

import org.slf4j.Logger; // Import Logger
import org.slf4j.LoggerFactory; // Import LoggerFactory
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.toastedsiopao.dto.CustomerSignUpDto; // UPDATED IMPORT
import com.toastedsiopao.service.CustomerService; // UPDATED IMPORT

import jakarta.validation.Valid;

@Controller
public class HomeController {

	// --- Add Logger ---
	private static final Logger log = LoggerFactory.getLogger(HomeController.class);
	// --- End Logger ---

	@Autowired
	private CustomerService customerService; // UPDATED INJECTION

	@GetMapping("/")
	public String home() {
		return "index";
	}

	@GetMapping("/signup")
	public String showSignupForm(Model model) {
		// We use "customerSignUpDto" as the object name to match the form
		if (!model.containsAttribute("customerSignUpDto")) {
			model.addAttribute("customerSignUpDto", new CustomerSignUpDto());
		}
		return "signup";
	}

	@PostMapping("/signup")
	public String processSignup(@Valid @ModelAttribute("customerSignUpDto") CustomerSignUpDto userDto,
			BindingResult result, RedirectAttributes redirectAttributes) {

		// 1. Check for standard validation errors
		if (result.hasErrors()) {
			log.warn("Signup form validation failed (DTO level). Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerSignUpDto",
					result);
			redirectAttributes.addFlashAttribute("customerSignUpDto", userDto);
			return "redirect:/signup";
		}

		// 2. Try saving
		try {
			customerService.saveCustomer(userDto); // UPDATED SERVICE CALL
			log.info("Signup successful for username: {}", userDto.getUsername());
			redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
			return "redirect:/login";

		} catch (IllegalArgumentException e) { // Catch validation errors from the service
			log.warn("Signup failed (Service level validation): {}", e.getMessage());

			// --- FIX: Use correct BindingResult object name ---
			if (e.getMessage().contains("Username already exists")) {
				result.rejectValue("username", "customerSignUpDto.username", e.getMessage());
			} else if (e.getMessage().contains("Email already exists")) {
				result.rejectValue("email", "customerSignUpDto.email", e.getMessage());
			} else if (e.getMessage().contains("Passwords do not match")) {
				result.rejectValue("confirmPassword", "customerSignUpDto.confirmPassword", e.getMessage());
			} else {
				redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
			}
			// --- END FIX ---

			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerSignUpDto",
					result);
			redirectAttributes.addFlashAttribute("customerSignUpDto", userDto);
			return "redirect:/signup";

		} catch (Exception e) { // Catch unexpected errors
			log.error("Unexpected error during signup for username {}: {}", userDto.getUsername(), e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"An unexpected error occurred during registration. Please try again later.");
			redirectAttributes.addFlashAttribute("customerSignUpDto", userDto);
			return "redirect:/signup";
		}
	}

	// --- Other mappings remain unchanged ---
	@GetMapping("/login")
	public String showLoginForm() {
		return "login";
	}

	@GetMapping("/menu")
	public String menu() {
		return "menu";
	}

	@GetMapping("/order")
	public String order() {
		return "order";
	}

	@GetMapping("/about")
	public String about() {
		return "about";
	}

	@GetMapping("/access-denied")
	public String accessDenied() {
		return "access-denied";
	}
}