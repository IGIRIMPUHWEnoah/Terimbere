package com.terimbere.budget.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.terimbere.budget.model.Budget;
import com.terimbere.budget.model.BudgetEntry;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ReportService {

    private final BudgetService budgetService;

    public ReportService(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    public byte[] generateBudgetExcelReport(UUID budgetId) throws IOException {
        Budget budget = budgetService.getBudgetById(budgetId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Budget Summary");

            // Define styles
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Title Row
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("Terimbere Budget Book: " + budget.getName());
            titleRow.getCell(0).setCellStyle(headerStyle);

            Row infoRow = sheet.createRow(1);
            infoRow.createCell(0).setCellValue("Period: " + budget.getStartDate() + " to " + budget.getEndDate());

            // Header Row
            Row headerRow = sheet.createRow(3);
            String[] headers = {"Category", "Type", "Planned Amount", "Actual Amount", "Difference"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 4;
            for (BudgetEntry entry : budget.getEntries()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(entry.getCategory());
                row.createCell(1).setCellValue(entry.getEntryType());
                row.createCell(2).setCellValue(entry.getPlannedAmount().doubleValue());
                row.createCell(3).setCellValue(entry.getActualAmount().doubleValue());

                // Difference planned - actual (for Expense) or actual - planned (for Income)
                double diff = 0.0;
                if (entry.getEntryType().equalsIgnoreCase("INCOME")) {
                    diff = entry.getActualAmount().subtract(entry.getPlannedAmount()).doubleValue();
                } else {
                    diff = entry.getPlannedAmount().subtract(entry.getActualAmount()).doubleValue();
                }
                row.createCell(4).setCellValue(diff);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    public byte[] generateBudgetPdfReport(UUID budgetId) {
        Budget budget = budgetService.getBudgetById(budgetId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Document Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font tableCellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

            // Document Header
            Paragraph title = new Paragraph("TERIMBERE BUDGET SUMMARY", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph budgetName = new Paragraph("Budget Book: " + budget.getName(), sectionFont);
            budgetName.setAlignment(Element.ALIGN_CENTER);
            budgetName.setSpacingBefore(10);
            document.add(budgetName);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            Paragraph meta = new Paragraph(
                    String.format("Generated on: %s | Date Range: %s to %s | Period: %s",
                            LocalDate.now().format(formatter),
                            budget.getStartDate().format(formatter),
                            budget.getEndDate().format(formatter),
                            budget.getPeriodType()),
                    metaFont
            );
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(20);
            document.add(meta);

            // Add line separator
            document.add(new Paragraph("----------------------------------------------------------------------------------------------------------------------------------", metaFont));

            // Summary Stats
            BigDecimal totalPlannedIncome = BigDecimal.ZERO;
            BigDecimal totalActualIncome = BigDecimal.ZERO;
            BigDecimal totalPlannedExpense = BigDecimal.ZERO;
            BigDecimal totalActualExpense = BigDecimal.ZERO;

            for (BudgetEntry entry : budget.getEntries()) {
                if (entry.getEntryType().equalsIgnoreCase("INCOME")) {
                    totalPlannedIncome = totalPlannedIncome.add(entry.getPlannedAmount());
                    totalActualIncome = totalActualIncome.add(entry.getActualAmount());
                } else {
                    totalPlannedExpense = totalPlannedExpense.add(entry.getPlannedAmount());
                    totalActualExpense = totalActualExpense.add(entry.getActualAmount());
                }
            }

            Paragraph statsHeader = new Paragraph("Financial Runway Analysis", sectionFont);
            statsHeader.setSpacingBefore(10);
            statsHeader.setSpacingAfter(10);
            document.add(statsHeader);

            PdfPTable statsTable = new PdfPTable(3);
            statsTable.setWidthPercentage(100);
            
            // Header for stats
            addStatsRow(statsTable, "Metric", "Planned Amount", "Actual Realized", true, tableHeaderFont);
            addStatsRow(statsTable, "Total Inflow (Income)", totalPlannedIncome.toString(), totalActualIncome.toString(), false, tableCellFont);
            addStatsRow(statsTable, "Total Outflow (Expense)", totalPlannedExpense.toString(), totalActualExpense.toString(), false, tableCellFont);
            
            BigDecimal plannedSurplus = totalPlannedIncome.subtract(totalPlannedExpense);
            BigDecimal actualSurplus = totalActualIncome.subtract(totalActualExpense);
            addStatsRow(statsTable, "Net Runway Surplus/Deficit", plannedSurplus.toString(), actualSurplus.toString(), false, tableCellFont);
            
            document.add(statsTable);

            // Detailed Table
            Paragraph detailedHeader = new Paragraph("Itemized Budget Allocations", sectionFont);
            detailedHeader.setSpacingBefore(20);
            detailedHeader.setSpacingAfter(10);
            document.add(detailedHeader);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{3, 2, 2, 2, 2});

            // Set Headers
            PdfPCell h1 = new PdfPCell(new Phrase("Category", tableHeaderFont));
            h1.setBackgroundColor(Color.DARK_GRAY);
            table.addCell(h1);

            PdfPCell h2 = new PdfPCell(new Phrase("Type", tableHeaderFont));
            h2.setBackgroundColor(Color.DARK_GRAY);
            table.addCell(h2);

            PdfPCell h3 = new PdfPCell(new Phrase("Planned", tableHeaderFont));
            h3.setBackgroundColor(Color.DARK_GRAY);
            table.addCell(h3);

            PdfPCell h4 = new PdfPCell(new Phrase("Actual", tableHeaderFont));
            h4.setBackgroundColor(Color.DARK_GRAY);
            table.addCell(h4);

            PdfPCell h5 = new PdfPCell(new Phrase("Date", tableHeaderFont));
            h5.setBackgroundColor(Color.DARK_GRAY);
            table.addCell(h5);

            for (BudgetEntry entry : budget.getEntries()) {
                table.addCell(new PdfPCell(new Phrase(entry.getCategory(), tableCellFont)));
                table.addCell(new PdfPCell(new Phrase(entry.getEntryType(), tableCellFont)));
                table.addCell(new PdfPCell(new Phrase(entry.getPlannedAmount().toString(), tableCellFont)));
                table.addCell(new PdfPCell(new Phrase(entry.getActualAmount().toString(), tableCellFont)));
                table.addCell(new PdfPCell(new Phrase(entry.getEntryDate().format(formatter), tableCellFont)));
            }

            document.add(table);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Error occurred while generating PDF", e);
        }

        return baos.toByteArray();
    }

    private void addStatsRow(PdfPTable table, String metric, String planned, String actual, boolean isHeader, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(metric, font));
        PdfPCell c2 = new PdfPCell(new Phrase(planned, font));
        PdfPCell c3 = new PdfPCell(new Phrase(actual, font));
        
        if (isHeader) {
            c1.setBackgroundColor(Color.GRAY);
            c2.setBackgroundColor(Color.GRAY);
            c3.setBackgroundColor(Color.GRAY);
        }
        
        table.addCell(c1);
        table.addCell(c2);
        table.addCell(c3);
    }
}
