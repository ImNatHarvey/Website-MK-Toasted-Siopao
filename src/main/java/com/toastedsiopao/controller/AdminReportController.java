package com.toastedsiopao.controller;

import com.toastedsiopao.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest; 
import org.springframework.data.domain.Pageable; 
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    private static final Logger log = LoggerFactory.getLogger(AdminReportController.class);

    @Autowired
    private ReportService reportService;

    @GetMapping("/financial")
    @PreAuthorize("hasAuthority('VIEW_TRANSACTIONS')")
    public ResponseEntity<InputStreamResource> downloadFinancialReport(
            @RequestParam(value = "keyword", required = false) String keyword, // --- ADDED ---
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        log.info("Generating financial report for keyword: [{}], start: [{}], end: [{}]", keyword, startDate, endDate); // --- MODIFIED ---

        try {
            ByteArrayInputStream bis = reportService.generateFinancialReport(keyword, startDate, endDate); // --- MODIFIED ---

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "Financial_Report_" + timestamp + ".xlsx";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate financial report: {}", e.getMessage(), e);
            // In a real app, you might redirect to an error page
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating financial report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/financial/pdf")
    @PreAuthorize("hasAuthority('VIEW_TRANSACTIONS')")
    public ResponseEntity<InputStreamResource> downloadFinancialReportPdf(
            @RequestParam(value = "keyword", required = false) String keyword, // --- ADDED ---
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        log.info("Generating financial PDF report for keyword: [{}], start: [{}], end: [{}]", keyword, startDate, endDate); // --- MODIFIED ---

        try {
            ByteArrayInputStream bis = reportService.generateFinancialReportPdf(keyword, startDate, endDate); // --- MODIFIED ---

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "Financial_Report_" + timestamp + ".pdf";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName); // Download directly

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate financial PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating financial PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    public ResponseEntity<InputStreamResource> downloadInventoryReport(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) Long categoryId) {

        log.info("Generating inventory EXCEL report for keyword: [{}], categoryId: [{}]", keyword, categoryId);

        try {
            ByteArrayInputStream bis = reportService.generateInventoryReport(keyword, categoryId);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "Inventory_Report_" + timestamp + ".xlsx";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate inventory Excel report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating inventory Excel report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/inventory/pdf")
    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    public ResponseEntity<InputStreamResource> downloadInventoryReportPdf(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) Long categoryId) {

        log.info("Generating inventory PDF report for keyword: [{}], categoryId: [{}]", keyword, categoryId);

        try {
            ByteArrayInputStream bis = reportService.generateInventoryReportPdf(keyword, categoryId);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "Inventory_Report_" + timestamp + ".pdf";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate inventory PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating inventory PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('VIEW_PRODUCTS')")
    public ResponseEntity<InputStreamResource> downloadProductReport(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) Long categoryId) {

        log.info("Generating product EXCEL report for keyword: [{}], categoryId: [{}]", keyword, categoryId);

        try {
            ByteArrayInputStream bis = reportService.generateProductReport(keyword, categoryId);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "Product_Report_" + timestamp + ".xlsx";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate product Excel report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating product Excel report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/products/pdf")
    @PreAuthorize("hasAuthority('VIEW_PRODUCTS')")
    public ResponseEntity<InputStreamResource> downloadProductReportPdf(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) Long categoryId) {

        log.info("Generating product PDF report for keyword: [{}], categoryId: [{}]", keyword, categoryId);

        try {
            ByteArrayInputStream bis = reportService.generateProductReportPdf(keyword, categoryId);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "Product_Report_" + timestamp + ".pdf";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate product PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating product PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/invoice/{id}")
    @PreAuthorize("hasAuthority('VIEW_ORDERS')")
    public ResponseEntity<InputStreamResource> downloadInvoicePdf(@PathVariable("id") Long orderId) {

        log.info("Generating invoice PDF for Order ID: {}", orderId);

        try {
            ByteArrayInputStream bis = reportService.generateInvoicePdf(orderId);

            HttpHeaders headers = new HttpHeaders();
            String fileName = "Invoice_ORD-" + orderId + ".pdf";
            
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

    @GetMapping("/activity-log/pdf")
    @PreAuthorize("hasAuthority('VIEW_ACTIVITY_LOG')")
    public ResponseEntity<InputStreamResource> downloadActivityLogPdf(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        log.info("Generating activity log PDF report for page: [{}], size: [{}]", page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            ByteArrayInputStream bis = reportService.generateActivityLogPdf(pageable);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "Activity_Log_Page-" + (page + 1) + "_" + timestamp + ".pdf";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate activity log PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating activity log PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}