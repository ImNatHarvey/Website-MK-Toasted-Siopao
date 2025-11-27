package com.toastedsiopao.controller;

import com.toastedsiopao.model.Order; 
import com.toastedsiopao.service.OrderService;
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
import java.util.Optional; 

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    private static final Logger log = LoggerFactory.getLogger(AdminReportController.class);

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private OrderService orderService;

    @GetMapping("/financial")
    @PreAuthorize("hasAuthority('VIEW_TRANSACTIONS')")
    public ResponseEntity<InputStreamResource> downloadFinancialReport(
            @RequestParam(value = "keyword", required = false) String keyword, 
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        log.info("Generating financial report for keyword: [{}], start: [{}], end: [{}]", keyword, startDate, endDate); 

        try {
            ByteArrayInputStream bis = reportService.generateFinancialReport(keyword, startDate, endDate); 

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "MK-Toasted-Siopao_Financial-Report_" + timestamp + ".xlsx";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate financial report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating financial report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/financial/pdf")
    @PreAuthorize("hasAuthority('VIEW_TRANSACTIONS')")
    public ResponseEntity<InputStreamResource> downloadFinancialReportPdf(
            @RequestParam(value = "keyword", required = false) String keyword, 
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        log.info("Generating financial PDF report for keyword: [{}], start: [{}], end: [{}]", keyword, startDate, endDate); 

        try {
            ByteArrayInputStream bis = reportService.generateFinancialReportPdf(keyword, startDate, endDate); 

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "MK-Toasted-Siopao_Financial-Report_" + timestamp + ".pdf";

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
            String fileName = "MK-Toasted-Siopao_Inventory-Report_" + timestamp + ".xlsx";

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
            String fileName = "MK-Toasted-Siopao_Inventory-Report_" + timestamp + ".pdf";

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
            String fileName = "MK-Toasted-Siopao_Product-Report_" + timestamp + ".xlsx";

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
            String fileName = "MK-Toasted-Siopao_Product-Report_" + timestamp + ".pdf";

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

    // --- ADDED: Waste Report Endpoints ---
    @GetMapping("/waste")
    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    public ResponseEntity<InputStreamResource> downloadWasteReport(
            @RequestParam(value = "wasteKeyword", required = false) String wasteKeyword) {

        log.info("Generating waste EXCEL report for keyword: [{}]", wasteKeyword);

        try {
            ByteArrayInputStream bis = reportService.generateWasteReport(wasteKeyword);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "MK-Toasted-Siopao_Waste-Spoilage-Report_" + timestamp + ".xlsx";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate waste Excel report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating waste Excel report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/waste/pdf")
    @PreAuthorize("hasAuthority('VIEW_INVENTORY')")
    public ResponseEntity<InputStreamResource> downloadWasteReportPdf(
            @RequestParam(value = "wasteKeyword", required = false) String wasteKeyword) {

        log.info("Generating waste PDF report for keyword: [{}]", wasteKeyword);

        try {
            ByteArrayInputStream bis = reportService.generateWasteReportPdf(wasteKeyword);

            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String fileName = "MK-Toasted-Siopao_Waste-Spoilage-Report_" + timestamp + ".pdf";

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(bis));

        } catch (IOException e) {
            log.error("Failed to generate waste PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating waste PDF report: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    // --- END ADDED ---

    // --- MODIFIED: Added documentType parameter ---
    @GetMapping("/download/{documentType}/{id}")
    @PreAuthorize("hasAuthority('VIEW_ORDERS')")
    public ResponseEntity<InputStreamResource> downloadOrderDocumentPdf(
    		@PathVariable("documentType") String documentType,
            @PathVariable("id") Long orderId) {

        log.info("Generating {} PDF for Order ID: {}", documentType, orderId);

        try {
            Order order = orderService.findOrderForInvoice(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));
			
            ByteArrayInputStream bis = reportService.generateOrderDocumentPdf(order, documentType);

            HttpHeaders headers = new HttpHeaders();
            String fileName = String.format("MK-Toasted-Siopao_%s_ORD-%d.pdf", documentType.toUpperCase(), orderId);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new InputStreamResource(bis));

        } catch (IllegalArgumentException e) {
            log.warn("Failed to generate {} PDF for order {}: {}", documentType, orderId, e.getMessage());
            return ResponseEntity.notFound().build(); 
        } catch (IOException e) {
            log.error("Failed to generate {} PDF for order {}: {}", documentType, orderId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Unexpected error generating {} PDF for order {}: {}", documentType, orderId, e.getMessage(), e);
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
            String fileName = "MK-Toasted-Siopao_Activity-Log_Page-" + (page + 1) + "_" + timestamp + ".pdf";

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