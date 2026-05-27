package com.terimbere.budget.controller;

import com.terimbere.budget.model.Contract;
import com.terimbere.budget.service.ContractStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/contracts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Contract Upload Center", description = "Endpoints for dragging-and-dropping signed agreements, binding them to contacts/debts, and downloading files.")
public class ContractController {

    private final ContractStorageService contractStorageService;

    public ContractController(ContractStorageService contractStorageService) {
        this.contractStorageService = contractStorageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a signed physical contract file", description = "Accepts file upload (PDF/PNG/JPEG) and binds it to a specific contact and optionally a debt record.")
    public ResponseEntity<Contract> uploadContractFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("contactId") UUID contactId,
            @RequestParam(value = "debtRecordId", required = false) UUID debtRecordId,
            @RequestParam("title") String title,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "notes", required = false) String notes) {

        Contract contract = contractStorageService.storeContractFile(file, contactId, debtRecordId, title, startDate, endDate, notes);
        return new ResponseEntity<>(contract, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all uploaded contracts metadata")
    public ResponseEntity<List<Contract>> getAllContracts() {
        return ResponseEntity.ok(contractStorageService.getAllContractsForCurrentUser());
    }

    @GetMapping("/{contractId}")
    @Operation(summary = "Get contract details by ID")
    public ResponseEntity<Contract> getContractById(@PathVariable UUID contractId) {
        return ResponseEntity.ok(contractStorageService.getContractById(contractId));
    }

    @GetMapping("/{contractId}/download")
    @Operation(summary = "Download/stream the actual contract file payload")
    public ResponseEntity<Resource> downloadContractFile(@PathVariable UUID contractId) {
        Contract contract = contractStorageService.getContractById(contractId);
        Resource resource = contractStorageService.loadContractFileAsResource(contractId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contract.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{contractId}")
    @Operation(summary = "Delete an uploaded contract and its storage file")
    public ResponseEntity<Void> deleteContract(@PathVariable UUID contractId) {
        contractStorageService.deleteContract(contractId);
        return ResponseEntity.noContent().build();
    }
}
