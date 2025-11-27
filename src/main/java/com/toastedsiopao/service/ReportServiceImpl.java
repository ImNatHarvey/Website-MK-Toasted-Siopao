package com.toastedsiopao.service;

import com.toastedsiopao.model.ActivityLogEntry; 
import com.toastedsiopao.model.InventoryItem;
import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.OrderItem;
import com.toastedsiopao.model.Product;
import com.toastedsiopao.model.RecipeIngredient;
import com.toastedsiopao.model.SiteSettings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page; 
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private SiteSettingsService siteSettingsService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryCategoryService inventoryCategoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ActivityLogService activityLogService; 

    private LocalDateTime parseDate(String date, boolean isEndDate) {
        if (!StringUtils.hasText(date)) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(date);
            return isEndDate ? localDate.atTime(LocalTime.MAX) : localDate.atStartOfDay();
        } catch (Exception e) {
            log.warn("Invalid date format for report: {}. Ignoring.", date);
            return null;
        }
    }

    @Override
    public ByteArrayInputStream generateFinancialReport(String keyword, String startDate, String endDate) throws IOException { 
        LocalDateTime startDateTime = parseDate(startDate, false);
        LocalDateTime endDateTime = parseDate(endDate, true);

        List<Order> orders = orderService.findDeliveredOrdersForReport(keyword, startDateTime, endDateTime); 
        SiteSettings settings = siteSettingsService.getSiteSettings();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);
            CellStyle totalRowStyle = createTotalRowStyle(workbook);
            CellStyle totalCurrencyStyle = createTotalCurrencyStyle(workbook, totalRowStyle);

            Sheet summarySheet = workbook.createSheet("Summary");
            createSummarySheet(summarySheet, orders, settings, headerStyle, boldStyle, currencyStyle, totalCurrencyStyle, startDateTime, endDateTime);

            Sheet detailSheet = workbook.createSheet("Detailed Breakdown");
            createDetailedBreakdownSheet(detailSheet, orders, headerStyle, currencyStyle, totalRowStyle, totalCurrencyStyle);

            autoSizeColumns(summarySheet, 3);
            autoSizeColumns(detailSheet, 7);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void createSummarySheet(Sheet sheet, List<Order> orders, SiteSettings settings,
            CellStyle headerStyle, CellStyle boldStyle, CellStyle currencyStyle,
            CellStyle totalCurrencyStyle,
            LocalDateTime start, LocalDateTime end) {

        AtomicInteger rowIdx = new AtomicInteger(0);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        Row titleRow = sheet.createRow(rowIdx.getAndIncrement());
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(settings.getWebsiteName() + " - Financial Report");
        titleCell.setCellStyle(headerStyle);

        String dateRange = "For all completed orders";
        if (start != null && end != null) {
            dateRange = "For orders from " + start.format(dtf) + " to " + end.format(dtf);
        } else if (start != null) {
            dateRange = "For orders since " + start.format(dtf);
        } else if (end != null) {
            dateRange = "For orders up to " + end.format(dtf);
        }
        Row dateRow = sheet.createRow(rowIdx.getAndIncrement());
        dateRow.createCell(0).setCellValue(dateRange);
        dateRow.getCell(0).setCellStyle(boldStyle);

        rowIdx.getAndIncrement();

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalCogs = BigDecimal.ZERO;

        for (Order order : orders) {
            totalSales = totalSales.add(order.getTotalAmount());
            totalCogs = totalCogs.add(orderService.calculateCogsForOrder(order));
        }
        BigDecimal grossProfit = totalSales.subtract(totalCogs);

        Row headerRow = sheet.createRow(rowIdx.getAndIncrement());
        headerRow.createCell(0).setCellValue("Metric");
        headerRow.createCell(1).setCellValue("Amount");
        headerRow.getCell(0).setCellStyle(boldStyle);
        headerRow.getCell(1).setCellStyle(boldStyle);

        Row salesRow = sheet.createRow(rowIdx.getAndIncrement());
        salesRow.createCell(0).setCellValue("Total Sales (Revenue)");
        createCurrencyCell(salesRow, 1, totalSales, currencyStyle);

        Row cogsRow = sheet.createRow(rowIdx.getAndIncrement());
        cogsRow.createCell(0).setCellValue("Total Cost of Goods Sold (COGS)");
        createCurrencyCell(cogsRow, 1, totalCogs, currencyStyle);

        Row profitRow = sheet.createRow(rowIdx.getAndIncrement());
        profitRow.createCell(0).setCellValue("Gross Profit (Sales - COGS)");
        profitRow.getCell(0).setCellStyle(boldStyle);
        createCurrencyCell(profitRow, 1, grossProfit, totalCurrencyStyle);

        rowIdx.getAndIncrement(); 

        Row countRow = sheet.createRow(rowIdx.getAndIncrement());
        countRow.createCell(0).setCellValue("Total Orders Included");
        countRow.createCell(1).setCellValue(orders.size());
    }

    private void createDetailedBreakdownSheet(Sheet sheet, List<Order> orders, CellStyle headerStyle,
            CellStyle currencyStyle, CellStyle totalRowStyle, CellStyle totalCurrencyStyle) {

        AtomicInteger rowIdx = new AtomicInteger(0);

        String[] headers = { "Order ID", "Date", "Customer", "Items", "Total Sales", "Est. COGS", "Est. Gross Profit" };
        Row headerRow = sheet.createRow(rowIdx.getAndIncrement());
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        BigDecimal grandTotalSales = BigDecimal.ZERO;
        BigDecimal grandTotalCogs = BigDecimal.ZERO;
        BigDecimal grandTotalProfit = BigDecimal.ZERO;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Order order : orders) {
            Row row = sheet.createRow(rowIdx.getAndIncrement());

            BigDecimal orderCogs = orderService.calculateCogsForOrder(order);
            BigDecimal orderProfit = order.getTotalAmount().subtract(orderCogs);

            String items = order.getItems().stream()
                    .map(item -> item.getQuantity() + "x " + item.getProduct().getName())
                    .collect(Collectors.joining(", "));

            row.createCell(0).setCellValue("ORD-" + order.getId());
            row.createCell(1).setCellValue(order.getOrderDate().format(dtf));
            row.createCell(2).setCellValue(order.getShippingFirstName() + " " + order.getShippingLastName());
            row.createCell(3).setCellValue(items);
            createCurrencyCell(row, 4, order.getTotalAmount(), currencyStyle);
            createCurrencyCell(row, 5, orderCogs, currencyStyle);
            createCurrencyCell(row, 6, orderProfit, currencyStyle);

            grandTotalSales = grandTotalSales.add(order.getTotalAmount());
            grandTotalCogs = grandTotalCogs.add(orderCogs);
            grandTotalProfit = grandTotalProfit.add(orderProfit);
        }

        Row totalRow = sheet.createRow(rowIdx.getAndIncrement());
        for (int i = 0; i < headers.length; i++) {
            totalRow.createCell(i).setCellStyle(totalRowStyle);
        }
        totalRow.getCell(3).setCellValue("Grand Totals:");
        createCurrencyCell(totalRow, 4, grandTotalSales, totalCurrencyStyle);
        createCurrencyCell(totalRow, 5, grandTotalCogs, totalCurrencyStyle);
        createCurrencyCell(totalRow, 6, grandTotalProfit, totalCurrencyStyle);
    }

    @Override
    public ByteArrayInputStream generateFinancialReportPdf(String keyword, String startDate, String endDate) throws IOException { 
        LocalDateTime startDateTime = parseDate(startDate, false);
        LocalDateTime endDateTime = parseDate(endDate, true);

        List<Order> orders = orderService.findDeliveredOrdersForReport(keyword, startDateTime, endDateTime); 
        
        return pdfService.generateFinancialReportPdf(orders, startDateTime, endDateTime);
    }

    private List<InventoryItem> getFilteredInventoryItems(String keyword, Long categoryId) {
        return inventoryItemService.searchItems(keyword, categoryId, Pageable.unpaged()).getContent();
    }

    @Override
    public ByteArrayInputStream generateInventoryReport(String keyword, Long categoryId) throws IOException {
        List<InventoryItem> items = getFilteredInventoryItems(keyword, categoryId);
        SiteSettings settings = siteSettingsService.getSiteSettings();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);
            CellStyle totalRowStyle = createTotalRowStyle(workbook);
            CellStyle totalCurrencyStyle = createTotalCurrencyStyle(workbook, totalRowStyle);
            CellStyle numericStyle = workbook.createCellStyle();
            numericStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

            // --- ADDED STYLES ---
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
            
            CellStyle dateTimeStyle = workbook.createCellStyle();
            dateTimeStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));
            // --- END ADDED ---

            Sheet sheet = workbook.createSheet("Inventory Report");
            AtomicInteger rowIdx = new AtomicInteger(0);

            Row titleRow = sheet.createRow(rowIdx.getAndIncrement());
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(settings.getWebsiteName() + " - Inventory Stock Report");
            titleCell.setCellStyle(headerStyle);

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
            Row dateRow = sheet.createRow(rowIdx.getAndIncrement());
            dateRow.createCell(0).setCellValue(filterDesc);
            dateRow.getCell(0).setCellStyle(boldStyle);

            rowIdx.getAndIncrement(); 

            // --- UPDATED HEADERS ---
            String[] headers = { 
            		"Item ID", "Item Name", "Category", "Current Stock", "Unit", "Cost Per Unit", "Total Cost Value", "Item Status", "Stock Status",
            		"Received Date", "Last Updated", "Exp. Days", "Expiration Date"
            };
            // --- END UPDATED HEADERS ---
            
            Row headerRow = sheet.createRow(rowIdx.getAndIncrement());
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            BigDecimal grandTotalValue = BigDecimal.ZERO;
            for (InventoryItem item : items) {
                Row row = sheet.createRow(rowIdx.getAndIncrement());
                
                row.createCell(0).setCellValue(item.getId());
                row.createCell(1).setCellValue(item.getName());
                row.createCell(2).setCellValue(item.getCategory().getName());
                
                Cell stockCell = row.createCell(3);
                stockCell.setCellValue(item.getCurrentStock().doubleValue());
                stockCell.setCellStyle(numericStyle);
                
                row.createCell(4).setCellValue(item.getUnit().getAbbreviation());
                
                createCurrencyCell(row, 5, item.getCostPerUnit(), currencyStyle);
                
                BigDecimal totalCostValue = item.getTotalCostValue();
                createCurrencyCell(row, 6, totalCostValue, currencyStyle);
                
                row.createCell(7).setCellValue(item.getItemStatus());
                row.createCell(8).setCellValue(item.getStockStatus());

                // --- NEW COLUMNS DATA ---
                Cell recvCell = row.createCell(9);
                if (item.getReceivedDate() != null) {
                    recvCell.setCellValue(item.getReceivedDate());
                    recvCell.setCellStyle(dateStyle);
                } else {
                    recvCell.setCellValue("N/A");
                }

                Cell updatedCell = row.createCell(10);
                if (item.getLastUpdated() != null) {
                    updatedCell.setCellValue(item.getLastUpdated());
                    updatedCell.setCellStyle(dateTimeStyle);
                } else {
                    updatedCell.setCellValue("N/A");
                }

                row.createCell(11).setCellValue(item.getExpirationDays());

                Cell expDateCell = row.createCell(12);
                if (item.getExpirationDate() != null) {
                    expDateCell.setCellValue(item.getExpirationDate());
                    expDateCell.setCellStyle(dateStyle);
                } else {
                    expDateCell.setCellValue("N/A");
                }
                // --- END NEW COLUMNS ---

                grandTotalValue = grandTotalValue.add(totalCostValue);
            }

            Row totalRow = sheet.createRow(rowIdx.getAndIncrement());
            for (int i = 0; i < headers.length; i++) {
                totalRow.createCell(i).setCellStyle(totalRowStyle);
            }
            totalRow.getCell(5).setCellValue("Total Inventory Value:");
            createCurrencyCell(totalRow, 6, grandTotalValue, totalCurrencyStyle);
            
            autoSizeColumns(sheet, headers.length);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Override
    public ByteArrayInputStream generateInventoryReportPdf(String keyword, Long categoryId) throws IOException {
        List<InventoryItem> items = getFilteredInventoryItems(keyword, categoryId);
        return pdfService.generateInventoryReportPdf(items, keyword, categoryId);
    }

    @Override
    public ByteArrayInputStream generateProductReport(String keyword, Long categoryId) throws IOException {
        List<Product> products = getFilteredProducts(keyword, categoryId);
        SiteSettings settings = siteSettingsService.getSiteSettings();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);
            
            // --- ADDED STYLES ---
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
            
            CellStyle dateTimeStyle = workbook.createCellStyle();
            dateTimeStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm"));

            Sheet sheet = workbook.createSheet("Product Report");
            AtomicInteger rowIdx = new AtomicInteger(0);

            Row titleRow = sheet.createRow(rowIdx.getAndIncrement());
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(settings.getWebsiteName() + " - Product & Recipe Report");
            titleCell.setCellStyle(headerStyle);

            String filterDesc = "Filters: ";
            if (StringUtils.hasText(keyword)) {
                filterDesc += "Keyword='" + keyword + "' ";
            }
            if (categoryId != null) {
                filterDesc += "Category ID='" + categoryId + "'";
            }
            if (!StringUtils.hasText(keyword) && categoryId == null) {
                filterDesc += "None (All Products)";
            }
            Row dateRow = sheet.createRow(rowIdx.getAndIncrement());
            dateRow.createCell(0).setCellValue(filterDesc);
            dateRow.getCell(0).setCellStyle(boldStyle);

            rowIdx.getAndIncrement(); 
            
            // --- UPDATED HEADERS ---
            String[] headers = { 
            		"Product ID", "Product Name", "Category", "Price", "Current Stock", "Product Status", "Stock Status", 
            		"Created/Received", "Last Stock Update", "Exp. Days", "Exp. Date",
            		"Recipe Ingredients" 
            };
            
            Row headerRow = sheet.createRow(rowIdx.getAndIncrement());
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (Product product : products) {
                Row row = sheet.createRow(rowIdx.getAndIncrement());

                String recipe = product.getIngredients().stream()
                        .map(ing -> ing.getQuantityNeeded() + " " +
                                (ing.getInventoryItem().getUnit() != null ? ing.getInventoryItem().getUnit().getAbbreviation() : "units") +
                                " of " + ing.getInventoryItem().getName())
                        .collect(Collectors.joining("\n")); 

                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getCategory().getName());
                createCurrencyCell(row, 3, product.getPrice(), currencyStyle);
                row.createCell(4).setCellValue(product.getCurrentStock());
                row.createCell(5).setCellValue(product.getProductStatus());
                row.createCell(6).setCellValue(product.getStockStatus());
                
                // --- NEW COLUMNS DATA ---
                Cell createdCell = row.createCell(7);
                if(product.getCreatedDate() != null) {
                    createdCell.setCellValue(product.getCreatedDate());
                    createdCell.setCellStyle(dateStyle);
                } else {
                    createdCell.setCellValue("N/A");
                }
                
                Cell updatedCell = row.createCell(8);
                if(product.getStockLastUpdated() != null) {
                    updatedCell.setCellValue(product.getStockLastUpdated());
                    updatedCell.setCellStyle(dateTimeStyle);
                } else {
                    updatedCell.setCellValue("N/A");
                }

                row.createCell(9).setCellValue(product.getExpirationDays());

                Cell expDateCell = row.createCell(10);
                if(product.getExpirationDate() != null) {
                    expDateCell.setCellValue(product.getExpirationDate());
                    expDateCell.setCellStyle(dateStyle);
                } else {
                    expDateCell.setCellValue("N/A");
                }
                
                Cell recipeCell = row.createCell(11);
                recipeCell.setCellValue(recipe);
                CellStyle wrapStyle = workbook.createCellStyle();
                wrapStyle.setWrapText(true);
                recipeCell.setCellStyle(wrapStyle);
            }
            
            autoSizeColumns(sheet, headers.length);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private List<Product> getFilteredProducts(String keyword, Long categoryId) {
        return productService.findAllForReport(keyword, categoryId);
    }

    @Override
    public ByteArrayInputStream generateProductReportPdf(String keyword, Long categoryId) throws IOException {
        List<Product> products = getFilteredProducts(keyword, categoryId);
        return pdfService.generateProductReportPdf(products, keyword, categoryId);
    }
    
    @Override
    public ByteArrayInputStream generateWasteReport(String keyword, String reasonCategory, String wasteType) throws IOException {
        Page<ActivityLogEntry> wasteLogs = activityLogService.searchWasteLogs(keyword, reasonCategory, wasteType, Pageable.unpaged()); 
        SiteSettings settings = siteSettingsService.getSiteSettings();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook); 
            CellStyle dateTimeStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            dateTimeStyle.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm:ss"));


            Sheet sheet = workbook.createSheet("Waste & Spoilage Log");
            AtomicInteger rowIdx = new AtomicInteger(0);

            Row titleRow = sheet.createRow(rowIdx.getAndIncrement());
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(settings.getWebsiteName() + " - Waste & Spoilage Log");
            titleCell.setCellStyle(headerStyle);

            String filterDesc = "Filters: ";
            if (StringUtils.hasText(keyword)) {
                filterDesc += "Item Keyword='" + keyword + "' ";
            }
            if (StringUtils.hasText(reasonCategory)) {
                filterDesc += "Reason='" + reasonCategory + "'";
            }
            if (StringUtils.hasText(wasteType)) {
                filterDesc += "Type='" + wasteType + "'";
            }
            if (!StringUtils.hasText(keyword) && !StringUtils.hasText(reasonCategory) && !StringUtils.hasText(wasteType)) {
                filterDesc += "None (All Records)";
            }
            Row dateRow = sheet.createRow(rowIdx.getAndIncrement());
            dateRow.createCell(0).setCellValue(filterDesc);
            dateRow.getCell(0).setCellStyle(boldStyle);

            rowIdx.getAndIncrement(); 
            
            String[] headers = { "Timestamp", "Admin User", "Type", "Reason", "Item Name", "Cost/Unit", "Total Value", "Details" };
            Row headerRow = sheet.createRow(rowIdx.getAndIncrement());
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (ActivityLogEntry logEntry : wasteLogs.getContent()) {
                Row row = sheet.createRow(rowIdx.getAndIncrement());

                Cell timestampCell = row.createCell(0);
                timestampCell.setCellValue(logEntry.getTimestamp());
                timestampCell.setCellStyle(dateTimeStyle);

                row.createCell(1).setCellValue(logEntry.getUsername());
                
                // Type Logic
                String type = "Unknown";
                if (logEntry.getAction().startsWith("PRODUCT_")) type = "Product";
                else if (logEntry.getAction().startsWith("STOCK_")) type = "Inventory";
                row.createCell(2).setCellValue(type);
                
                // Reason Logic
                String reason = logEntry.getAction().replace("STOCK_WASTE_", "").replace("PRODUCT_WASTE_", "");
                row.createCell(3).setCellValue(reason);
                
                row.createCell(4).setCellValue(logEntry.getItemName() != null ? logEntry.getItemName() : "N/A");
                
                if (logEntry.getCostPerUnit() != null) {
                    createCurrencyCell(row, 5, logEntry.getCostPerUnit(), currencyStyle);
                } else {
                    row.createCell(5).setCellValue("N/A");
                }
                
                if (logEntry.getTotalValue() != null) {
                    createCurrencyCell(row, 6, logEntry.getTotalValue(), currencyStyle);
                } else {
                    row.createCell(6).setCellValue("N/A");
                }
                
                row.createCell(7).setCellValue(logEntry.getDetails());
            }
            
            autoSizeColumns(sheet, headers.length);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
    
    @Override
    public ByteArrayInputStream generateWasteReportPdf(String keyword, String reasonCategory, String wasteType) throws IOException {
        Page<ActivityLogEntry> wasteLogs = activityLogService.searchWasteLogs(keyword, reasonCategory, wasteType, Pageable.unpaged());
        return pdfService.generateWasteLogPdf(wasteLogs, keyword, reasonCategory, wasteType); 
    }

    @Override
    public ByteArrayInputStream generateOrderDocumentPdf(Order order, String documentType) throws IOException, IllegalArgumentException {
        return pdfService.generateOrderDocumentPdf(order, documentType);
    }
    
    @Override
    public ByteArrayInputStream generateActivityLogPdf(Pageable pageable) throws IOException {
        Page<ActivityLogEntry> logPage = activityLogService.getAllLogs(pageable);
        return pdfService.generateActivityLogPdf(logPage);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("₱#,##0.00"));
        return style;
    }

    private CellStyle createTotalRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTotalCurrencyStyle(Workbook workbook, CellStyle baseStyle) {
        CellStyle style = workbook.createCellStyle();
        style.cloneStyleFrom(baseStyle);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("₱#,##0.00"));
        return style;
    }

    private void createCurrencyCell(Row row, int colIndex, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}