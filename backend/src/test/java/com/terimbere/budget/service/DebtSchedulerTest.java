package com.terimbere.budget.service;

import com.terimbere.budget.model.DebtRecord;
import com.terimbere.budget.model.User;
import com.terimbere.budget.repository.DebtInstallmentRepository;
import com.terimbere.budget.repository.DebtRecordRepository;
import com.terimbere.budget.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DebtSchedulerTest {

    @Mock
    private DebtRecordRepository debtRecordRepository;

    @Mock
    private DebtInstallmentRepository debtInstallmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskScheduler taskScheduler;

    @InjectMocks
    private DebtScheduler debtScheduler;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(UUID.randomUUID())
                .email("user1@test.com")
                .build();
        user2 = User.builder()
                .id(UUID.randomUUID())
                .email("user2@test.com")
                .build();
    }

    @Test
    void testMarkOverdueDebts() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        DebtRecord debt1 = DebtRecord.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .status("ACTIVE")
                .build();

        DebtRecord debt2 = DebtRecord.builder()
                .id(UUID.randomUUID())
                .user(user1)
                .status("ACTIVE")
                .build();

        DebtRecord debt3 = DebtRecord.builder()
                .id(UUID.randomUUID())
                .user(user2)
                .status("ACTIVE")
                .build();

        // user1 has 2 overdue debts, user2 has 1
        when(debtRecordRepository.findOverdueDebts(eq(user1), any(LocalDate.class)))
                .thenReturn(Arrays.asList(debt1, debt2));
        
        when(debtRecordRepository.findOverdueDebts(eq(user2), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(debt3));

        when(debtInstallmentRepository.findOverdueInstallments(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        debtScheduler.markOverdueDebts();

        // Assert
        assertEquals("OVERDUE", debt1.getStatus());
        assertEquals("OVERDUE", debt2.getStatus());
        assertEquals("OVERDUE", debt3.getStatus());

        // Verify that save was called for all 3 debts
        verify(debtRecordRepository, times(1)).save(debt1);
        verify(debtRecordRepository, times(1)).save(debt2);
        verify(debtRecordRepository, times(1)).save(debt3);
        verify(debtRecordRepository, times(3)).save(any(DebtRecord.class));
    }

    @Test
    void testMarkOverdueDebts_NoOverdueDebts() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user1));

        when(debtRecordRepository.findOverdueDebts(eq(user1), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        when(debtInstallmentRepository.findOverdueInstallments(any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // Act
        debtScheduler.markOverdueDebts();

        // Assert
        verify(debtRecordRepository, never()).save(any());
    }

    @Test
    void testMarkOverdueDebts_NoUsers() {
        // Arrange
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        debtScheduler.markOverdueDebts();

        // Assert
        verify(debtRecordRepository, never()).findOverdueDebts(any(), any());
        verify(debtRecordRepository, never()).save(any());
    }
}
