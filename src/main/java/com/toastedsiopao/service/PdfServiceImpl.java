package com.toastedsiopao.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.SiteSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PdfServiceImpl implements PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfServiceImpl.class);

    @Autowired
    private SiteSettingsService siteSettingsService;

    @Autowired
    private OrderService orderService; // To calculate COGS for each order

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryCategoryService inventoryCategoryService;

    // --- Define Fonts ---
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font FONT_TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FONT_BOLD_CELL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
    private static final Font FONT_TOTAL_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font FONT_TOTAL_CELL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);

    private static final Color COLOR_TABLE_HEADER_BG = new Color(17, 63, 103); // --primary
    private static final Color COLOR_TOTAL_ROW_BG = new Color(230, 230, 230); // Light gray

    @Override
    public ByteArrayInputStream generateFinancialReportPdf(List<Order> orders, LocalDateTime start, LocalDateTime end) throws IOException {
        SiteSettings settings = siteSettingsService.getSiteSettings();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Document document = new Document(PageSize.A4.rotate())) { // Use landscape
            PdfWriter.getInstance(document, out);
            document.open();

            // === 1. Add Title ===
            Paragraph title = new Paragraph(settings.getWebsiteName() + " - Financial Report", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // === 2. Add Date Range ===
            String dateRange = "For all completed orders";
            if (start != null && end != null) {
                dateRange = "For orders from " + start.format(dtf) + " to " + end.format(dtf);
            } else if (start != null) {
                dateRange = "For orders since " + start.format(dtf);
            } else if (end != null) {
                dateRange = "For orders up to " + end.format(dtf);
            }
            Paragraph subtitle = new Paragraph(dateRange, FONT_SUBTITLE);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15f);
            document.add(subtitle);

            // === 3. Add Summary Table ===
            log.debug("Calculating summary totals for PDF...");
            BigDecimal totalSales = BigDecimal.ZERO;
            BigDecimal totalCogs = BigDecimal.ZERO;

            for (Order order : orders) {
                totalSales = totalSales.add(order.getTotalAmount());
                totalCogs = totalCogs.add(orderService.calculateCogsForOrder(order));
            }
            BigDecimal grossProfit = totalSales.subtract(totalCogs);

            PdfPTable summaryTable = new PdfPTable(4); // 4 columns
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[] { 2f, 1.5f, 2f, 1.5f });
            summaryTable.setSpacingAfter(20f);

            addSummaryCell(summaryTable, "Total Sales:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
            addSummaryCell(summaryTable, formatCurrency(totalSales), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
            addSummaryCell(summaryTable, "Total Orders Included:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
            addSummaryCell(summaryTable, String.valueOf(orders.size()), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
            
            addSummaryCell(summaryTable, "Total COGS:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
            addSummaryCell(summaryTable, formatCurrency(totalCogs), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
            addSummaryCell(summaryTable, "Gross Profit:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
            addSummaryCell(summaryTable, formatCurrency(grossProfit), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
            
            document.add(summaryTable);


            // === 4. Add Detailed Breakdown Table ===
            log.debug("Building detailed breakdown table for PDF...");
            PdfPTable detailTable = new PdfPTable(7); // 7 columns
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[] { 1f, 1.5f, 2f, 3f, 1.2f, 1.2f, 1.2f }); // Relative widths

            // --- Table Header ---
            addTableHeader(detailTable, "Order ID");
            addTableHeader(detailTable, "Date");
            addTableHeader(detailTable, "Customer");
            addTableHeader(detailTable, "Items");
            addTableHeader(detailTable, "Total Sales");
            addTableHeader(detailTable, "Est. COGS");
            addTableHeader(detailTable, "Est. Profit");

            // --- Table Body ---
            DateTimeFormatter orderDtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (Order order : orders) {
                BigDecimal orderCogs = orderService.calculateCogsForOrder(order);
                BigDecimal orderProfit = order.getTotalAmount().subtract(orderCogs);

                String items = order.getItems().stream()
                        .map(item -> item.getQuantity() + "x " + item.getProduct().getName())
                        .collect(Collectors.joining("\n")); // Use newline for PDF

                addTableCell(detailTable, "ORD-" + order.getId(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, order.getOrderDate().format(orderDtf), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, order.getShippingFirstName() + " " + order.getShippingLastName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, items, FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, formatCurrency(order.getTotalAmount()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
                addTableCell(detailTable, formatCurrency(orderCogs), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
                addTableCell(detailTable, formatCurrency(orderProfit), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
            }
            
            // --- Table Footer (Grand Totals) ---
            addTableFooterCell(detailTable, "Grand Totals:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT, 4);
            addTableFooterCell(detailTable, formatCurrency(totalSales), FONT_TOTAL_CELL, Element.ALIGN_RIGHT, 1);
            addTableFooterCell(detailTable, formatCurrency(totalCogs), FONT_TOTAL_CELL, Element.ALIGN_RIGHT, 1);
            addTableFooterCell(detailTable, formatCurrency(grossProfit), FONT_TOTAL_CELL, Element.ALIGN_RIGHT, 1);

            document.add(detailTable);

        } catch (DocumentException e) {
            log.error("DocumentException during PDF generation: {}", e.getMessage(), e);
            throw new IOException("Error creating PDF document", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    
    @Override
    public ByteArrayInputStream generateInventoryReportPdf(List<InventoryItem> items, String keyword, Long categoryId) throws IOException {
        SiteSettings settings = siteSettingsService.getSiteSettings();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Document document = new Document(PageSize.A4)) { // Portrait for this report
            PdfWriter.getInstance(document, out);
            document.open();

            // === 1. Add Title ===
            Paragraph title = new Paragraph(settings.getWebsiteName() + " - Inventory Stock Report", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // === 2. Add Filter Info ===
            String filterDesc = "Filters: ";
            if (StringUtils.hasText(keyword)) {
                filterDesc += "Keyword='" + keyword + "' ";
            }
            if (categoryId != null) {
                String catName = inventoryCategoryService.findById(categoryId).map(c -> c.getName()).orElse("ID " + categoryId);
                filterDesc += "Category='" + catName + "'";
            }
            if (!StringUtils.hasText(keyword) && categoryId == null) {
                filterDesc += "None (All Items)";
            }
            Paragraph subtitle = new Paragraph(filterDesc, FONT_SUBTITLE);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15f);
            document.add(subtitle);

            // === 3. Add Summary ===
            BigDecimal grandTotalValue = items.stream()
                    .map(InventoryItem::getTotalCostValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PdfPTable summaryTable = new PdfPTable(4); // 4 columns
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[] { 2f, 1f, 2.5f, 2f });
            summaryTable.setSpacingAfter(20f);

            addSummaryCell(summaryTable, "Total Item Types:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
            addSummaryCell(summaryTable, String.valueOf(items.size()), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
            addSummaryCell(summaryTable, "Total Inventory Value:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
            addSummaryCell(summaryTable, formatCurrency(grandTotalValue), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
            
            document.add(summaryTable);
            
            // === 4. Add Detailed Inventory Table ===
            PdfPTable detailTable = new PdfPTable(8); // 8 columns
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[] { 0.8f, 2.5f, 1.8f, 1f, 0.8f, 1.2f, 1.5f, 1f });

            // --- Table Header ---
            addTableHeader(detailTable, "ID");
            addTableHeader(detailTable, "Item Name");
            addTableHeader(detailTable, "Category");
            addTableHeader(detailTable, "Stock");
            addTableHeader(detailTable, "Unit");
            addTableHeader(detailTable, "Cost/Unit");
            addTableHeader(detailTable, "Total Value");
            addTableHeader(detailTable, "Status");
            
            // --- Table Body ---
            for (InventoryItem item : items) {
                addTableCell(detailTable, item.getId().toString(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, item.getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, item.getCategory().getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, item.getCurrentStock().toString(), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
                addTableCell(detailTable, item.getUnit().getAbbreviation(), FONT_TABLE_CELL, Element.ALIGN_CENTER);
                addTableCell(detailTable, formatCurrency(item.getCostPerUnit()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
                addTableCell(detailTable, formatCurrency(item.getTotalCostValue()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
                addTableCell(detailTable, item.getStockStatus(), FONT_BOLD_CELL, Element.ALIGN_CENTER);
            }
            
            // --- Table Footer (Grand Total) ---
            addTableFooterCell(detailTable, "Total Inventory Value:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT, 6);
            addTableFooterCell(detailTable, formatCurrency(grandTotalValue), FONT_TOTAL_CELL, Element.ALIGN_RIGHT, 2);

            document.add(detailTable);

        } catch (DocumentException e) {
            log.error("DocumentException during PDF generation: {}", e.getMessage(), e);
            throw new IOException("Error creating PDF document", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // === NEW METHOD IMPLEMENTATION FOR PRODUCT REPORT ===
    @Override
    public ByteArrayInputStream generateProductReportPdf(List<Product> products, String keyword, Long categoryId) throws IOException {
        SiteSettings settings = siteSettingsService.getSiteSettings();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Document document = new Document(PageSize.A4.rotate())) { // Landscape
            PdfWriter.getInstance(document, out);
            document.open();

            // === 1. Add Title ===
            Paragraph title = new Paragraph(settings.getWebsiteName() + " - Product & Recipe Report", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // === 2. Add Filter Info ===
            String filterDesc = "Filters: ";
            if (StringUtils.hasText(keyword)) {
                filterDesc += "Keyword='" + keyword + "' ";
            }
            if (categoryId != null) {
                // We can get the category name from the first product if it exists
                String catName = products.isEmpty() ? "ID " + categoryId : products.get(0).getCategory().getName();
                filterDesc += "Category='" + catName + "'";
            }
            if (!StringUtils.hasText(keyword) && categoryId == null) {
                filterDesc += "None (All Products)";
            }
            Paragraph subtitle = new Paragraph(filterDesc, FONT_SUBTITLE);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15f);
            document.add(subtitle);

            // === 3. Add Detailed Product Table ===
            PdfPTable detailTable = new PdfPTable(8); // 8 columns
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[] { 0.8f, 2f, 1.5f, 1f, 1f, 1f, 1.2f, 3.5f }); // Relative widths

            // --- Table Header ---
            addTableHeader(detailTable, "ID");
            addTableHeader(detailTable, "Product Name");
            addTableHeader(detailTable, "Category");
            addTableHeader(detailTable, "Price");
            addTableHeader(detailTable, "Stock");
            addTableHeader(detailTable, "Prod. Status");
            addTableHeader(detailTable, "Stock Status");
            addTableHeader(detailTable, "Recipe Ingredients");
            
            // --- Table Body ---
            for (Product product : products) {
                String recipe = product.getIngredients().stream()
                        .map(ing -> ing.getQuantityNeeded() + " " +
                                (ing.getInventoryItem().getUnit() != null ? ing.getInventoryItem().getUnit().getAbbreviation() : "units") +
                                " of " + ing.getInventoryItem().getName())
                        .collect(Collectors.joining("\n")); // Newline for each ingredient

                addTableCell(detailTable, product.getId().toString(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, product.getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, product.getCategory().getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, formatCurrency(product.getPrice()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
                addTableCell(detailTable, product.getCurrentStock().toString(), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
                addTableCell(detailTable, product.getProductStatus(), FONT_TABLE_CELL, Element.ALIGN_CENTER);
                addTableCell(detailTable, product.getStockStatus(), FONT_BOLD_CELL, Element.ALIGN_CENTER);
                addTableCell(detailTable, recipe.isEmpty() ? "N/A" : recipe, FONT_TABLE_CELL, Element.ALIGN_LEFT);
            }

            document.add(detailTable);

        } catch (DocumentException e) {
            log.error("DocumentException during PDF generation: {}", e.getMessage(), e);
            throw new IOException("Error creating PDF document", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }


    // === PDF Cell Helpers ===

    private void addTableHeader(PdfPTable table, String headerTitle) {
        PdfPCell cell = new PdfPCell(new Phrase(headerTitle, FONT_TABLE_HEADER));
        cell.setBackgroundColor(COLOR_TABLE_HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String data, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(data, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addTableFooterCell(PdfPTable table, String data, Font font, int alignment, int colSpan) {
        PdfPCell cell = new PdfPCell(new Phrase(data, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(COLOR_TOTAL_ROW_BG);
        cell.setColspan(colSpan);
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    private void addSummaryCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String formatCurrency(BigDecimal value) {
        return "P " + String.format("%,.2f", value);
    }
}