package com.demo.example.mstransfercontrol.service.impl;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import com.demo.example.mstransfercontrol.entity.Account;
import com.demo.example.mstransfercontrol.entity.Transfer;
import com.demo.example.mstransfercontrol.mapper.TransferMapper;
import com.demo.example.mstransfercontrol.mapper.impl.TransferMapperImpl;
import com.demo.example.mstransfercontrol.repository.AccountRepository;
import com.demo.example.mstransfercontrol.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    private static final String SOURCE_ACCOUNT = "00123456789";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Spy
    private TransferMapper transferMapper = new TransferMapperImpl();

    @InjectMocks
    private TransferServiceImpl transferService;

    @Captor
    private ArgumentCaptor<Transfer> transferCaptor;

    @Test
    void shouldRegisterTransferWhenAllBusinessRulesAreMet() {
        // Arrange
        TransferRequest request = request("25.50");
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Optional.of(activeAccount()));
        when(transferRepository.totalTransferredToday(eq(SOURCE_ACCOUNT), any(), any()))
                .thenReturn(new BigDecimal("9974.50"));

        // Act
        TransferResponse response = transferService.validateAndTransfer(request);

        // Assert
        assertEquals("APPROVED", response.getStatus());
        assertEquals(SOURCE_ACCOUNT, response.getSourceAccount());
        assertEquals(new BigDecimal("25.50"), response.getAmount());
        verify(accountRepository).findByAccountNumber(SOURCE_ACCOUNT);
        verify(transferRepository).totalTransferredToday(eq(SOURCE_ACCOUNT), any(), any());
        verify(transferRepository).save(transferCaptor.capture());
        assertEquals(SOURCE_ACCOUNT, transferCaptor.getValue().getAccountNumber());
        assertEquals("successful", transferCaptor.getValue().getStatus());
        verify(transferMapper).toTransferEntity(request);
        verify(transferMapper).toApprovedResponse(request);
    }

    @Test
    void shouldThrowWhenSourceAccountDoesNotExist() {
        // Arrange
        TransferRequest request = request("10.00");
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT)).thenReturn(Optional.empty());

        // Act / Assert
        assertThrows(NoSuchElementException.class, () -> transferService.validateAndTransfer(request));
        verify(accountRepository).findByAccountNumber(SOURCE_ACCOUNT);
        verifyNoInteractions(transferRepository, transferMapper);
    }

    @Test
    void shouldThrowWhenSourceAccountIsNotActive() {
        // Arrange
        TransferRequest request = request("10.00");
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Optional.of(Account.builder().accountNumber(SOURCE_ACCOUNT).status("BLOCKED").build()));

        // Act / Assert
        assertThrows(IllegalStateException.class, () -> transferService.validateAndTransfer(request));
        verify(accountRepository).findByAccountNumber(SOURCE_ACCOUNT);
        verifyNoInteractions(transferRepository, transferMapper);
    }

    @ParameterizedTest
    @CsvSource(value = {"0", "-1.00", "NULL"}, nullValues = "NULL")
    void shouldRejectNullZeroOrNegativeAmounts(String amount) {
        // Arrange
        TransferRequest request = request(amount);
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Optional.of(activeAccount()));

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> transferService.validateAndTransfer(request));
        verify(accountRepository).findByAccountNumber(SOURCE_ACCOUNT);
        verifyNoInteractions(transferRepository, transferMapper);
    }

    @Test
    void shouldNotSaveTransferWhenDailyLimitIsExceeded() {
        // Arrange
        TransferRequest request = request("0.02");
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Optional.of(activeAccount()));
        when(transferRepository.totalTransferredToday(eq(SOURCE_ACCOUNT), any(), any()))
                .thenReturn(new BigDecimal("9999.99"));

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> transferService.validateAndTransfer(request));
        verify(transferRepository).totalTransferredToday(eq(SOURCE_ACCOUNT), any(), any());
        verify(transferRepository, org.mockito.Mockito.never()).save(any());
        verifyNoInteractions(transferMapper);
    }

    @Test
    void shouldAllowTransferExactlyAtDailyLimit() {
        // Arrange
        TransferRequest request = request("25.00");
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Optional.of(activeAccount()));
        when(transferRepository.totalTransferredToday(eq(SOURCE_ACCOUNT), any(), any()))
                .thenReturn(new BigDecimal("9975.00"));

        // Act
        TransferResponse response = transferService.validateAndTransfer(request);

        // Assert
        assertEquals("APPROVED", response.getStatus());
        verify(transferRepository).save(any(Transfer.class));
    }

    @Test
    void shouldReturnDailyAvailableAmount() {
        // Arrange
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Optional.of(activeAccount()));
        when(transferRepository.totalTransferredToday(eq(SOURCE_ACCOUNT), any(), any()))
                .thenReturn(new BigDecimal("3500.00"));

        // Act
        BigDecimal available = transferService.getDailyAvailableAmount(SOURCE_ACCOUNT);

        // Assert
        assertEquals(new BigDecimal("6500.00"), available);
        verify(accountRepository).findByAccountNumber(SOURCE_ACCOUNT);
        verify(transferRepository).totalTransferredToday(eq(SOURCE_ACCOUNT), any(), any());
        verifyNoInteractions(transferMapper);
    }

    @Test
    void shouldReturnZeroWhenDailyLimitWasAlreadyExceeded() {
        // Arrange
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Optional.of(activeAccount()));
        when(transferRepository.totalTransferredToday(eq(SOURCE_ACCOUNT), any(), any()))
                .thenReturn(new BigDecimal("10500.00"));

        // Act
        BigDecimal available = transferService.getDailyAvailableAmount(SOURCE_ACCOUNT);

        // Assert
        assertEquals(BigDecimal.ZERO, available);
        verifyNoInteractions(transferMapper);
    }

    @Test
    void shouldNotQueryTransfersWhenAccountForAvailableAmountDoesNotExist() {
        // Arrange
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT)).thenReturn(Optional.empty());

        // Act / Assert
        assertThrows(NoSuchElementException.class,
                () -> transferService.getDailyAvailableAmount(SOURCE_ACCOUNT));
        verifyNoInteractions(transferRepository, transferMapper);
    }

    @Test
    void shouldNotQueryTransfersWhenAccountForAvailableAmountIsInactive() {
        // Arrange
        when(accountRepository.findByAccountNumber(SOURCE_ACCOUNT))
                .thenReturn(Optional.of(Account.builder().accountNumber(SOURCE_ACCOUNT).status("BLOCKED").build()));

        // Act / Assert
        assertThrows(IllegalStateException.class,
                () -> transferService.getDailyAvailableAmount(SOURCE_ACCOUNT));
        verifyNoInteractions(transferRepository, transferMapper);
    }

    private TransferRequest request(String amount) {
        return TransferRequest.builder()
                .customerId("customer-1")
                .sourceAccount(SOURCE_ACCOUNT)
                .targetAccount("00987654321")
                .amount(amount == null ? null : new BigDecimal(amount))
                .build();
    }

    private Account activeAccount() {
        return Account.builder()
                .accountNumber(SOURCE_ACCOUNT)
                .status("ACTIVE")
                .build();
    }
}
