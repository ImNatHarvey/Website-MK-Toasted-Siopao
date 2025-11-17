package com.toastedsiopao.controller;

import com.toastedsiopao.dto.IssueReportDto; 
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.SiteSettings;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.IssueReportService; 
import com.toastedsiopao.service.NotificationService; 
import com.toastedsiopao.service.OrderService;
import com.toastedsiopao.service.ReportService; 
import com.toastedsiopao.service.SiteSettingsService;
import jakarta.validation.Valid; 
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource; 
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders; 
import org.springframework.http.MediaType; 
import org.springframework.http.ResponseEntity; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult; 
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable; 
import org.springframework.web.bind.annotation.PostMapping; 
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile; 
import org.springframework.web.servlet.mvc.support.RedirectAttributes; 

import java.io.ByteArrayInputStream; 
import java.io.IOException; 
import java.security.Principal;
import java.util.Optional; 

@Controller
@RequestMapping("/u")
public class CustomerHistoryController {
	
	private static final Logger log = LoggerFactory.getLogger(CustomerHistoryController.class);

	@Autowired
	private SiteSettingsService siteSettingsService;

	@Autowired
	private CustomerService customerService;

	@Autowired
	private OrderService orderService;
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private ReportService reportService;
	
	@Autowired
	private IssueReportService issueReportService;

	@ModelAttribute
	public void addCommonAttributes(Model model) {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		model.addAttribute("siteSettings", settings);
		
		// --- START: REMOVED ---
		// if (!model.containsAttribute("issueReportDto")) {
		// 	model.addAttribute("issueReportDto", new IssueReportDto());
		// }
		// --- END: REMOVED ---
	}

	@GetMapping("/history")
	public String customerHistory(Model model, Principal principal,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "5") int size) { 

		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return "redirect:/logout";
		}

		Pageable pageable = PageRequest.of(page, size);
		Page<Order> orderPage = orderService.findOrdersByUserAndStatus(user, status, pageable);

		model.addAttribute("orderPage", orderPage);
		model.addAttribute("currentStatus", status);

		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", orderPage.getTotalPages());
		model.addAttribute("totalItems", orderPage.getTotalElements());
		model.addAttribute("size", size);

		return "customer/history";
	}

	@PostMapping("/history/cancel/{id}")
	public String cancelOrder(@PathVariable("id") Long orderId, Principal principal,
			RedirectAttributes redirectAttributes) {

		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return "redirect:/logout";
		}

		try {
			orderService.cancelOrder(orderId, user);
			redirectAttributes.addFlashAttribute("orderSuccess", "Order #ORD-" + orderId + " has been cancelled.");
			
			// --- Admin Notification ---
			String notifMessage = "Customer " + user.getUsername() + " cancelled order #" + orderId + ". Stock has been reversed.";
			String notifLink = "/admin/orders?status=CANCELLED";
			notificationService.createAdminNotification(notifMessage, notifLink);
			
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("orderError", e.getMessage());
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("orderError", "An unexpected error occurred. Please try again.");
		}

		return "redirect:/u/history";
	}
	
	@PostMapping("/history/report-issue")
	public String reportIssue(@Valid @ModelAttribute("issueReportDto") IssueReportDto reportDto,
							  BindingResult bindingResult,
							  @RequestParam("attachmentFile") MultipartFile attachmentFile,
							  Principal principal,
							  RedirectAttributes redirectAttributes) {

		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			return "redirect:/logout"; // Should not happen, but good practice
		}

		if (bindingResult.hasErrors()) {
			log.warn("IssueReportDto validation failed for user: {}", user.getUsername());
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.issueReportDto", bindingResult);
			redirectAttributes.addFlashAttribute("issueReportDto", reportDto);
			redirectAttributes.addFlashAttribute("issueError", "Validation failed. Please check the fields below.");
			// --- This is a bit of a hack to re-open the modal on failure ---
			// We can't use the standard ?showModal= URL param because we're on a different page
			// We'll rely on the modal's JS to check for this attribute
			redirectAttributes.addFlashAttribute("reOpenIssueModal", reportDto.getOrderId());
			return "redirect:/u/history";
		}

		try {
			issueReportService.createIssueReport(user, reportDto, attachmentFile);
			redirectAttributes.addFlashAttribute("issueSuccess", "Your issue report for Order #ORD-" + reportDto.getOrderId() + " has been submitted successfully.");
		
		} catch (IllegalArgumentException e) {
			log.warn("Issue report submission failed for user {}: {}", user.getUsername(), e.getMessage());
			redirectAttributes.addFlashAttribute("issueReportDto", reportDto);
			redirectAttributes.addFlashAttribute("issueError", e.getMessage());
			redirectAttributes.addFlashAttribute("reOpenIssueModal", reportDto.getOrderId());
		
		} catch (Exception e) {
			log.error("Unexpected error submitting issue report for user {}: {}", user.getUsername(), e.getMessage(), e);
			redirectAttributes.addFlashAttribute("issueReportDto", reportDto);
			redirectAttributes.addFlashAttribute("issueError", "An unexpected error occurred. Please try again.");
			redirectAttributes.addFlashAttribute("reOpenIssueModal", reportDto.getOrderId());
		}

		return "redirect:/u/history";
	}
	
	@GetMapping("/history/invoice/{id}")
	public ResponseEntity<InputStreamResource> downloadMyInvoice(@PathVariable("id") Long orderId, Principal principal) {

		User user = customerService.findByUsername(principal.getName());
		if (user == null) {
			log.warn("Attempt to download invoice by unauthenticated user.");
			return ResponseEntity.status(401).build(); // Unauthorized
		}

		log.info("User {} attempting to download invoice for Order ID: {}", user.getUsername(), orderId);

		try {
			// 1. Fetch the order
			Optional<Order> orderOpt = orderService.findOrderForInvoice(orderId);

			// 2. Security Check: Ensure order exists AND belongs to the logged-in user
			if (orderOpt.isEmpty() || !orderOpt.get().getUser().getId().equals(user.getId())) {
				log.warn("SECURITY: User {} attempted to access invoice for Order ID {} which does not belong to them.", user.getUsername(), orderId);
				return ResponseEntity.notFound().build(); // 404
			}
			
			// 3. Generate the PDF
			ByteArrayInputStream bis = reportService.generateInvoicePdf(orderOpt.get());

			HttpHeaders headers = new HttpHeaders();
			String fileName = "Invoice_ORD-" + orderId + ".pdf";
			
			// inline = view in browser, attachment = download directly
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName);

			return ResponseEntity
					.ok()
					.headers(headers)
					.contentType(MediaType.APPLICATION_PDF)
					.body(new InputStreamResource(bis));

		} catch (IllegalArgumentException e) {
			log.warn("Failed to generate invoice PDF for order {}: {}", orderId, e.getMessage());
			return ResponseEntity.notFound().build(); // 404 if order not found
		} catch (IOException e) {
			log.error("Failed to generate invoice PDF for order {}: {}", orderId, e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		} catch (Exception e) {
			log.error("Unexpected error generating invoice PDF for order {}: {}", orderId, e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}
}