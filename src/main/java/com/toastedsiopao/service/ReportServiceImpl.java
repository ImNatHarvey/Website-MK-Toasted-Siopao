package com.toastedsiopao.service;

import com.toastedsiopao.model.Order;
import com.toastedsiopao.model.OrderItem;
import com.toastedsiopao.model.SiteSettings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    public ByteArrayInputStream generateFinancialReport(String startDate, String endDate) throws IOException {
        LocalDateTime startDateTime = parseDate(startDate, false);
        LocalDateTime endDateTime = parseDate(endDate, true);

        List<Order> orders = orderService.findDeliveredOrdersForReport(startDateTime, endDateTime);
        SiteSettings settings = siteSettingsService.getSiteSettings();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {

            // === Create Cell Styles ===
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle boldStyle = createBoldStyle(workbook);
            CellStyle totalRowStyle = createTotalRowStyle(workbook);
            CellStyle totalCurrencyStyle = createTotalCurrencyStyle(workbook, totalRowStyle);

            // === Summary Sheet ===
            Sheet summarySheet = workbook.createSheet("Summary");
            // --- THIS IS THE FIX: Added totalCurrencyStyle to the method call ---
            createSummarySheet(summarySheet, orders, settings, headerStyle, boldStyle, currencyStyle, totalCurrencyStyle, startDateTime, endDateTime);

            // === Detailed Breakdown Sheet ===
            Sheet detailSheet = workbook.createSheet("Detailed Breakdown");
            createDetailedBreakdownSheet(detailSheet, orders, headerStyle, currencyStyle, totalRowStyle, totalCurrencyStyle);

            // Auto-size columns for both sheets
            autoSizeColumns(summarySheet, 3);
            autoSizeColumns(detailSheet, 7);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void createSummarySheet(Sheet sheet, List<Order> orders, SiteSettings settings,
            CellStyle headerStyle, CellStyle boldStyle, CellStyle currencyStyle,
            // --- THIS IS THE FIX: Added totalCurrencyStyle as a parameter ---
            CellStyle totalCurrencyStyle,
            LocalDateTime start, LocalDateTime end) {

        AtomicInteger rowIdx = new AtomicInteger(0);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        // --- Title Row ---
        Row titleRow = sheet.createRow(rowIdx.getAndIncrement());
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(settings.getWebsiteName() + " - Financial Report");
        titleCell.setCellStyle(headerStyle);

        // --- Date Range Row ---
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

        // --- Spacer Row ---
        rowIdx.getAndIncrement();

        // --- Summary Data ---
        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal totalCogs = BigDecimal.ZERO;

        for (Order order : orders) {
            totalSales = totalSales.add(order.getTotalAmount());
            totalCogs = totalCogs.add(orderService.calculateCogsForOrder(order));
        }
        BigDecimal grossProfit = totalSales.subtract(totalCogs);

        // --- Totals Table ---
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

        rowIdx.getAndIncrement(); // Spacer

        Row countRow = sheet.createRow(rowIdx.getAndIncrement());
        countRow.createCell(0).setCellValue("Total Orders Included");
        countRow.createCell(1).setCellValue(orders.size());
    }

    private void createDetailedBreakdownSheet(Sheet sheet, List<Order> orders, CellStyle headerStyle,
            CellStyle currencyStyle, CellStyle totalRowStyle, CellStyle totalCurrencyStyle) {

        AtomicInteger rowIdx = new AtomicInteger(0);

        // --- Header Row ---
        String[] headers = { "Order ID", "Date", "Customer", "Items", "Total Sales", "Est. COGS", "Est. Gross Profit" };
        Row headerRow = sheet.createRow(rowIdx.getAndIncrement());
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // --- Data Rows ---
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

        // --- Total Row ---
        Row totalRow = sheet.createRow(rowIdx.getAndIncrement());
        for (int i = 0; i < headers.length; i++) {
            totalRow.createCell(i).setCellStyle(totalRowStyle);
        }
        totalRow.getCell(3).setCellValue("Grand Totals:");
        createCurrencyCell(totalRow, 4, grandTotalSales, totalCurrencyStyle);
        createCurrencyCell(totalRow, 5, grandTotalCogs, totalCurrencyStyle);
        createCurrencyCell(totalRow, 6, grandTotalProfit, totalCurrencyStyle);
    }

    // === Styling Helpers ===

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

    // === PDF Generation (Placeholder) ===

    @Override
    public ByteArrayInputStream generateFinancialReportPdf(String startDate, String endDate) throws IOException {
        LocalDateTime startDateTime = parseDate(startDate, false);
        LocalDateTime endDateTime = parseDate(endDate, true);

        List<Order> orders = orderService.findDeliveredOrdersForReport(startDateTime, endDateTime);
        
        // This will be implemented in the next batch
        return pdfService.generateFinancialReportPdf(orders, startDateTime, endDateTime);
    }
}