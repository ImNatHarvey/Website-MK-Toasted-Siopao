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

    ByteArrayInputStream generateWasteReport(String keyword, String reasonCategory, String wasteType, String startDate, String endDate) throws IOException;
    
    ByteArrayInputStream generateWasteReportPdf(String keyword, String reasonCategory, String wasteType, String startDate, String endDate) throws IOException;

    ByteArrayInputStream generateOrderDocumentPdf(Order order, String documentType) throws IOException, IllegalArgumentException;

    ByteArrayInputStream generateActivityLogPdf(String keyword, String startDate, String endDate, Pageable pageable) throws IOException;
    
    // --- NEW: Dashboard Report ---
    ByteArrayInputStream generateDashboardReportPdf() throws IOException;
}