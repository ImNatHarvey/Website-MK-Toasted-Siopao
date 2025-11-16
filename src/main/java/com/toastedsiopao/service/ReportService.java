package com.toastedsiopao.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface ReportService {

    /**
     * Generates a financial report (Sales, COGS, Profit) in Excel format.
     *
     * @param startDate The start date for the report (yyyy-MM-dd format). Can be
     * null.
     * @param endDate   The end date for the report (yyyy-MM-dd format). Can be
     * null.
     * @return A ByteArrayInputStream containing the .xlsx file data.
     * @throws IOException if there's an error creating the Excel file.
     */
    ByteArrayInputStream generateFinancialReport(String startDate, String endDate) throws IOException;

    /**
     * Generates a financial report (Sales, COGS, Profit) in PDF format.
     *
     * @param startDate The start date for the report (yyyy-MM-dd format). Can be
     * null.
     * @param endDate   The end date for the report (yyyy-MM-dd format). Can be
     * null.
     * @return A ByteArrayInputStream containing the .pdf file data.
     * @throws IOException if there's an error creating the PDF.
     */
    ByteArrayInputStream generateFinancialReportPdf(String startDate, String endDate) throws IOException;

    /**
     * Generates an inventory report in Excel format.
     *
     * @param keyword    The search term for item name (can be null).
     * @param categoryId The category ID to filter by (can be null).
     * @return A ByteArrayInputStream containing the .xlsx file data.
     * @throws IOException if there's an error creating the Excel file.
     */
    ByteArrayInputStream generateInventoryReport(String keyword, Long categoryId) throws IOException;

    /**
     * Generates an inventory report in PDF format.
     *
     * @param keyword    The search term for item name (can be null).
     * @param categoryId The category ID to filter by (can be null).
     * @return A ByteArrayInputStream containing the .pdf file data.
     * @throws IOException if there's an error creating the PDF.
     */
    ByteArrayInputStream generateInventoryReportPdf(String keyword, Long categoryId) throws IOException;

    /**
     * Generates a product report (with recipes) in Excel format.
     *
     * @param keyword    The search term for product name (can be null).
     * @param categoryId The category ID to filter by (can be null).
     * @return A ByteArrayInputStream containing the .xlsx file data.
     * @throws IOException if there's an error creating the Excel file.
     */
    ByteArrayInputStream generateProductReport(String keyword, Long categoryId) throws IOException;

    /**
     * Generates a product report (with recipes) in PDF format.
     *
     * @param keyword    The search term for product name (can be null).
     * @param categoryId The category ID to filter by (can be null).
     * @return A ByteArrayInputStream containing the .pdf file data.
     * @throws IOException if there's an error creating the PDF.
     */
    ByteArrayInputStream generateProductReportPdf(String keyword, Long categoryId) throws IOException;

    /**
     * Generates a single invoice/receipt for a specific order.
     *
     * @param orderId The ID of the order to generate an invoice for.
     * @return A ByteArrayInputStream containing the .pdf file data.
     * @throws IOException              if there's an error creating the PDF.
     * @throws IllegalArgumentException if the order is not found.
     */
    ByteArrayInputStream generateInvoicePdf(Long orderId) throws IOException, IllegalArgumentException;
}