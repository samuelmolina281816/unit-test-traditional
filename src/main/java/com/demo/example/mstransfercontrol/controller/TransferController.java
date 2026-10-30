package com.demo.example.mstransfercontrol.controller;

import com.demo.example.mstransfercontrol.dto.TransferRequest;
import com.demo.example.mstransfercontrol.dto.TransferResponse;
import com.demo.example.mstransfercontrol.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/")
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/transfers/control")
    public TransferResponse transfer(@RequestHeader Map<String, String> headers,
                                     @RequestBody TransferRequest request) {
        return transferService.validateAndTransfer(request);
    }

    @GetMapping("/transfers/control/{sourceAccount}/daily-available")
    public BigDecimal getDailyAvailableAmount(@PathVariable String sourceAccount) {
        return transferService.getDailyAvailableAmount(sourceAccount);
    }

}
