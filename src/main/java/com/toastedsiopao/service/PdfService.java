package com.toastedsiopao.service;

import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.Product; 

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * Generates a PDF inventory report from a list of items.
     *
     * @param items      The list of inventory items to include.
     * @param keyword    The search keyword used (for the title).
     * @param categoryId The category ID used (for the title).
     * @return A ByteArrayInputStream containing the .pdf file data.
     * @throws IOException if the PDF generation fails.
     */
    ByteArrayInputStream generateInventoryReportPdf(List<InventoryItem> items, String keyword, Long categoryId) throws IOException;

    /**
     * Generates a PDF product report from a list of products.
     *
     * @param products   The list of products to include.
     * @param keyword    The search keyword used (for the title).
     * @param categoryId The category ID used (for the title).
     * @return A ByteArrayInputStream containing the .pdf file data.
     * @throws IOException if the PDF generation fails.
     */
    ByteArrayInputStream generateProductReportPdf(List<Product> products, String keyword, Long categoryId) throws IOException;

    /**
     * Generates a PDF invoice for a single order.
     *
     * @param order The fully populated Order object (with items, products, and user).
     * @return A ByteArrayInputStream containing the .pdf file data.
     * @throws IOException if the PDF generation fails.
     */
    ByteArrayInputStream generateInvoicePdf(Order order) throws IOException;

}