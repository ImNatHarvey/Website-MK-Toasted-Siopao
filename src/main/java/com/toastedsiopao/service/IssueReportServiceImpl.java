package com.toastedsiopao.service;

import com.toastedsiopao.dto.IssueReportDto;
import com.toastedsiopao.dto.IssueReportResponseDto; // --- ADDED ---
import com.toastedsiopao.model.IssueReport;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.User;
import com.toastedsiopao.repository.IssueReportRepository;
import com.toastedsiopao.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; 
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock; 
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // --- ADDED ---
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class IssueReportServiceImpl implements IssueReportService {

    private static final Logger log = LoggerFactory.getLogger(IssueReportServiceImpl.class);
    
    // --- ADDED ---
    private static final DateTimeFormatter DTO_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

    @Autowired
    private IssueReportRepository issueReportRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private Clock clock;

    @Override
    public IssueReport createIssueReport(User user, IssueReportDto reportDto, MultipartFile attachmentFile) {
        Order order = orderRepository.findById(reportDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));

        // Security check: Ensure the order belongs to the user
        if (!order.getUser().getId().equals(user.getId())) {
            log.warn("SECURITY: User {} attempted to report issue for order {} which does not belong to them.",
                    user.getUsername(), order.getId());
            throw new IllegalArgumentException("You can only report issues for your own orders.");
        }
        
        // --- START: MODIFICATION ---
        // NEW CHECK 1: Order Status
        if (!Order.STATUS_DELIVERED.equals(order.getStatus())) {
            log.warn("User {} attempted to report issue for order #{} which is not DELIVERED. Status: {}",
                    user.getUsername(), order.getId(), order.getStatus());
            throw new IllegalArgumentException("You can only report issues for delivered orders.");
        }

        // NEW CHECK 2: One report per order
        if (issueReportRepository.existsByOrder(order)) {
            log.warn("User {} attempted to file a second issue report for order #{}. Blocked.",
                    user.getUsername(), order.getId());
            throw new IllegalArgumentException("An issue report has already been submitted for this order.");
        }
        // --- END: MODIFICATION ---

        String attachmentImagePath = null;
        if (attachmentFile != null && !attachmentFile.isEmpty()) {
            try {
                // You might want to add file type validation here from another service
                attachmentImagePath = fileStorageService.store(attachmentFile);
                log.info("Stored issue attachment for user {} at: {}", user.getUsername(), attachmentImagePath);
            } catch (Exception e) {
                log.error("Failed to store issue attachment for user {}: {}", user.getUsername(), e.getMessage(), e);
                throw new IllegalArgumentException("Could not save attachment image. Please try again.");
            }
        }

        IssueReport newReport = new IssueReport(
                order,
                user,
                reportDto.getSummary(),
                reportDto.getDetails(),
                attachmentImagePath
        );
        
        // --- FIX: Manually update the collection on the parent side ---
        order.getIssueReports().add(newReport);
        // --- END FIX ---

        IssueReport savedReport = issueReportRepository.save(newReport);
        log.info("User {} filed new issue report #{} for Order #{}", user.getUsername(), savedReport.getId(), order.getId());

        // Notify admins
        String notifMessage = "New Issue Reported by " + user.getUsername() + " for Order #" + order.getId();
        String notifLink = "/admin/orders?keyword=" + order.getId(); // Link to admin orders page, filtered by order ID
        notificationService.createAdminNotification(notifMessage, notifLink);

        return savedReport;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IssueReportResponseDto> findReportsByOrderId(Long orderId) { // --- MODIFIED ---
        List<IssueReport> reports = issueReportRepository.findByOrderId(orderId);
        // --- ADDED ---
        return reports.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        // --- END ADDED ---
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Long> getOpenIssueCountsForOrders(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        return issueReportRepository.findOpenIssueCountsByOrderIds(orderIds);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean doesReportExistForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found."));
        return issueReportRepository.existsByOrder(order);
    }
    
    @Override
    public IssueReportResponseDto resolveIssueReport(Long issueId, User admin, String adminNotes) { // --- MODIFIED ---
        IssueReport report = issueReportRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue report #" + issueId + " not found."));

        if (!report.isOpen()) {
            throw new IllegalArgumentException("Issue report #" + issueId + " is already resolved.");
        }

        report.setOpen(false);
        report.setResolvedAt(LocalDateTime.now(clock));
        report.setResolvedByAdmin(admin.getUsername());
        if (StringUtils.hasText(adminNotes)) {
            report.setAdminNotes(adminNotes);
        }

        IssueReport savedReport = issueReportRepository.save(report);
        log.info("Admin {} resolved issue report #{}", admin.getUsername(), issueId);

        // Notify the customer
        String notifMessage = "Your issue report for Order #ORD-" + report.getOrder().getId() + " has been marked as resolved.";
        String notifLink = "/u/history"; // Link to their order history
        notificationService.createUserNotification(report.getUser(), notifMessage, notifLink);

        return convertToDto(savedReport); // --- MODIFIED ---
    }
    
    // --- START: NEW METHOD ---
    @Override
    @Transactional(readOnly = true)
	public IssueReportResponseDto getCustomerReportForOrder(User user, Long orderId) {
		List<IssueReport> reports = issueReportRepository.findByOrderId(orderId);
		if (reports.isEmpty()) {
			throw new IllegalArgumentException("No issue report found for this order.");
		}
		
		IssueReport report = reports.get(0); // We assume one report per order
		
		// Security Check
		if (!report.getUser().getId().equals(user.getId())) {
			throw new AccessDeniedException("You do not have permission to view this report.");
		}
		
		return convertToDto(report);
	}
    // --- END: NEW METHOD ---
    
    /**
     * Converts an IssueReport entity to a safe DTO for JSON serialization.
     * This prevents lazy-loading exceptions.
     */
    private IssueReportResponseDto convertToDto(IssueReport report) {
        IssueReportResponseDto dto = new IssueReportResponseDto();
        dto.setId(report.getId());
        dto.setSummary(report.getSummary());
        dto.setDetails(report.getDetails());
        dto.setAttachmentImageUrl(report.getAttachmentImageUrl());
        dto.setOpen(report.isOpen());
        dto.setReportedAt(report.getReportedAt().format(DTO_FORMATTER));
        
        // Safely access lazy-loaded user
        if (report.getUser() != null) {
            dto.setUsername(report.getUser().getUsername());
        } else {
            // This should not happen, but good to check
            dto.setUsername("Unknown");
        }

        if (report.getResolvedAt() != null) {
            dto.setResolvedAt(report.getResolvedAt().format(DTO_FORMATTER));
        }
        dto.setResolvedByAdmin(report.getResolvedByAdmin());
        dto.setAdminNotes(report.getAdminNotes());

        return dto;
    }
}