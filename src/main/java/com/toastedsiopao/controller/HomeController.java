package com.toastedsiopao.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.toastedsiopao.dto.UserDto;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.UserService; // Import UserService

import jakarta.validation.Valid;

@Controller
public class HomeController {

	// --- Inject UserService ---
	@Autowired
	private UserService userService;
	// --- End Injection ---

	@GetMapping("/")
	public String home() {
		return "index";
	}

	// --- SIGNUP (GET Request) ---
	@GetMapping("/signup")
	public String showSignupForm(Model model) {
		// Ensure the form always has a UserDto object to bind to
		if (!model.containsAttribute("userDto")) {
			model.addAttribute("userDto", new UserDto());
		}
		return "signup";
	}

	// --- SIGNUP (POST Request) ---
	@PostMapping("/signup")
	public String processSignup(@Valid @ModelAttribute("userDto") UserDto userDto, BindingResult result, // For
																											// validation
																											// results
			RedirectAttributes redirectAttributes) { // For success/error messages after redirect

		// 1. Check if username already exists
		User existingUser = userService.findByUsername(userDto.getUsername());
		if (existingUser != null) {
			result.rejectValue("username", "userDto.username", "Username already exists"); // More specific error code
		}

		// 2. Check if passwords match (only if password field itself has no other
		// errors)
		if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
				&& !userDto.getPassword().equals(userDto.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "userDto.confirmPassword", "Passwords do not match");
		}

		// 3. Check for standard validation errors (@NotBlank, @Size, etc.) + our custom
		// checks
		if (result.hasErrors()) {
			// Add the DTO and errors as Flash Attributes so they survive the redirect
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.userDto", result);
			redirectAttributes.addFlashAttribute("userDto", userDto);
			return "redirect:/signup"; // Redirect back to the GET mapping to show errors
		}

		// 4. If validation passes, save the new customer
		try {
			userService.saveCustomer(userDto);
			// Add a success message (Flash Attribute) and redirect to login
			redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
			return "redirect:/login";
		} catch (Exception e) {
			// Handle potential saving errors (e.g., database issues)
			redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred during registration.");
			// Pass DTO back in case of error
			redirectAttributes.addFlashAttribute("userDto", userDto);
			return "redirect:/signup";
		}
	}
	// --- END SIGNUP ---

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
