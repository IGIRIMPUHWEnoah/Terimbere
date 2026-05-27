package com.terimbere.budget.service;

import com.terimbere.budget.exception.ResourceNotFoundException;
import com.terimbere.budget.model.Contact;
import com.terimbere.budget.model.Contract;
import com.terimbere.budget.model.DebtRecord;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ContractStorageService {

    private final ContractRepository contractRepository;
    private final AuthService authService;
    private final DebtService debtService;
    private final Path fileStorageLocation;

    public ContractStorageService(ContractRepository contractRepository,
                                  AuthService authService,
                                  DebtService debtService,
                                  @Value("${app.upload.dir}") String uploadDir) {
        this.contractRepository = contractRepository;
        this.authService = authService;
        this.debtService = debtService;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @Transactional
    public Contract storeContractFile(MultipartFile file,
                                      UUID contactId,
                                      UUID debtRecordId,
                                      String title,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      String notes) {
        User user = authService.getCurrentAuthenticatedUser();
        Contact contact = debtService.getContactById(contactId);
        
        DebtRecord debtRecord = null;
        if (debtRecordId != null) {
            debtRecord = debtService.getDebtRecordById(debtRecordId);
        }

        // Clean file name
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        // Generate a secure unique filename to prevent collisons
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            if (fileName.contains("..")) {
                throw new IllegalArgumentException("Filename contains invalid path sequence: " + fileName);
            }

            // Copy file to the target location (Replacing existing file if any)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Contract contract = Contract.builder()
                    .user(user)
                    .contact(contact)
                    .debtRecord(debtRecord)
                    .title(title)
                    .filePath(targetLocation.toString())
                    .fileType(file.getContentType())
                    .startDate(startDate)
                    .endDate(endDate)
                    .notes(notes)
                    .build();

            return contractRepository.save(contract);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + fileName + ". Please try again!", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<Contract> getAllContractsForCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        return contractRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public Contract getContractById(UUID contractId) {
        User user = authService.getCurrentAuthenticatedUser();
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        if (!contract.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to contract");
        }
        return contract;
    }

    @Transactional(readOnly = true)
    public Resource loadContractFileAsResource(UUID contractId) {
        Contract contract = getContractById(contractId);
        try {
            Path filePath = Paths.get(contract.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found for contract: " + contractId);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found for contract: " + contractId, ex);
        }
    }

    @Transactional
    public void deleteContract(UUID contractId) {
        Contract contract = getContractById(contractId);
        try {
            Path filePath = Paths.get(contract.getFilePath()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            // Log file deletion issue but proceed to delete database entry
        }
        contractRepository.delete(contract);
    }
}
