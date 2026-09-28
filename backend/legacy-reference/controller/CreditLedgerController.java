package com.skillswap.controller;

import com.skillswap.entity.CreditLedger;
import com.skillswap.service.CreditLedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credits")
@CrossOrigin(origins = "*")
public class CreditLedgerController {

    private final CreditLedgerService creditLedgerService;

    public CreditLedgerController(
            CreditLedgerService creditLedgerService) {

        this.creditLedgerService = creditLedgerService;
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<CreditLedger>>
    getMemberHistory(
            @PathVariable Long memberId) {

        return ResponseEntity.ok(
                creditLedgerService
                        .getMemberHistory(memberId)
        );
    }
}