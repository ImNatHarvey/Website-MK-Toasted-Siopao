package com.toastedsiopao.service;

import com.toastedsiopao.model.ActivityLogEntry;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.Product; 
import org.springframework.data.domain.Page; 

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public interface PdfService {

    ByteArrayInputStream generateFinancialReportPdf(List<Order> orders, LocalDateTime start, LocalDateTime end) throws IOException;

    ByteArrayInputStream generateInventoryReportPdf(List<InventoryItem> items, String keyword, Long categoryId) throws IOException;

    ByteArrayInputStream generateProductReportPdf(List<Product> products, String keyword, Long categoryId) throws IOException;

    ByteArrayInputStream generateOrderDocumentPdf(Order order, String documentType) throws IOException;

    ByteArrayInputStream generateActivityLogPdf(Page<ActivityLogEntry> logPage) throws IOException;
    
    // --- MODIFIED: Added Date Range parameters ---
    ByteArrayInputStream generateWasteLogPdf(Page<ActivityLogEntry> logPage, String keyword, String reasonCategory, String wasteType, String startDate, String endDate) throws IOException;
    // --- END MODIFIED ---
}