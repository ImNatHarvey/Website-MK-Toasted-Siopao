package com.toastedsiopao.controller;

import org.springframework.stereotype.Controller;
// Removed Model, BindingResult, etc. imports for now
import org.springframework.web.bind.annotation.GetMapping;
// Removed @Autowired UserService for now
// Removed @PostMapping, @Valid, @ModelAttribute etc. for now

@Controller
public class HomeController {

	// Removed UserService injection

	@GetMapping("/")
	public String home() {
		return "index";
	}

	// --- SIGNUP (GET only for now) ---
	@GetMapping("/signup")
	public String showSignupForm() {
		// No ModelAttribute needed right now
		return "signup";
	}
	// --- Removed POST /signup ---

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
