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

import com.toastedsiopao.dto.UserDto;
// Removed User model import as it's not directly used here anymore
import com.toastedsiopao.service.UserService;

import jakarta.validation.Valid;

@Controller
public class HomeController {

	// --- Add Logger ---
	private static final Logger log = LoggerFactory.getLogger(HomeController.class);
	// --- End Logger ---

	@Autowired
	private UserService userService;

	@GetMapping("/")
	public String home() {
		return "index";
	}

	@GetMapping("/signup")
	public String showSignupForm(Model model) {
		if (!model.containsAttribute("userDto")) {
			model.addAttribute("userDto", new UserDto());
		}
		return "signup";
	}

	@PostMapping("/signup")
	public String processSignup(@Valid @ModelAttribute("userDto") UserDto userDto, BindingResult result,
			RedirectAttributes redirectAttributes) {

		// --- Removed Manual Validations (Username Check, Password Match) ---

		// 1. Check for standard validation errors (@NotBlank, @Size, @AssertTrue etc.
		// from DTO)
		if (result.hasErrors()) {
			log.warn("Signup form validation failed (DTO level). Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userDto", result);
			redirectAttributes.addFlashAttribute("userDto", userDto);
			return "redirect:/signup";
		}

		// 2. Try saving - Service layer now handles username/password logic
		try {
			userService.saveCustomer(userDto);
			log.info("Signup successful for username: {}", userDto.getUsername());
			redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
			return "redirect:/login";

		} catch (IllegalArgumentException e) { // Catch validation errors from the service
			log.warn("Signup failed (Service level validation): {}", e.getMessage());
			// Add specific error back to BindingResult or use generic message
			if (e.getMessage().contains("Username already exists")) {
				result.rejectValue("username", "userDto.username", e.getMessage());
			} else if (e.getMessage().contains("Passwords do not match")) {
				result.rejectValue("confirmPassword", "userDto.confirmPassword", e.getMessage());
			} else {
				// Generic message for other service validation errors
				redirectAttributes.addFlashAttribute("errorMessage", "Registration failed: " + e.getMessage());
			}
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userDto", result);
			redirectAttributes.addFlashAttribute("userDto", userDto);
			return "redirect:/signup";

		} catch (Exception e) { // Catch unexpected errors during saving
			log.error("Unexpected error during signup for username {}: {}", userDto.getUsername(), e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"An unexpected error occurred during registration. Please try again later.");
			redirectAttributes.addFlashAttribute("userDto", userDto); // Send DTO back
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