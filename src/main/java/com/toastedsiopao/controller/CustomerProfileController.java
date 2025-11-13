package com.toastedsiopao.controller;

import com.toastedsiopao.dto.CustomerPasswordDto;
import com.toastedsiopao.dto.CustomerProfileDto;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.SiteSettingsService;
import jakarta.validation.Valid; // ADDED
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; // ADDED
import org.springframework.security.core.context.SecurityContextHolder; // ADDED
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // ADDED
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping; // ADDED
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // ADDED

import java.security.Principal;

@Controller
@RequestMapping("/u")
public class CustomerProfileController {

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private CustomerService customerService;

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/profile")
	public String customerProfile(Model model, Principal principal) {
		String username = principal.getName();
		User user = customerService.findByUsername(username);

		if (user == null) {
			return "redirect:/logout";
		}

		if (!model.containsAttribute("profileDto")) {
			CustomerProfileDto profileDto = new CustomerProfileDto();
			profileDto.setFirstName(user.getFirstName());
			profileDto.setLastName(user.getLastName());
			profileDto.setUsername(user.getUsername());
			profileDto.setEmail(user.getEmail());
			profileDto.setPhone(user.getPhone());
			profileDto.setHouseNo(user.getHouseNo());
			profileDto.setLotNo(user.getLotNo());
			profileDto.setBlockNo(user.getBlockNo());
			profileDto.setStreet(user.getStreet());
			profileDto.setBarangay(user.getBarangay());
			profileDto.setMunicipality(user.getMunicipality());
			profileDto.setProvince(user.getProvince());
			model.addAttribute("profileDto", profileDto);
		}

		if (!model.containsAttribute("passwordDto")) {
			model.addAttribute("passwordDto", new CustomerPasswordDto());
		}

		return "customer/profile";
	}

	// --- ADDED ---
	@PostMapping("/profile/update-details")
	public String updateProfileDetails(@Valid @ModelAttribute("profileDto") CustomerProfileDto profileDto,
			BindingResult result, Principal principal, RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileDto", result);
			redirectAttributes.addFlashAttribute("profileDto", profileDto);
			redirectAttributes.addFlashAttribute("profileError", "Validation failed. Please check your information.");
			return "redirect:/u/profile";
		}

		try {
			customerService.updateCustomerProfile(principal.getName(), profileDto);

			// --- Update the username in the security context if it changed ---
			if (!principal.getName().equals(profileDto.getUsername())) {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				User user = (User) auth.getPrincipal();
				user.setUsername(profileDto.getUsername());
			}

			redirectAttributes.addFlashAttribute("profileSuccess", "Your profile has been updated successfully!");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("profileDto", profileDto); // Send back the attempted changes
			redirectAttributes.addFlashAttribute("profileError", e.getMessage());
		}

		return "redirect:/u/profile";
	}

	@PostMapping("/profile/update-password")
	public String updatePassword(@Valid @ModelAttribute("passwordDto") CustomerPasswordDto passwordDto,
			BindingResult result, Principal principal, RedirectAttributes redirectAttributes) {

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.passwordDto", result);
			redirectAttributes.addFlashAttribute("passwordDto", passwordDto);
			redirectAttributes.addFlashAttribute("passwordError", "Validation failed. Please check the fields.");
			return "redirect:/u/profile";
		}

		try {
			customerService.updateCustomerPassword(principal.getName(), passwordDto);
			redirectAttributes.addFlashAttribute("passwordSuccess", "Your password has been changed successfully!");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("passwordDto", new CustomerPasswordDto()); // Clear fields on error
			redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
		}

		return "redirect:/u/profile";
	}
	// --- END ADDED ---
}