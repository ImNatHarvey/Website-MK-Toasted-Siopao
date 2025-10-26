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
import com.toastedsiopao.service.UserService;

import jakarta.validation.Valid;

@Controller
public class HomeController {

	@Autowired
	private UserService userService; // Inject UserService

	@GetMapping("/")
	public String home() {
		return "index";
	}

	// --- SIGNUP ---
	@GetMapping("/signup")
	public String showSignupForm(Model model) {
		// Pass an empty DTO to the form for data binding
		model.addAttribute("userDto", new UserDto());
		return "signup"; // Return the signup.html template
	}

	@PostMapping("/signup")
	public String processSignup(@Valid @ModelAttribute("userDto") UserDto userDto, BindingResult result, Model model,
			RedirectAttributes redirectAttributes) {

		// 1. Check for basic validation errors (@NotBlank, @Size etc.)
		if (result.hasErrors()) {
			model.addAttribute("userDto", userDto); // Send DTO back to form with errors
			return "signup"; // Return to the signup page
		}

		// 2. Check if passwords match
		if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "error.userDto", "Passwords do not match");
		}

		// 3. Check if username already exists
		User existingUser = userService.findByUsername(userDto.getUsername());
		if (existingUser != null) {
			result.rejectValue("username", "error.userDto", "Username already exists");
		}

		// 4. If there are still errors (password mismatch or existing username), return
		// to form
		if (result.hasErrors()) {
			model.addAttribute("userDto", userDto);
			return "signup";
		}

		// 5. If validation passes, save the new customer
		userService.saveCustomer(userDto);

		// 6. Add a success message and redirect to login
		redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in.");
		return "redirect:/login"; // Redirect to prevent double submission
	}
	// --- END SIGNUP ---

	@GetMapping("/login")
	public String showLoginForm() { // Renamed from login() to avoid confusion
		// Model attributes for success/error messages can be added here if needed
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