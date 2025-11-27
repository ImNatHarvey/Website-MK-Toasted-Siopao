package com.toastedsiopao.service;

import org.springframework.data.domain.Pageable; 

import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.toastedsiopao.model.Order;

public interface ReportService {

    ByteArrayInputStream generateFinancialReport(String keyword, String startDate, String endDate) throws IOException;

    ByteArrayInputStream generateFinancialReportPdf(String keyword, String startDate, String endDate) throws IOException;

    ByteArrayInputStream generateInventoryReport(String keyword, Long categoryId) throws IOException;

    ByteArrayInputStream generateInventoryReportPdf(String keyword, Long categoryId) throws IOException;

    ByteArrayInputStream generateProductReport(String keyword, Long categoryId) throws IOException;

    ByteArrayInputStream generateProductReportPdf(String keyword, Long categoryId) throws IOException;

    // --- MODIFIED: Added wasteType parameter ---
    ByteArrayInputStream generateWasteReport(String keyword, String reasonCategory, String wasteType) throws IOException;
    
    ByteArrayInputStream generateWasteReportPdf(String keyword, String reasonCategory, String wasteType) throws IOException;
    // --- END MODIFIED ---

    ByteArrayInputStream generateOrderDocumentPdf(Order order, String documentType) throws IOException, IllegalArgumentException;

    ByteArrayInputStream generateActivityLogPdf(Pageable pageable) throws IOException;
}