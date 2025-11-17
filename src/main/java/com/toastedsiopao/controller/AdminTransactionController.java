package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.service.IssueReportService; // --- ADDED ---
import com.toastedsiopao.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize; 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List; // --- ADDED ---
import java.util.Map; // --- ADDED ---
import java.util.stream.Collectors; // --- ADDED ---

@Controller
@RequestMapping("/admin")
public class AdminTransactionController {

	private static final Logger log = LoggerFactory.getLogger(AdminTransactionController.class);

	@Autowired
	private OrderService orderService;
	
	// --- START: ADDED ---
	@Autowired
	private IssueReportService issueReportService;
	// --- END: ADDED ---

	@GetMapping("/transactions")
	@PreAuthorize("hasAuthority('VIEW_TRANSACTIONS')")
	public String viewTransactions(Model model, @RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "startDate", required = false) String startDate, // NEW
			@RequestParam(value = "endDate", required = false) String endDate, // NEW
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size) {

		log.info("Accessing transaction history page. Keyword: {}, Page: {}, Start: {}, End: {}", keyword, page,
				startDate, endDate);
		Pageable pageable = PageRequest.of(page, size);

		Page<Order> transactionPage = orderService.searchOrders(keyword, null, startDate, endDate, pageable); 

		BigDecimal totalRevenue = orderService.getTotalRevenueAllTime();
		long totalTransactions = orderService.getTotalTransactionsAllTime();
		BigDecimal avgOrderValue = BigDecimal.ZERO;
		if (totalTransactions > 0) {
			avgOrderValue = totalRevenue.divide(new BigDecimal(totalTransactions), 2, RoundingMode.HALF_UP);
		}

		model.addAttribute("transactionPage", transactionPage);
		model.addAttribute("transactions", transactionPage.getContent());
		model.addAttribute("keyword", keyword);
		model.addAttribute("startDate", startDate); 
		model.addAttribute("endDate", endDate); 
		model.addAttribute("totalRevenue", totalRevenue);
		model.addAttribute("totalTransactions", totalTransactions);
		model.addAttribute("avgOrderValue", avgOrderValue);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", transactionPage.getTotalPages());
		model.addAttribute("totalItems", transactionPage.getTotalElements());
		model.addAttribute("size", size);
		
		// --- START: ADDED ---
		// Fetch open issue counts for the orders on the current page
		List<Long> orderIdsOnPage = transactionPage.getContent().stream()
				.map(Order::getId)
				.collect(Collectors.toList());
		
		Map<Long, Long> openIssueCounts = issueReportService.getOpenIssueCountsForOrders(orderIdsOnPage);
		model.addAttribute("openIssueCounts", openIssueCounts);
		// --- END: ADDED ---

		return "admin/transactions";
	}
}