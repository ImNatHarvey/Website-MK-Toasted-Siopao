package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

// --- THIS IS THE FIX: Changed 'class' to 'interface' ---
public interface PdfService {

    /**
     * Generates a PDF financial report from a list of orders.
     *
     * @param orders The list of delivered orders to include in the report.
     * @param start  The start date of the report range (for the title).
     * @param end    The end date of the report range (for the title).
     * @return A ByteArrayInputStream containing the .pdf file data.
     * @throws IOException if the PDF generation fails.
     */
    ByteArrayInputStream generateFinancialReportPdf(List<Order> orders, LocalDateTime start, LocalDateTime end) throws IOException;

}