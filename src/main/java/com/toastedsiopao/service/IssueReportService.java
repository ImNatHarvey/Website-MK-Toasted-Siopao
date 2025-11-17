package com.toastedsiopao.service;

import com.toastedsiopao.dto.IssueReportDto;
import com.toastedsiopao.dto.IssueReportResponseDto; // --- MODIFIED ---
import com.toastedsiopao.model.IssueReport;
import com.toastedsiopao.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface IssueReportService {

    /**
     * Creates and saves a new issue report from a customer.
     *
     * @param user The customer reporting the issue.
     * @param reportDto The DTO containing the report summary and details.
     * @param attachmentFile The optional image attachment.
     * @return The saved IssueReport entity.
     * @throws IllegalArgumentException if validation fails or the order doesn't belong to the user.
     */
    IssueReport createIssueReport(User user, IssueReportDto reportDto, MultipartFile attachmentFile);

    /**
     * Finds all issue reports for a specific order.
     *
     * @param orderId The ID of the order.
     * @return A list of IssueReportResponseDto objects, sorted by date.
     */
    List<IssueReportResponseDto> findReportsByOrderId(Long orderId); // --- MODIFIED ---

    /**
     * Gets a map of open issue counts for a list of order IDs.
     *
     * @param orderIds A list of order IDs to check.
     * @return A Map where the key is the Order ID (Long) and the value is the count (Long) of open issues.
     */
    Map<Long, Long> getOpenIssueCountsForOrders(List<Long> orderIds);
    
    /**
     * Marks an issue report as resolved.
     *
     * @param issueId The ID of the issue report to resolve.
     * @param admin The admin user performing the action.
     * @param adminNotes Optional notes from the admin.
     * @return The updated IssueReportResponseDto.
     * @throws IllegalArgumentException if the issue is already resolved or not found.
     */
    IssueReportResponseDto resolveIssueReport(Long issueId, User admin, String adminNotes); // --- MODIFIED ---

    /**
     * Checks if an issue report already exists for a given order.
     *
     * @param orderId The ID of the order.
     * @return true if a report exists, false otherwise.
     */
    boolean doesReportExistForOrder(Long orderId);
    
    // --- START: NEW METHOD ---
    /**
     * Gets a single issue report for an order, but only if it belongs to the specified user.
     *
     * @param user The customer requesting the report.
     * @param orderId The ID of the order the report is for.
     * @return The IssueReportResponseDto.
     * @throws IllegalArgumentException if no report is found.
     * @throws AccessDeniedException if the report does not belong to the user.
     */
    IssueReportResponseDto getCustomerReportForOrder(User user, Long orderId);
    // --- END: NEW METHOD ---
}