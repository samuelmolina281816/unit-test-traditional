package com.demo.example.mstransfercontrol.controller;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import com.demo.example.mstransfercontrol.service.TransferService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private TransferService transferService;

    @InjectMocks
    private TransferController transferController;

    @Test
    void shouldDelegateTransferRequestToServiceOnce() {
        // Arrange
        TransferRequest request = TransferRequest.builder()
                .sourceAccount("00123456789")
                .targetAccount("00987654321")
                .amount(new BigDecimal("100.00"))
                .build();
        TransferResponse expectedResponse = TransferResponse.builder().status("APPROVED").build();
        when(transferService.validateAndTransfer(request)).thenReturn(expectedResponse);

        // Act
        TransferResponse response = transferController.transfer(Map.of(), request);

        // Assert
        assertSame(expectedResponse, response);
        verify(transferService).validateAndTransfer(request);
        verifyNoMoreInteractions(transferService);
    }

    @Test
    void shouldDelegateDailyAvailableAmountQueryToServiceOnce() {
        // Arrange
        String sourceAccount = "00123456789";
        BigDecimal expectedAvailable = new BigDecimal("4500.00");
        when(transferService.getDailyAvailableAmount(sourceAccount)).thenReturn(expectedAvailable);

        // Act
        BigDecimal available = transferController.getDailyAvailableAmount(sourceAccount);

        // Assert
        assertEquals(expectedAvailable, available);
        verify(transferService).getDailyAvailableAmount(sourceAccount);
        verifyNoMoreInteractions(transferService);
    }
}
