package com.demo.example.mstransfercontrol.service;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import java.math.BigDecimal;

public interface TransferService {
    TransferResponse validateAndTransfer(TransferRequest request);
    BigDecimal getDailyAvailableAmount(String sourceAccount);
}
