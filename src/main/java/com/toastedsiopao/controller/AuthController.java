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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.util.StringUtils;

import com.toastedsiopao.dto.CustomerSignUpDto;
import com.toastedsiopao.dto.PasswordResetDto;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.SiteSettingsService;

import jakarta.servlet.http.HttpServletRequest;
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
	public String showSignupForm(Model model, @RequestParam(value = "source", required = false) String source) {
		if (!model.containsAttribute("customerSignUpDto")) {
			model.addAttribute("customerSignUpDto", new CustomerSignUpDto());
		}

		if ("checkout".equals(source)) {
			model.addAttribute("checkoutMessage", "Please create an account to proceed with your order.");
		}

		return "signup";
	}

	@PostMapping("/signup")
	public String processSignup(@Valid @ModelAttribute("customerSignUpDto") CustomerSignUpDto userDto,
			BindingResult result, @RequestParam(value = "source", required = false) String source,
			RedirectAttributes redirectAttributes, HttpServletRequest request) {

		if (result.hasErrors()) {
			log.warn("Signup form validation failed (DTO level). Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.customerSignUpDto",
					result);
			redirectAttributes.addFlashAttribute("customerSignUpDto", userDto);
			return "redirect:/signup" + (StringUtils.hasText(source) ? "?source=" + source : "");
		}

		try {
			String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null).build()
					.toUriString();

			customerService.saveCustomer(userDto, baseUrl);
			log.info("Signup successful for username: {}", userDto.getUsername());

			redirectAttributes.addFlashAttribute("successMessage",
					"Registration successful! Please check your email to verify your account.");

			String redirectUrl = "/login";
			if ("checkout".equals(source)) {
				redirectUrl = "/login?source=checkout";
			}
			return "redirect:" + redirectUrl;

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
			return "redirect:/signup" + (StringUtils.hasText(source) ? "?source=" + source : "");

		} catch (Exception e) {
			log.error("Unexpected error during signup for username {}: {}", userDto.getUsername(), e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"An unexpected error occurred during registration. Please try again later.");
			redirectAttributes.addFlashAttribute("customerSignUpDto", userDto);
			return "redirect:/signup" + (StringUtils.hasText(source) ? "?source=" + source : "");
		}
	}

	// --- UPDATED: Verification now uses explicit URL parameters for feedback ---
	@GetMapping("/verify")
	public String verifyAccount(@RequestParam("id") Long userId, @RequestParam("token") String token) {

		String result = customerService.verifyAccount(userId, token);

		if ("SUCCESS".equals(result)) {
			return "redirect:/login?verified=success";
		} else if ("ALREADY_VERIFIED".equals(result)) {
			return "redirect:/login?verified=already";
		} else {
			return "redirect:/login?error=invalid_token";
		}
	}

	@PostMapping("/forgot-password")
	public String processForgotPassword(@RequestParam("email") String email, HttpServletRequest request,
			RedirectAttributes redirectAttributes) {

		try {
			String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

			customerService.processPasswordForgotRequest(email, baseUrl);

			redirectAttributes.addFlashAttribute("successMessage",
					"If an account with that email exists, a password reset link has been sent.");

		} catch (Exception e) {
			log.error("Error processing password reset for email {}: {}", email, e.getMessage(), e);
			redirectAttributes.addFlashAttribute("errorMessage",
					"Error sending reset email. Please try again later or contact support.");
		}

		return "redirect:/login";
	}

	@GetMapping("/reset-password")
	public String showResetPasswordForm(@RequestParam("token") String token, Model model,
			RedirectAttributes redirectAttributes) {

		if (!customerService.validatePasswordResetToken(token)) {
			log.warn("Invalid or expired token used: {}", token);
			redirectAttributes.addFlashAttribute("errorMessage",
					"Invalid or expired password reset link. Please try again.");
			return "redirect:/login";
		}

		PasswordResetDto dto = new PasswordResetDto();
		dto.setToken(token);
		model.addAttribute("passwordResetDto", dto);

		return "reset-password";
	}

	@PostMapping("/reset-password")
	public String processResetPassword(@Valid @ModelAttribute("passwordResetDto") PasswordResetDto passwordResetDto,
			BindingResult result, RedirectAttributes redirectAttributes, Model model) {

		if (result.hasErrors()) {
			model.addAttribute("passwordResetDto", passwordResetDto);
			return "reset-password";
		}

		try {
			customerService.resetPassword(passwordResetDto);
			redirectAttributes.addFlashAttribute("successMessage",
					"Your password has been reset successfully! Please log in.");
			return "redirect:/login";

		} catch (IllegalArgumentException e) {
			log.warn("Password reset failed: {}", e.getMessage());
			result.reject("global", e.getMessage());
			model.addAttribute("passwordResetDto", passwordResetDto);
			model.addAttribute("errorMessage", e.getMessage());
			return "reset-password";

		} catch (Exception e) {
			log.error("Unexpected error during password reset: {}", e.getMessage(), e);
			model.addAttribute("passwordResetDto", passwordResetDto);
			model.addAttribute("errorMessage", "An unexpected error occurred. Please try again.");
			return "reset-password";
		}
	}
}