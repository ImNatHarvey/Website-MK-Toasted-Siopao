package com.toastedsiopao.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.toastedsiopao.model.ActivityLogEntry; 
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.OrderItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.SiteSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page; 
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
    private OrderService orderService; 

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryCategoryService inventoryCategoryService;

    @Autowired
    private FileStorageService fileStorageService; 

    // --- Define Fonts ---
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
    private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font FONT_TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
    private static final Font FONT_BOLD_CELL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
    private static final Font FONT_TOTAL_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font FONT_TOTAL_CELL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
    private static final Font FONT_INVOICE_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLACK);
    private static final Font FONT_INVOICE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_INVOICE_BODY = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
    private static final Font FONT_INVOICE_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);


    private static final Color COLOR_TABLE_HEADER_BG = new Color(17, 63, 103); 
    private static final Color COLOR_TOTAL_ROW_BG = new Color(230, 230, 230);

    @Override
    public ByteArrayInputStream generateFinancialReportPdf(List<Order> orders, LocalDateTime start, LocalDateTime end) throws IOException {
        SiteSettings settings = siteSettingsService.getSiteSettings();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Document document = new Document(PageSize.A4.rotate())) { 
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph(settings.getWebsiteName() + " - Financial Report", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

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

            log.debug("Calculating summary totals for PDF...");
            BigDecimal totalSales = BigDecimal.ZERO;
            BigDecimal totalCogs = BigDecimal.ZERO;

            for (Order order : orders) {
                totalSales = totalSales.add(order.getTotalAmount());
                totalCogs = totalCogs.add(orderService.calculateCogsForOrder(order));
            }
            BigDecimal grossProfit = totalSales.subtract(totalCogs);

            PdfPTable summaryTable = new PdfPTable(4); 
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

            log.debug("Building detailed breakdown table for PDF...");
            PdfPTable detailTable = new PdfPTable(7); 
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[] { 1f, 1.5f, 2f, 3f, 1.2f, 1.2f, 1.2f });

            addTableHeader(detailTable, "Order ID");
            addTableHeader(detailTable, "Date");
            addTableHeader(detailTable, "Customer");
            addTableHeader(detailTable, "Items");
            addTableHeader(detailTable, "Total Sales");
            addTableHeader(detailTable, "Est. COGS");
            addTableHeader(detailTable, "Est. Profit");

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

        try (Document document = new Document(PageSize.A4)) { 
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph(settings.getWebsiteName() + " - Inventory Stock Report", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

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

            BigDecimal grandTotalValue = items.stream()
                    .map(InventoryItem::getTotalCostValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            PdfPTable summaryTable = new PdfPTable(4); 
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[] { 2f, 1f, 2.5f, 2f });
            summaryTable.setSpacingAfter(20f);

            addSummaryCell(summaryTable, "Total Item Types:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
            addSummaryCell(summaryTable, String.valueOf(items.size()), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
            addSummaryCell(summaryTable, "Total Inventory Value:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
            addSummaryCell(summaryTable, formatCurrency(grandTotalValue), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
            
            document.add(summaryTable);
            
            PdfPTable detailTable = new PdfPTable(8); 
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[] { 0.8f, 2.5f, 1.8f, 1f, 0.8f, 1.2f, 1.5f, 1f });

            addTableHeader(detailTable, "ID");
            addTableHeader(detailTable, "Item Name");
            addTableHeader(detailTable, "Category");
            addTableHeader(detailTable, "Stock");
            addTableHeader(detailTable, "Unit");
            addTableHeader(detailTable, "Cost/Unit");
            addTableHeader(detailTable, "Total Value");
            addTableHeader(detailTable, "Status");
            
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
            
            addTableFooterCell(detailTable, "Total Inventory Value:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT, 6);
            addTableFooterCell(detailTable, formatCurrency(grandTotalValue), FONT_TOTAL_CELL, Element.ALIGN_RIGHT, 2);

            document.add(detailTable);

        } catch (DocumentException e) {
            log.error("DocumentException during PDF generation: {}", e.getMessage(), e);
            throw new IOException("Error creating PDF document", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public ByteArrayInputStream generateProductReportPdf(List<Product> products, String keyword, Long categoryId) throws IOException {
        SiteSettings settings = siteSettingsService.getSiteSettings();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph(settings.getWebsiteName() + " - Product & Recipe Report", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            String filterDesc = "Filters: ";
            if (StringUtils.hasText(keyword)) {
                filterDesc += "Keyword='" + keyword + "' ";
            }
            if (categoryId != null) {
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

            PdfPTable detailTable = new PdfPTable(8); 
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[] { 0.8f, 2f, 1.5f, 1f, 1f, 1f, 1.2f, 3.5f });

            addTableHeader(detailTable, "ID");
            addTableHeader(detailTable, "Product Name");
            addTableHeader(detailTable, "Category");
            addTableHeader(detailTable, "Price");
            addTableHeader(detailTable, "Stock");
            addTableHeader(detailTable, "Prod. Status");
            addTableHeader(detailTable, "Stock Status");
            addTableHeader(detailTable, "Recipe Ingredients");
            
            for (Product product : products) {
                String recipe = product.getIngredients().stream()
                        .map(ing -> ing.getQuantityNeeded() + " " +
                                (ing.getInventoryItem().getUnit() != null ? ing.getInventoryItem().getUnit().getAbbreviation() : "units") +
                                " of " + ing.getInventoryItem().getName())
                        .collect(Collectors.joining("\n")); 

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

    @Override
    public ByteArrayInputStream generateInvoicePdf(Order order) throws IOException {
        SiteSettings settings = siteSettingsService.getSiteSettings();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Document document = new Document(PageSize.A4)) { 
            PdfWriter.getInstance(document, out);
            document.open();
            
            Paragraph title = new Paragraph("INVOICE / ORDER RECEIPT", FONT_INVOICE_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph storeName = new Paragraph(settings.getWebsiteName(), FONT_INVOICE_BODY);
            storeName.setAlignment(Element.ALIGN_CENTER);
            storeName.setSpacingAfter(20f);
            document.add(storeName);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[] { 1f, 1f });

            PdfPCell orderCell = new PdfPCell();
            orderCell.setBorder(Rectangle.NO_BORDER);
            orderCell.setPadding(5);
            orderCell.addElement(new Phrase("ORDER ID:", FONT_INVOICE_HEADER));
            orderCell.addElement(new Phrase("ORD-" + order.getId(), FONT_INVOICE_BODY));
            
            orderCell.addElement(Chunk.NEWLINE);
            orderCell.addElement(new Phrase("ORDER DATE:", FONT_INVOICE_HEADER));
            orderCell.addElement(new Phrase(order.getOrderDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy, h:mm a")), FONT_INVOICE_BODY));
            
            orderCell.addElement(Chunk.NEWLINE);
            orderCell.addElement(new Phrase("PAYMENT METHOD:", FONT_INVOICE_HEADER));
            orderCell.addElement(new Phrase(order.getPaymentMethod().toUpperCase(), FONT_INVOICE_BODY));
            
            orderCell.addElement(Chunk.NEWLINE);
            orderCell.addElement(new Phrase("PAYMENT STATUS:", FONT_INVOICE_HEADER));
            orderCell.addElement(new Phrase(order.getPaymentStatus().replace("_", " "), FONT_INVOICE_BODY));

            if ("gcash".equalsIgnoreCase(order.getPaymentMethod())) {
                if (StringUtils.hasText(order.getTransactionId())) {
                    orderCell.addElement(Chunk.NEWLINE);
                    orderCell.addElement(new Phrase("GCASH TRANSACTION ID:", FONT_INVOICE_HEADER));
                    orderCell.addElement(new Phrase(order.getTransactionId(), FONT_INVOICE_BODY));
                }
                if (StringUtils.hasText(order.getPaymentReceiptImageUrl())) {
                    orderCell.addElement(Chunk.NEWLINE);
                    orderCell.addElement(new Phrase("GCASH RECEIPT QR:", FONT_INVOICE_HEADER));
                    try {
                        Resource qrCodeResource = fileStorageService.loadAsResource(order.getPaymentReceiptImageUrl());
                        if (qrCodeResource != null && qrCodeResource.exists()) {
                            Image qrImage = Image.getInstance(qrCodeResource.getURL());
                            qrImage.scaleToFit(100, 100); 
                            orderCell.addElement(qrImage);
                        } else {
                            log.warn("Could not load QR code image for invoice: {}", order.getPaymentReceiptImageUrl());
                            orderCell.addElement(new Phrase("[Image not found]", FONT_INVOICE_BODY));
                        }
                    } catch (Exception e) {
                        log.error("Error loading QR code image for PDF: {}", e.getMessage());
                        orderCell.addElement(new Phrase("[Error loading image]", FONT_INVOICE_BODY));
                    }
                }
            }
            
            PdfPCell customerCell = new PdfPCell();
            customerCell.setBorder(Rectangle.NO_BORDER);
            customerCell.setPadding(5);
            customerCell.addElement(new Phrase("BILL TO:", FONT_INVOICE_HEADER));
            customerCell.addElement(new Phrase(order.getShippingFirstName() + " " + order.getShippingLastName(), FONT_INVOICE_BODY));
            customerCell.addElement(new Phrase(order.getShippingPhone(), FONT_INVOICE_BODY));
            customerCell.addElement(new Phrase(order.getShippingEmail(), FONT_INVOICE_BODY));
            
            customerCell.addElement(Chunk.NEWLINE);
            customerCell.addElement(new Phrase("SHIPPING ADDRESS:", FONT_INVOICE_HEADER));
            customerCell.addElement(new Phrase(order.getShippingAddress(), FONT_INVOICE_BODY));
            
            infoTable.addCell(orderCell);
            infoTable.addCell(customerCell);
            document.add(infoTable);

            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[] { 4f, 1f, 1.5f, 1.5f });
            itemsTable.setSpacingBefore(20f);
            
            addTableHeader(itemsTable, "Item Description");
            addTableHeader(itemsTable, "Qty");
            addTableHeader(itemsTable, "Unit Price");
            addTableHeader(itemsTable, "Total");
            
            for (OrderItem item : order.getItems()) {
                addTableCell(itemsTable, item.getProduct().getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(itemsTable, item.getQuantity().toString(), FONT_TABLE_CELL, Element.ALIGN_CENTER);
                addTableCell(itemsTable, formatCurrency(item.getPricePerUnit()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
                addTableCell(itemsTable, formatCurrency(item.getTotalPrice()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
            }
            
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL AMOUNT", FONT_INVOICE_TOTAL));
            totalLabelCell.setColspan(3);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalLabelCell.setPadding(8);
            totalLabelCell.setBorder(Rectangle.NO_BORDER);
            itemsTable.addCell(totalLabelCell);
            
            PdfPCell totalValueCell = new PdfPCell(new Phrase(formatCurrency(order.getTotalAmount()), FONT_INVOICE_TOTAL));
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalValueCell.setPadding(8);
            totalValueCell.setBackgroundColor(COLOR_TOTAL_ROW_BG);
            itemsTable.addCell(totalValueCell);
            
            document.add(itemsTable);
            
            if (StringUtils.hasText(order.getNotes())) {
                Paragraph notesHeader = new Paragraph("NOTES:", FONT_INVOICE_HEADER);
                notesHeader.setSpacingBefore(15f);
                document.add(notesHeader);
                Paragraph notesBody = new Paragraph(order.getNotes(), FONT_INVOICE_BODY);
                document.add(notesBody);
            }


        } catch (DocumentException e) {
            log.error("DocumentException during PDF generation: {}", e.getMessage(), e);
            throw new IOException("Error creating PDF document", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    @Override
    public ByteArrayInputStream generateActivityLogPdf(Page<ActivityLogEntry> logPage) throws IOException {
        SiteSettings settings = siteSettingsService.getSiteSettings();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (Document document = new Document(PageSize.A4.rotate())) { 
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph title = new Paragraph(settings.getWebsiteName() + " - Admin Activity Log", FONT_TITLE);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            String pageInfo = String.format("Page %d of %d (Entries %d-%d of %d)",
                    logPage.getNumber() + 1,
                    logPage.getTotalPages(),
                    logPage.getPageable().getOffset() + 1,
                    logPage.getPageable().getOffset() + logPage.getNumberOfElements(),
                    logPage.getTotalElements());

            Paragraph subtitle = new Paragraph(pageInfo, FONT_SUBTITLE);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15f);
            document.add(subtitle);

            PdfPTable detailTable = new PdfPTable(4);
            detailTable.setWidthPercentage(100);
            detailTable.setWidths(new float[] { 1.5f, 1f, 1.5f, 4f }); 

            addTableHeader(detailTable, "Timestamp");
            addTableHeader(detailTable, "Admin User");
            addTableHeader(detailTable, "Action");
            addTableHeader(detailTable, "Details");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (ActivityLogEntry logEntry : logPage.getContent()) {
                addTableCell(detailTable, logEntry.getTimestamp().format(dtf), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, logEntry.getUsername(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, logEntry.getAction(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
                addTableCell(detailTable, StringUtils.hasText(logEntry.getDetails()) ? logEntry.getDetails() : "", FONT_TABLE_CELL, Element.ALIGN_LEFT);
            }

            document.add(detailTable);

        } catch (DocumentException e) {
            log.error("DocumentException during PDF generation: {}", e.getMessage(), e);
            throw new IOException("Error creating PDF document", e);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

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
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addTableFooterCell(PdfPTable table, String data, Font font, int alignment, int colSpan) {
        PdfPCell cell = new PdfPCell(new Phrase(data, font));
        cell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
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