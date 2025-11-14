package com.toastedsiopao.controller;

import com.toastedsiopao.dto.OrderSubmitDto; // --- ADDED ---
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User; 
import com.toastedsiopao.service.CustomerService; 
import com.toastedsiopao.service.FileStorageService; // --- ADDED ---
import com.toastedsiopao.service.OrderService; // --- ADDED ---
import com.toastedsiopao.service.SiteSettingsService;
import jakarta.validation.Valid; // --- ADDED ---
import org.slf4j.Logger; // --- ADDED ---
import org.slf4j.LoggerFactory; // --- ADDED ---
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; // --- ADDED ---
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping; // --- ADDED ---
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // --- ADDED ---
import org.springframework.web.multipart.MultipartFile; // --- ADDED ---
import org.springframework.web.servlet.mvc.support.RedirectAttributes; // --- ADDED ---
import org.springframework.util.StringUtils; // --- THIS IS THE FIX ---

import java.security.Principal; 
import java.util.List; // --- ADDED ---

@Controller
@RequestMapping("/u")
public class CustomerOrderController {
	
	// --- ADDED ---
	private static final Logger log = LoggerFactory.getLogger(CustomerOrderController.class);
	private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif");
	// --- END ADDED ---

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private CustomerService customerService;
	
	// --- ADDED ---
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private FileStorageService fileStorageService;
	// --- END ADDED ---

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
	}

	@GetMapping("/order")
	public String customerOrder(Model model, Principal principal) {
		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return "redirect:/logout";
		}
		model.addAttribute("customer", user);
		
		// --- ADDED: Add empty DTO for the modal form ---
		if (!model.containsAttribute("orderDto")) {
			model.addAttribute("orderDto", new OrderSubmitDto());
		}
		// --- END ADDED ---
		
		return "customer/order";
	}
	
	// --- ADDED: New endpoint to handle order submission ---
	@PostMapping("/order/submit")
	public String submitOrder(@Valid @ModelAttribute("orderDto") OrderSubmitDto orderDto,
	                          BindingResult bindingResult,
	                          @RequestParam("receiptFile") MultipartFile receiptFile,
	                          Principal principal,
	                          RedirectAttributes redirectAttributes) {

		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return "redirect:/logout";
		}

		// 1. Validate DTO
		if (bindingResult.hasErrors()) {
			log.warn("OrderSubmitDto validation failed for user: {}", user.getUsername());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.orderDto", bindingResult);
			redirectAttributes.addFlashAttribute("orderDto", orderDto);
			redirectAttributes.addFlashAttribute("orderError", "Validation failed. Please check your details."); // For a toast
			return "redirect:/u/order"; // Redirect back to the order page
		}
		
		// 2. Validate File
		String receiptImagePath = null;
		if (orderDto.getPaymentMethod().equalsIgnoreCase("gcash")) {
			
			// --- ##### MODIFICATION START ##### ---
			// --- NEW: Validate Transaction ID ---
			String txId = orderDto.getTransactionId();
			if (!StringUtils.hasText(txId)) {
				log.warn("GCash order submitted without Transaction ID by user: {}", user.getUsername());
				bindingResult.rejectValue("transactionId", "orderDto.transactionId", "Transaction ID is required for GCash payments.");
				redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.orderDto", bindingResult); // Pass back the error
				redirectAttributes.addFlashAttribute("orderDto", orderDto);
				redirectAttributes.addFlashAttribute("orderError", "Transaction ID is required for GCash payments.");
				return "redirect:/u/order";
			} else if (txId.length() < 13 || !txId.matches("\\d+")) { // Check length and if it's all digits
				log.warn("GCash order submitted with invalid Transaction ID '{}' by user: {}", txId, user.getUsername());
				bindingResult.rejectValue("transactionId", "orderDto.transactionId", "Invalid Transaction ID. It must be 13 digits.");
				redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.orderDto", bindingResult); // Pass back the error
				redirectAttributes.addFlashAttribute("orderDto", orderDto);
				redirectAttributes.addFlashAttribute("orderError", "Invalid Transaction ID. It must be 13 digits.");
				return "redirect:/u/order";
			}
			// --- END NEW ---
			// --- ##### MODIFICATION END ##### ---

			if (receiptFile == null || receiptFile.isEmpty()) {
				log.warn("GCash order submitted without receipt file by user: {}", user.getUsername());
				redirectAttributes.addFlashAttribute("orderDto", orderDto);
				redirectAttributes.addFlashAttribute("orderError", "Payment receipt is required for GCash payments.");
				return "redirect:/u/order";
			}
			
			String contentType = receiptFile.getContentType();
			if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
				log.warn("Invalid file type uploaded by user {}: {}", user.getUsername(), contentType);
				redirectAttributes.addFlashAttribute("orderDto", orderDto);
				redirectAttributes.addFlashAttribute("orderError", "Invalid file type. Only JPG, PNG, or GIF are allowed for receipts.");
				return "redirect:/u/order";
			}

			// 3. Store File
			try {
				receiptImagePath = fileStorageService.store(receiptFile);
				log.info("Stored payment receipt for user {} at: {}", user.getUsername(), receiptImagePath);
			} catch (Exception e) {
				log.error("Failed to store receipt file for user {}: {}", user.getUsername(), e.getMessage(), e);
				redirectAttributes.addFlashAttribute("orderDto", orderDto);
				redirectAttributes.addFlashAttribute("orderError", "Could not save receipt image. Please try again.");
				return "redirect:/u/order";
			}
		} else {
			// Handle Cash on Delivery (COD)
			log.info("Processing Cash on Delivery (COD) order for user: {}", user.getUsername());
		}

		// 4. Create Order (this is the transactional part)
		try {
			orderService.createOrder(user, orderDto, receiptImagePath);
			
			redirectAttributes.addFlashAttribute("orderSuccess", "Your order has been placed successfully!");
			redirectAttributes.addFlashAttribute("clearCart", true); // Flag for JS
			
			return "redirect:/u/history"; // Redirect to order history

		} catch (IllegalArgumentException e) {
			// This catches validation errors like "Insufficient stock"
			log.warn("Order creation failed for user {}: {}", user.getUsername(), e.getMessage());
			redirectAttributes.addFlashAttribute("orderDto", orderDto);
			redirectAttributes.addFlashAttribute("orderError", e.getMessage());
			
			// If stock was the issue, we must delete the uploaded receipt
			if (receiptImagePath != null) {
				fileStorageService.delete(receiptImagePath);
			}
			
			return "redirect:/u/order";
			
		} catch (Exception e) {
			// Catch-all for other runtime errors (e.g., DB connection)
			log.error("Unexpected error creating order for user {}: {}", user.getUsername(), e.getMessage(), e);
			redirectAttributes.addFlashAttribute("orderDto", orderDto);
			redirectAttributes.addFlashAttribute("orderError", "An unexpected error occurred. Please try again.");
			
			if (receiptImagePath != null) {
				fileStorageService.delete(receiptImagePath);
			}
			
			return "redirect:/u/order";
		}
	}
	// --- END ADDED ---
}