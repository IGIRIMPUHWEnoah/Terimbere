package com.terimbere.budget.controller;

import com.terimbere.budget.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Downloads / Reports Engine", description = "Endpoints for downloading beautifully formatted PDF and Excel summaries of user budget data.")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/budgets/{budgetId}/excel")
    @Operation(summary = "Export a budget summary as a Microsoft Excel sheet")
    public ResponseEntity<byte[]> downloadBudgetExcel(@PathVariable UUID budgetId) throws IOException {
        byte[] data = reportService.generateBudgetExcelReport(budgetId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"budget_report_" + budgetId + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/budgets/{budgetId}/pdf")
    @Operation(summary = "Export a budget summary as a styled PDF document")
    public ResponseEntity<byte[]> downloadBudgetPdf(@PathVariable UUID budgetId) {
        byte[] data = reportService.generateBudgetPdfReport(budgetId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"budget_report_" + budgetId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
