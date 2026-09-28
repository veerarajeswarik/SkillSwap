package com.example.backend.controller;

import com.example.backend.entity.CreditLedger;
import com.example.backend.service.CreditLedgerService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@CrossOrigin(origins = "*") // DEV ONLY
public class CreditLedgerController {

    private final CreditLedgerService creditLedgerService;

    public CreditLedgerController(CreditLedgerService creditLedgerService) {
        this.creditLedgerService = creditLedgerService;
    }

    // GET /api/credits/member/{memberId}  -> transaction history, newest first
    @GetMapping("/member/{memberId}")
    public List<CreditLedger> getHistory(@PathVariable Long memberId) {
        return creditLedgerService.getHistory(memberId);
    }
}
