package com.toastedsiopao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/u")
public class UserController {

	@GetMapping("/dashboard")
	public String customerDashboard() {
		return "customer/dashboard";
	}

	@GetMapping("/profile")
	public String customerProfile() {
		return "customer/profile";
	}

	@GetMapping("/history")
	public String customerHistory() {
		return "customer/history";
	}

	@GetMapping("/menu")
	public String customerMenu() {
		return "customer/menu";
	}

	@GetMapping("/order")
	public String customerOrder() {
		return "customer/order";
	}
}
