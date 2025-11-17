package com.toastedsiopao.controller;

import com.toastedsiopao.dto.IssueReportResponseDto; 
import com.toastedsiopao.model.IssueReport;
import com.toastedsiopao.model.User;
import com.toastedsiopao.service.CustomerService;
import com.toastedsiopao.service.IssueReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; 
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/issues")
public class IssueReportController {

    private static final Logger log = LoggerFactory.getLogger(IssueReportController.class);

    @Autowired
    private IssueReportService issueReportService;

    @Autowired
    private CustomerService customerService;

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAuthority('VIEW_ISSUE_REPORTS')")
    public ResponseEntity<List<IssueReportResponseDto>> getIssuesForOrder(@PathVariable Long orderId, Principal principal) { 
        log.info("Admin {} fetching issue reports for Order ID: {}", principal.getName(), orderId);
        try {
            List<IssueReportResponseDto> reports = issueReportService.findReportsByOrderId(orderId); 
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            log.error("Could not fetch issue reports for Order ID {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/resolve/{issueId}")
    @PreAuthorize("hasAuthority('RESOLVE_ISSUE_REPORTS')")
    public ResponseEntity<?> resolveIssue(@PathVariable Long issueId,
                                          @RequestBody Map<String, String> payload,
                                          Principal principal) {
        
        User admin = customerService.findByUsername(principal.getName());
        if (admin == null) {
            return ResponseEntity.status(401).build(); 
        }

        String adminNotes = payload.get("adminNotes");
        log.info("Admin {} attempting to resolve issue report #{} with notes: {}", admin.getUsername(), issueId, adminNotes);

        try {
            IssueReportResponseDto resolvedReport = issueReportService.resolveIssueReport(issueId, admin, adminNotes); 
            return ResponseEntity.ok(resolvedReport); 
        } catch (IllegalArgumentException e) {
            log.warn("Failed to resolve issue #{}: {}", issueId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error resolving issue #{}: {}", issueId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred."));
        }
    }
    
    @GetMapping("/my-report/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyIssueReportForOrder(@PathVariable Long orderId, Principal principal) {
        
        User user = customerService.findByUsername(principal.getName());
        if (user == null) {
            return ResponseEntity.status(401).build(); 
        }

        log.info("Customer {} fetching their issue report for Order ID: {}", user.getUsername(), orderId);
        
        try {
            IssueReportResponseDto reportDto = issueReportService.getCustomerReportForOrder(user, orderId);
            return ResponseEntity.ok(reportDto);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to fetch issue report for order #{} by {}: {}", orderId, user.getUsername(), e.getMessage());
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
			log.warn("SECURITY: User {} tried to access report for order #{} which is not theirs.", user.getUsername(), orderId);
			return ResponseEntity.status(403).body(Map.of("error", "Access Denied."));
		} catch (Exception e) {
            log.error("Error fetching issue report for order #{}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An unexpected error occurred."));
        }
    }
}