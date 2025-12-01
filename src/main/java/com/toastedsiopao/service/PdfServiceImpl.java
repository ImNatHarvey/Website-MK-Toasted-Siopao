package com.toastedsiopao.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
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

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
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

	private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
	private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
	private static final Font FONT_TABLE_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
	private static final Font FONT_TABLE_CELL = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
	private static final Font FONT_BOLD_CELL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
	private static final Font FONT_TOTAL_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
	private static final Font FONT_TOTAL_CELL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

// Invoice Specific Fonts
	private static final Font FONT_INVOICE_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24,
			new Color(17, 63, 103));
	private static final Font FONT_INVOICE_STORE_NAME = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14,
			Color.BLACK);
	private static final Font FONT_INVOICE_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10,
			new Color(100, 100, 100));
	private static final Font FONT_INVOICE_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
	private static final Font FONT_SECTION_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12,
			new Color(17, 63, 103));

	private static final Color COLOR_TABLE_HEADER_BG = new Color(17, 63, 103);
	private static final Color COLOR_TOTAL_ROW_BG = new Color(240, 240, 240);

	@Override
	public ByteArrayInputStream generateFinancialReportPdf(List<Order> orders, LocalDateTime start, LocalDateTime end)
			throws IOException {
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
						.collect(Collectors.joining("\n"));

				addTableCell(detailTable, "ORD-" + order.getId(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, order.getOrderDate().format(orderDtf), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, order.getShippingFirstName() + " " + order.getShippingLastName(),
						FONT_TABLE_CELL, Element.ALIGN_LEFT);
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
	public ByteArrayInputStream generateInventoryReportPdf(List<InventoryItem> items, String keyword, Long categoryId)
			throws IOException {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		DateTimeFormatter genDateFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

		try (Document document = new Document(PageSize.A4.rotate())) {
			PdfWriter.getInstance(document, out);
			document.open();
			Paragraph title = new Paragraph(settings.getWebsiteName() + " - Inventory Stock Report", FONT_TITLE);
			title.setAlignment(Element.ALIGN_CENTER);
			document.add(title);
			Paragraph genDate = new Paragraph("Generated on: " + LocalDateTime.now().format(genDateFmt), FONT_SUBTITLE);
			genDate.setAlignment(Element.ALIGN_CENTER);
			genDate.setSpacingAfter(5f);
			document.add(genDate);

			String filterDesc = "Filters: " + (StringUtils.hasText(keyword) ? "Keyword='" + keyword + "' " : "")
					+ (categoryId != null ? "Category ID " + categoryId : "");
			Paragraph subtitle = new Paragraph(filterDesc, FONT_SUBTITLE);
			subtitle.setAlignment(Element.ALIGN_CENTER);
			subtitle.setSpacingAfter(15f);
			document.add(subtitle);

			BigDecimal grandTotalValue = items.stream().map(InventoryItem::getTotalCostValue).reduce(BigDecimal.ZERO,
					BigDecimal::add);

			PdfPTable summaryTable = new PdfPTable(4);
			summaryTable.setWidthPercentage(100);
			summaryTable.setWidths(new float[] { 2f, 1f, 2.5f, 2f });
			summaryTable.setSpacingAfter(20f);
			addSummaryCell(summaryTable, "Total Item Types:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
			addSummaryCell(summaryTable, String.valueOf(items.size()), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
			addSummaryCell(summaryTable, "Total Inventory Value:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT);
			addSummaryCell(summaryTable, formatCurrency(grandTotalValue), FONT_TOTAL_CELL, Element.ALIGN_LEFT);
			document.add(summaryTable);

			PdfPTable detailTable = new PdfPTable(12);
			detailTable.setWidthPercentage(100);
			detailTable
					.setWidths(new float[] { 0.7f, 2.0f, 1.5f, 1.0f, 0.8f, 1.0f, 1.0f, 1.0f, 1.2f, 1.2f, 0.8f, 1.2f });

			addTableHeader(detailTable, "ID");
			addTableHeader(detailTable, "Item Name");
			addTableHeader(detailTable, "Category");
			addTableHeader(detailTable, "Stock");
			addTableHeader(detailTable, "Unit");
			addTableHeader(detailTable, "Cost/Unit");
			addTableHeader(detailTable, "Total");
			addTableHeader(detailTable, "Status");
			addTableHeader(detailTable, "Received");
			addTableHeader(detailTable, "Updated");
			addTableHeader(detailTable, "Exp Day");
			addTableHeader(detailTable, "Exp Date");

			for (InventoryItem item : items) {
				addTableCell(detailTable, item.getId().toString(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, item.getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, item.getCategory().getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, item.getCurrentStock().toString(), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
				addTableCell(detailTable, item.getUnit().getAbbreviation(), FONT_TABLE_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable, formatCurrency(item.getCostPerUnit()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
				addTableCell(detailTable, formatCurrency(item.getTotalCostValue()), FONT_TABLE_CELL,
						Element.ALIGN_RIGHT);
				addTableCell(detailTable, item.getStockStatus(), FONT_BOLD_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable,
						item.getReceivedDate() != null ? item.getReceivedDate().format(dateFmt) : "N/A",
						FONT_TABLE_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable,
						item.getLastUpdated() != null ? item.getLastUpdated().format(dateTimeFmt) : "N/A",
						FONT_TABLE_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable, String.valueOf(item.getExpirationDays()), FONT_TABLE_CELL,
						Element.ALIGN_CENTER);
				addTableCell(detailTable,
						item.getExpirationDate() != null ? item.getExpirationDate().format(dateFmt) : "N/A",
						FONT_TABLE_CELL, Element.ALIGN_CENTER);
			}
			document.add(detailTable);
		} catch (DocumentException e) {
			throw new IOException("Error creating PDF document", e);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	@Override
	public ByteArrayInputStream generateProductReportPdf(List<Product> products, String keyword, Long categoryId)
			throws IOException {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		DateTimeFormatter genDateFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

		try (Document document = new Document(PageSize.A4.rotate())) {
			PdfWriter.getInstance(document, out);
			document.open();
			Paragraph title = new Paragraph(settings.getWebsiteName() + " - Product & Recipe Report", FONT_TITLE);
			title.setAlignment(Element.ALIGN_CENTER);
			document.add(title);
			Paragraph genDate = new Paragraph("Generated on: " + LocalDateTime.now().format(genDateFmt), FONT_SUBTITLE);
			genDate.setAlignment(Element.ALIGN_CENTER);
			genDate.setSpacingAfter(15f);
			document.add(genDate);

			PdfPTable detailTable = new PdfPTable(12);
			detailTable.setWidthPercentage(100);
			detailTable
					.setWidths(new float[] { 0.6f, 1.8f, 1.2f, 0.9f, 0.8f, 0.9f, 0.9f, 1.1f, 1.3f, 0.7f, 1.1f, 2.5f });

			addTableHeader(detailTable, "ID");
			addTableHeader(detailTable, "Product Name");
			addTableHeader(detailTable, "Category");
			addTableHeader(detailTable, "Price");
			addTableHeader(detailTable, "Stock");
			addTableHeader(detailTable, "Status");
			addTableHeader(detailTable, "Stk Lvl");
			addTableHeader(detailTable, "Created");
			addTableHeader(detailTable, "Updated");
			addTableHeader(detailTable, "Exp Day");
			addTableHeader(detailTable, "Exp Date");
			addTableHeader(detailTable, "Recipe Ingredients");

			for (Product product : products) {
				String recipe = product.getIngredients().stream()
						.map(ing -> ing.getQuantityNeeded() + " "
								+ (ing.getInventoryItem().getUnit() != null
										? ing.getInventoryItem().getUnit().getAbbreviation()
										: "units")
								+ " " + ing.getInventoryItem().getName())
						.collect(Collectors.joining("\n"));
				addTableCell(detailTable, product.getId().toString(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, product.getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, product.getCategory().getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, formatCurrency(product.getPrice()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
				addTableCell(detailTable, product.getCurrentStock().toString(), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
				addTableCell(detailTable, product.getProductStatus(), FONT_TABLE_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable, product.getStockStatus(), FONT_BOLD_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable,
						product.getCreatedDate() != null ? product.getCreatedDate().format(dateFmt) : "N/A",
						FONT_TABLE_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable,
						product.getStockLastUpdated() != null ? product.getStockLastUpdated().format(dateTimeFmt)
								: "N/A",
						FONT_TABLE_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable, String.valueOf(product.getExpirationDays()), FONT_TABLE_CELL,
						Element.ALIGN_CENTER);
				addTableCell(detailTable,
						product.getExpirationDate() != null ? product.getExpirationDate().format(dateFmt) : "N/A",
						FONT_TABLE_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable, recipe.isEmpty() ? "N/A" : recipe, FONT_TABLE_CELL, Element.ALIGN_LEFT);
			}
			document.add(detailTable);
		} catch (DocumentException e) {
			throw new IOException("Error creating PDF document", e);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	@Override
	public ByteArrayInputStream generateOrderDocumentPdf(Order order, String documentType) throws IOException {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try (Document document = new Document(PageSize.A4)) {
			PdfWriter.getInstance(document, out);
			document.open();

			// 1. HEADER: Store Info
			Paragraph storeName = new Paragraph(settings.getWebsiteName(), FONT_INVOICE_STORE_NAME);
			storeName.setAlignment(Element.ALIGN_RIGHT);
			document.add(storeName);

			Paragraph storeContact = new Paragraph(
					(settings.getContactPhoneName() != null ? settings.getContactPhoneName() : "No Phone") + "\n"
							+ (settings.getContactFacebookName() != null ? settings.getContactFacebookName() : ""),
					FONT_SUBTITLE);
			storeContact.setAlignment(Element.ALIGN_RIGHT);
			storeContact.setSpacingAfter(20f);
			document.add(storeContact);

			// 2. TITLE
			String titleText = "INVOICE";
			if ("RECEIPT".equalsIgnoreCase(documentType)) {
				titleText = "OFFICIAL RECEIPT";
			}
			Paragraph title = new Paragraph(titleText, FONT_INVOICE_TITLE);
			title.setAlignment(Element.ALIGN_LEFT);
			title.setSpacingAfter(20f);
			document.add(title);

			// 3. ORDER & BILLING DETAILS (2 Columns)
			PdfPTable metaTable = new PdfPTable(2);
			metaTable.setWidthPercentage(100);
			metaTable.setWidths(new float[] { 1f, 1f });

			// Left Column: Bill To
			PdfPCell billToCell = new PdfPCell();
			billToCell.setBorder(Rectangle.NO_BORDER);
			billToCell.addElement(new Phrase("BILL TO:", FONT_INVOICE_LABEL));
			billToCell.addElement(
					new Phrase(order.getShippingFirstName() + " " + order.getShippingLastName(), FONT_INVOICE_VALUE));
			billToCell.addElement(new Phrase(order.getShippingAddress(), FONT_INVOICE_VALUE));
			billToCell.addElement(new Phrase(order.getShippingPhone(), FONT_INVOICE_VALUE));
			billToCell.addElement(new Phrase(order.getShippingEmail(), FONT_INVOICE_VALUE));
			metaTable.addCell(billToCell);

			// Right Column: Order Details
			PdfPCell orderDetailsCell = new PdfPCell();
			orderDetailsCell.setBorder(Rectangle.NO_BORDER);
			orderDetailsCell.setHorizontalAlignment(Element.ALIGN_RIGHT); // Content inside aligns right

			// We use a nested table to align labels and values nicely in the right column
			PdfPTable detailsTable = new PdfPTable(2);
			detailsTable.setWidthPercentage(100);
			detailsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

			addDetailRow(detailsTable, "Order ID:", "#ORD-" + order.getId());
			addDetailRow(detailsTable, "Date:",
					order.getOrderDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")));

			// Formatted Status logic to match UI
			String statusText = order.getStatus().replace("_", " ");
			if ("PENDING".equals(order.getStatus()) && "COD".equalsIgnoreCase(order.getPaymentMethod())) {
				statusText = "PENDING (COD)";
			} else if ("PENDING_VERIFICATION".equals(order.getStatus())) {
				statusText = "PENDING (GCASH)";
			} else if ("OUT_FOR_DELIVERY".equals(order.getStatus())) {
				statusText = "OUT FOR DELIVERY";
			}

			addDetailRow(detailsTable, "Status:", statusText);
			addDetailRow(detailsTable, "Payment Method:", order.getPaymentMethod().toUpperCase());
			addDetailRow(detailsTable, "Payment Status:", order.getPaymentStatus().replace("_", " "));

			if ("GCASH".equalsIgnoreCase(order.getPaymentMethod()) && StringUtils.hasText(order.getTransactionId())) {
				addDetailRow(detailsTable, "Transaction ID:", order.getTransactionId());
			}

			orderDetailsCell.addElement(detailsTable);
			metaTable.addCell(orderDetailsCell);

			document.add(metaTable);

			// Spacer
			Paragraph spacer = new Paragraph(" ");
			spacer.setSpacingAfter(10f);
			document.add(spacer);

			// 4. ITEMS TABLE
			PdfPTable itemsTable = new PdfPTable(4);
			itemsTable.setWidthPercentage(100);
			itemsTable.setWidths(new float[] { 3f, 1f, 1.5f, 1.5f });
			itemsTable.setSpacingBefore(10f);

			addTableHeader(itemsTable, "Item Description");
			addTableHeader(itemsTable, "Qty");
			addTableHeader(itemsTable, "Unit Price");
			addTableHeader(itemsTable, "Amount");

			for (OrderItem item : order.getItems()) {
				addTableCell(itemsTable, item.getProduct().getName(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(itemsTable, String.valueOf(item.getQuantity()), FONT_TABLE_CELL, Element.ALIGN_CENTER);
				addTableCell(itemsTable, formatCurrency(item.getPricePerUnit()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
				addTableCell(itemsTable, formatCurrency(item.getTotalPrice()), FONT_TABLE_CELL, Element.ALIGN_RIGHT);
			}

			// 5. TOTALS
			// Empty cells for spacing
			itemsTable.addCell(createNoBorderCell());
			itemsTable.addCell(createNoBorderCell());

			// Subtotal Label & Value
			PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL AMOUNT", FONT_INVOICE_LABEL));
			totalLabelCell.setBorder(Rectangle.TOP);
			totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			totalLabelCell.setPaddingTop(10f);
			itemsTable.addCell(totalLabelCell);

			PdfPCell totalValueCell = new PdfPCell(
					new Phrase(formatCurrency(order.getTotalAmount()), FONT_INVOICE_TITLE));
			totalValueCell.setBorder(Rectangle.TOP);
			totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			totalValueCell.setPaddingTop(10f);
			itemsTable.addCell(totalValueCell);

			document.add(itemsTable);

			// 6. FOOTER NOTES
			Paragraph notes = new Paragraph("\n\nThank you for your business!", FONT_SUBTITLE);
			notes.setAlignment(Element.ALIGN_CENTER);
			document.add(notes);

			if (StringUtils.hasText(order.getNotes())) {
				Paragraph customerNotes = new Paragraph("\nNotes: " + order.getNotes(), FONT_TABLE_CELL);
				document.add(customerNotes);
			}

			// Generated Date at bottom
			Paragraph genDate = new Paragraph(
					"\nGenerated on: "
							+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")),
					FONT_SUBTITLE);
			genDate.setAlignment(Element.ALIGN_CENTER);
			genDate.setSpacingBefore(20f);
			document.add(genDate);

			// 7. GCASH RECEIPT IMAGE (If available and asked for)
			if (StringUtils.hasText(order.getPaymentReceiptImageUrl())) {
				try {
					document.newPage();
					Paragraph receiptHeader = new Paragraph("Payment Receipt Attachment", FONT_SECTION_HEADER);
					receiptHeader.setSpacingAfter(10f);
					document.add(receiptHeader);

					Resource imageResource = fileStorageService.loadAsResource(order.getPaymentReceiptImageUrl());

					if (imageResource != null && imageResource.exists()) {
						Image img = Image.getInstance(imageResource.getFile().getAbsolutePath());

						// Scale image to fit page
						float maxWidth = document.getPageSize().getWidth() - document.leftMargin()
								- document.rightMargin();
						float maxHeight = document.getPageSize().getHeight() - document.topMargin()
								- document.bottomMargin() - 50; // minus header space

						if (img.getScaledWidth() > maxWidth || img.getScaledHeight() > maxHeight) {
							img.scaleToFit(maxWidth, maxHeight);
						}

						img.setAlignment(Element.ALIGN_CENTER);
						document.add(img);
					} else {
						document.add(new Paragraph("[Image file not found on server]", FONT_TABLE_CELL));
					}
				} catch (Exception e) {
					log.error("Failed to embed receipt image in PDF: {}", e.getMessage());
					document.add(new Paragraph("[Error loading receipt image]", FONT_TABLE_CELL));
				}
			}

		} catch (DocumentException e) {
			throw new IOException("Error creating PDF document", e);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	private void addDetailRow(PdfPTable table, String label, String value) {
		PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_INVOICE_LABEL));
		labelCell.setBorder(Rectangle.NO_BORDER);
		labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		table.addCell(labelCell);

		PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_INVOICE_VALUE));
		valueCell.setBorder(Rectangle.NO_BORDER);
		valueCell.setHorizontalAlignment(Element.ALIGN_LEFT); // Align value to left of its column (next to label)
		valueCell.setPaddingLeft(10f);
		table.addCell(valueCell);
	}

	private PdfPCell createNoBorderCell() {
		PdfPCell cell = new PdfPCell(new Phrase(""));
		cell.setBorder(Rectangle.NO_BORDER);
		return cell;
	}

	@Override
	public ByteArrayInputStream generateActivityLogPdf(Page<ActivityLogEntry> logPage, String keyword, String startDate,
			String endDate) throws IOException {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		DateTimeFormatter genDateFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

		try (Document document = new Document(PageSize.A4.rotate())) {
			PdfWriter.getInstance(document, out);
			document.open();

			Paragraph title = new Paragraph(settings.getWebsiteName() + " - Admin Activity Log", FONT_TITLE);
			title.setAlignment(Element.ALIGN_CENTER);
			document.add(title);

			Paragraph genDate = new Paragraph("Generated on: " + LocalDateTime.now().format(genDateFmt), FONT_SUBTITLE);
			genDate.setAlignment(Element.ALIGN_CENTER);
			genDate.setSpacingAfter(5f);
			document.add(genDate);

			String filterDesc = "Filters: ";
			if (StringUtils.hasText(keyword)) {
				filterDesc += "Keyword='" + keyword + "' ";
			}
			if (StringUtils.hasText(startDate)) {
				filterDesc += "From='" + startDate + "' ";
			}
			if (StringUtils.hasText(endDate)) {
				filterDesc += "To='" + endDate + "' ";
			}
			if (!StringUtils.hasText(keyword) && !StringUtils.hasText(startDate) && !StringUtils.hasText(endDate)) {
				filterDesc += "None (All Logs)";
			}

			Paragraph subtitle = new Paragraph(filterDesc, FONT_SUBTITLE);
			subtitle.setAlignment(Element.ALIGN_CENTER);
			subtitle.setSpacingAfter(15f);
			document.add(subtitle);

			PdfPTable detailTable = new PdfPTable(4);
			detailTable.setWidthPercentage(100);
			detailTable.setWidths(new float[] { 1.5f, 1f, 1.5f, 4f });
			addTableHeader(detailTable, "Timestamp");
			addTableHeader(detailTable, "User");
			addTableHeader(detailTable, "Action");
			addTableHeader(detailTable, "Details");
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			for (ActivityLogEntry logEntry : logPage.getContent()) {
				addTableCell(detailTable, logEntry.getTimestamp().format(dtf), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, logEntry.getUsername(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, logEntry.getAction(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, logEntry.getDetails(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
			}
			document.add(detailTable);
		} catch (DocumentException e) {
			throw new IOException("Error creating PDF document", e);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	@Override
	public ByteArrayInputStream generateWasteLogPdf(Page<ActivityLogEntry> logPage, String keyword,
			String reasonCategory, String wasteType, String startDate, String endDate) throws IOException {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		DateTimeFormatter genDateFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy h:mm a");

		try (Document document = new Document(PageSize.A4.rotate())) {
			PdfWriter.getInstance(document, out);
			document.open();
			Paragraph title = new Paragraph(settings.getWebsiteName() + " - Waste & Spoilage Log", FONT_TITLE);
			title.setAlignment(Element.ALIGN_CENTER);
			document.add(title);

			Paragraph genDate = new Paragraph("Generated on: " + LocalDateTime.now().format(genDateFmt), FONT_SUBTITLE);
			genDate.setAlignment(Element.ALIGN_CENTER);
			genDate.setSpacingAfter(5f);
			document.add(genDate);

			String filterDesc = "Filters: ";
			if (StringUtils.hasText(keyword)) {
				filterDesc += "Item Name='" + keyword + "' ";
			}
			if (StringUtils.hasText(reasonCategory)) {
				filterDesc += "Reason='" + reasonCategory + "' ";
			}
			if (StringUtils.hasText(wasteType)) {
				filterDesc += "Type='" + wasteType + "' ";
			}
			if (StringUtils.hasText(startDate)) {
				filterDesc += "From='" + startDate + "' ";
			}
			if (StringUtils.hasText(endDate)) {
				filterDesc += "To='" + endDate + "' ";
			}
			if (filterDesc.equals("Filters: ")) {
				filterDesc += "None (All Waste Records)";
			}

			Paragraph subtitle = new Paragraph(filterDesc, FONT_SUBTITLE);
			subtitle.setAlignment(Element.ALIGN_CENTER);
			subtitle.setSpacingAfter(15f);
			document.add(subtitle);

			PdfPTable detailTable = new PdfPTable(7);
			detailTable.setWidthPercentage(100);
			detailTable.setWidths(new float[] { 1.2f, 1.0f, 1.0f, 1.5f, 1.0f, 1.0f, 2.0f });

			addTableHeader(detailTable, "Timestamp");
			addTableHeader(detailTable, "Admin User");
			addTableHeader(detailTable, "Reason Category");
			addTableHeader(detailTable, "Item Name");
			addTableHeader(detailTable, "Cost/Unit");
			addTableHeader(detailTable, "Total Value");
			addTableHeader(detailTable, "Details");

			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			BigDecimal grandTotalWaste = BigDecimal.ZERO;

			for (ActivityLogEntry logEntry : logPage.getContent()) {
				addTableCell(detailTable, logEntry.getTimestamp().format(dtf), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				addTableCell(detailTable, logEntry.getUsername(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
				String reason = logEntry.getAction().replace("STOCK_WASTE_", "").replace("PRODUCT_WASTE_", "");
				addTableCell(detailTable, reason, FONT_TABLE_CELL, Element.ALIGN_CENTER);
				addTableCell(detailTable, logEntry.getItemName() != null ? logEntry.getItemName() : "N/A",
						FONT_TABLE_CELL, Element.ALIGN_LEFT);

				if (logEntry.getCostPerUnit() != null) {
					createCurrencyCell(detailTable, logEntry.getCostPerUnit(), FONT_TABLE_CELL);
				} else {
					addTableCell(detailTable, "N/A", FONT_TABLE_CELL, Element.ALIGN_RIGHT);
				}

				if (logEntry.getTotalValue() != null) {
					createCurrencyCell(detailTable, logEntry.getTotalValue(), FONT_TABLE_CELL);
					grandTotalWaste = grandTotalWaste.add(logEntry.getTotalValue());
				} else {
					addTableCell(detailTable, "N/A", FONT_TABLE_CELL, Element.ALIGN_RIGHT);
				}

				addTableCell(detailTable, logEntry.getDetails(), FONT_TABLE_CELL, Element.ALIGN_LEFT);
			}

			addTableFooterCell(detailTable, "Total Waste Value:", FONT_TOTAL_HEADER, Element.ALIGN_RIGHT, 5);
			addTableFooterCell(detailTable, formatCurrency(grandTotalWaste), FONT_TOTAL_CELL, Element.ALIGN_RIGHT, 1);
			addTableFooterCell(detailTable, "", FONT_TOTAL_CELL, Element.ALIGN_RIGHT, 1);

			document.add(detailTable);
		} catch (DocumentException e) {
			throw new IOException("Error creating PDF document", e);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	@Override
	public ByteArrayInputStream generateDashboardPdf(Map<String, Object> data) throws IOException {
		SiteSettings settings = siteSettingsService.getSiteSettings();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DateTimeFormatter genDateFmt = DateTimeFormatter.ofPattern("MMMM dd, yyyy h:mm a");

		try (Document document = new Document(PageSize.A4)) {
			PdfWriter.getInstance(document, out);
			document.open();

			Paragraph title = new Paragraph("Admin Dashboard Report", FONT_TITLE);
			title.setAlignment(Element.ALIGN_CENTER);
			document.add(title);

			Paragraph subtitle = new Paragraph(settings.getWebsiteName(), FONT_SUBTITLE);
			subtitle.setAlignment(Element.ALIGN_CENTER);
			document.add(subtitle);

			Paragraph date = new Paragraph("Generated on: " + LocalDateTime.now().format(genDateFmt), FONT_SUBTITLE);
			date.setAlignment(Element.ALIGN_CENTER);
			date.setSpacingAfter(20f);
			document.add(date);

			addSectionHeader(document, "Financial Summary");
			PdfPTable finTable = new PdfPTable(2);
			finTable.setWidthPercentage(100);
			finTable.setWidths(new float[] { 1f, 1f });

			addDashboardRow(finTable, "Sales Today", formatCurrency((BigDecimal) data.get("salesToday")));
			addDashboardRow(finTable, "Sales This Week", formatCurrency((BigDecimal) data.get("salesWeek")));
			addDashboardRow(finTable, "Sales This Month", formatCurrency((BigDecimal) data.get("salesMonth")));

			addDashboardRow(finTable, "Est. COGS Today", formatCurrency((BigDecimal) data.get("cogsToday")));
			addDashboardRow(finTable, "Est. COGS This Month", formatCurrency((BigDecimal) data.get("cogsMonth")));

			addDashboardRow(finTable, "Total Revenue (All Time)",
					formatCurrency((BigDecimal) data.get("totalRevenue")));
			addDashboardRow(finTable, "Potential Revenue (Pending)",
					formatCurrency((BigDecimal) data.get("potentialRevenue")));
			addDashboardRow(finTable, "Average Order Value", formatCurrency((BigDecimal) data.get("avgOrderValue")));

			document.add(finTable);

			addSectionHeader(document, "Order Summary");
			PdfPTable orderTable = new PdfPTable(4);
			orderTable.setWidthPercentage(100);

			addDashboardStatCell(orderTable, "Total Orders", data.get("totalOrders").toString());
			addDashboardStatCell(orderTable, "Pending (GCash)", data.get("pendingVerification").toString());
			addDashboardStatCell(orderTable, "Pending (COD)", data.get("pending").toString());
			addDashboardStatCell(orderTable, "Processing", data.get("processing").toString());

			addDashboardStatCell(orderTable, "Out For Delivery", data.get("outForDelivery").toString());
			addDashboardStatCell(orderTable, "Delivered", data.get("delivered").toString());
			addDashboardStatCell(orderTable, "Cancelled", data.get("cancelled").toString());
			addDashboardStatCell(orderTable, "Rejected", data.get("rejected").toString());

			document.add(orderTable);

			addSectionHeader(document, "Inventory & Products");
			PdfPTable invTable = new PdfPTable(4);
			invTable.setWidthPercentage(100);

			addDashboardStatCell(invTable, "Total Items", data.get("totalInventoryItems").toString());
			addDashboardStatCell(invTable, "Total Value", formatCurrency((BigDecimal) data.get("totalStockValue")));
			addDashboardStatCell(invTable, "Low Stock Items", data.get("invLow").toString());
			addDashboardStatCell(invTable, "Out of Stock Items", data.get("invOut").toString());

			addDashboardStatCell(invTable, "Total Products", data.get("totalProducts").toString());
			addDashboardStatCell(invTable, "Low Products", data.get("prodLow").toString());
			addDashboardStatCell(invTable, "Critical Products", data.get("prodCritical").toString());
			addDashboardStatCell(invTable, "Out Products", data.get("prodOut").toString());

			document.add(invTable);

			addSectionHeader(document, "Waste & Spoilage");
			PdfPTable wasteTable = new PdfPTable(2);
			wasteTable.setWidthPercentage(100);

			addDashboardRow(wasteTable, "Total Waste Value", formatCurrency((BigDecimal) data.get("wasteTotalValue")));
			addDashboardRow(wasteTable, "Expired Value", formatCurrency((BigDecimal) data.get("wasteExpired")));
			addDashboardRow(wasteTable, "Damaged Value", formatCurrency((BigDecimal) data.get("wasteDamaged")));
			addDashboardRow(wasteTable, "Other Waste Value", formatCurrency((BigDecimal) data.get("wasteOther")));

			document.add(wasteTable);

			addSectionHeader(document, "User Summary");
			PdfPTable userTable = new PdfPTable(4);
			userTable.setWidthPercentage(100);

			addDashboardStatCell(userTable, "Total Customers", data.get("totalCustomers").toString());
			addDashboardStatCell(userTable, "Active Customers", data.get("activeCustomers").toString());
			addDashboardStatCell(userTable, "New This Month", data.get("newCustomers").toString());
			addDashboardStatCell(userTable, "Admin Accounts", data.get("totalAdmins").toString());

			document.add(userTable);

		} catch (DocumentException e) {
			throw new IOException("Error creating PDF document", e);
		}

		return new ByteArrayInputStream(out.toByteArray());
	}

	private void addSectionHeader(Document document, String title) throws DocumentException {
		Paragraph p = new Paragraph(title, FONT_SECTION_HEADER);
		p.setSpacingBefore(15f);
		p.setSpacingAfter(5f);
		document.add(p);
	}

	private void addDashboardRow(PdfPTable table, String label, String value) {
		PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_TABLE_CELL));
		labelCell.setBackgroundColor(new Color(240, 240, 240));
		labelCell.setPadding(5);
		table.addCell(labelCell);

		PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_BOLD_CELL));
		valueCell.setPadding(5);
		valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		table.addCell(valueCell);
	}

	private void addDashboardStatCell(PdfPTable table, String label, String value) {
		PdfPCell cell = new PdfPCell();
		cell.setPadding(8);
		cell.addElement(new Phrase(label, FONT_TABLE_CELL));
		cell.addElement(new Phrase(value, FONT_BOLD_CELL));
		table.addCell(cell);
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

	private void createCurrencyCell(PdfPTable table, BigDecimal value, Font font) {
		PdfPCell cell = new PdfPCell(new Phrase(formatCurrency(value), font));
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cell.setVerticalAlignment(Element.ALIGN_TOP);
		cell.setPadding(4);
		cell.setBorderWidth(0.5f);
		cell.setBorderColor(Color.LIGHT_GRAY);
		table.addCell(cell);
	}

	private String formatCurrency(BigDecimal value) {
		return "P " + String.format("%,.2f", value);
	}
}