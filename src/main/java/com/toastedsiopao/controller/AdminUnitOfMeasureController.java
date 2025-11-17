package com.toastedsiopao.controller;

import com.toastedsiopao.dto.UnitOfMeasureDto;
import com.toastedsiopao.model.UnitOfMeasure;
import com.toastedsiopao.service.ActivityLogService;
import com.toastedsiopao.service.AdminService; 
import com.toastedsiopao.service.UnitOfMeasureService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; 
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Optional;

@Controller
@RequestMapping("/admin/inventory/units")
@PreAuthorize("hasAuthority('MANAGE_UNITS')")
public class AdminUnitOfMeasureController {

	private static final Logger log = LoggerFactory.getLogger(AdminUnitOfMeasureController.class);

	@Autowired
	private UnitOfMeasureService unitOfMeasureService;

	@Autowired
	private ActivityLogService activityLogService;

	@Autowired 
	private AdminService adminService;

	@ModelAttribute("unitOfMeasureDto")
	public UnitOfMeasureDto unitOfMeasureDto() {
		return new UnitOfMeasureDto();
	}

	@ModelAttribute("unitOfMeasureUpdateDto")
	public UnitOfMeasureDto unitOfMeasureUpdateDto() {
		return new UnitOfMeasureDto();
	}

	@PostMapping("/add")
	public String addUnitOfMeasure(@Valid @ModelAttribute("unitOfMeasureDto") UnitOfMeasureDto unitDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {
		if (result.hasErrors() || result.hasGlobalErrors()) {
			redirectAttributes.addFlashAttribute("globalError", "Validation failed. Please check the fields below.");
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageUnitsModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}
		try {
			UnitOfMeasure newUnit = unitOfMeasureService.saveFromDto(unitDto);
			activityLogService.logAdminAction(principal.getName(), "ADD_UNIT_OF_MEASURE",
					"Added new unit: " + newUnit.getName() + " (" + newUnit.getAbbreviation() + ")");
			redirectAttributes.addFlashAttribute("unitSuccess", "Unit '" + newUnit.getName() + "' added successfully!");
		} catch (IllegalArgumentException e) {
			log.warn("Validation error adding unit: {}", e.getMessage());
			if (e.getMessage().contains("Unit name")) {
				result.rejectValue("name", "duplicate.unitName", e.getMessage());
			} else if (e.getMessage().contains("Unit abbreviation")) {
				result.rejectValue("abbreviation", "duplicate.unitAbbreviation", e.getMessage());
			} else {
				result.reject("global", e.getMessage());
			}
			redirectAttributes.addFlashAttribute("globalError", "Error adding unit: " + e.getMessage());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureDto", unitDto);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "manageUnitsModal").build()
					.toUriString();
			return "redirect:" + redirectUrl;
		}

		return "redirect:/admin/inventory";
	}

	@PostMapping("/update")
	public String updateUnitOfMeasure(@Valid @ModelAttribute("unitOfMeasureUpdateDto") UnitOfMeasureDto unitDto,
			BindingResult result, RedirectAttributes redirectAttributes, Principal principal,
			UriComponentsBuilder uriBuilder) {

		if (result.hasErrors()) {
			log.warn("Unit update DTO validation failed. Errors: {}", result.getAllErrors());
			redirectAttributes.addFlashAttribute("globalError", "Validation failed. Please check the fields below.");
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureUpdateDto", unitDto);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editUnitModal")
					.queryParam("editId", unitDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;
		}

		try {
			UnitOfMeasure updatedUnit = unitOfMeasureService.updateFromDto(unitDto);
			activityLogService.logAdminAction(principal.getName(), "EDIT_UNIT_OF_MEASURE",
					"Updated unit: " + updatedUnit.getName() + " (ID: " + updatedUnit.getId() + ")");
			redirectAttributes.addFlashAttribute("unitSuccess",
					"Unit '" + updatedUnit.getName() + "' updated successfully!");

		} catch (IllegalArgumentException e) {
			log.warn("Validation error updating unit: {}", e.getMessage());
			if (e.getMessage().contains("Unit name")) {
				result.rejectValue("name", "duplicate.unitName", e.getMessage());
			} else if (e.getMessage().contains("Unit abbreviation")) {
				result.rejectValue("abbreviation", "duplicate.unitAbbreviation", e.getMessage());
			} else {
				result.reject("global", e.getMessage());
			}
			redirectAttributes.addFlashAttribute("globalError", "Error updating unit: " + e.getMessage());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.unitOfMeasureUpdateDto",
					result);
			redirectAttributes.addFlashAttribute("unitOfMeasureUpdateDto", unitDto);
			String redirectUrl = uriBuilder.path("/admin/inventory").queryParam("showModal", "editUnitModal")
					.queryParam("editId", unitDto.getId()).build().toUriString();
			return "redirect:" + redirectUrl;

		}

		return "redirect:/admin/inventory";
	}

	@PostMapping("/delete/{id}")
	public String deleteUnitOfMeasure(@PathVariable("id") Long id,
			@RequestParam(value = "password", required = false) String password, 
			RedirectAttributes redirectAttributes,
			Principal principal) {

		if (!adminService.validateOwnerPassword(password)) {
			redirectAttributes.addFlashAttribute("globalError", "Incorrect Owner Password. Deletion cancelled.");
			return "redirect:/admin/inventory";
		}

		Optional<UnitOfMeasure> unitOpt = unitOfMeasureService.findById(id);
		if (unitOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute("unitError", "Unit not found.");
			return "redirect:/admin/inventory";
		}
		String unitName = unitOpt.get().getName();

		unitOfMeasureService.deleteById(id);

		activityLogService.logAdminAction(principal.getName(), "DELETE_UNIT_OF_MEASURE",
				"Deleted unit: " + unitName + " (ID: " + id + ")");
		redirectAttributes.addFlashAttribute("unitSuccess", "Unit '" + unitName + "' deleted successfully!");

		return "redirect:/admin/inventory";
	}
}