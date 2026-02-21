package com.valstrz.controller;

import com.valstrz.entity.payroll.AccountingEntry;
import com.valstrz.service.AccountingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/accounting")
public class AccountingController {

    private final AccountingService accountingService;

    public AccountingController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    @GetMapping("/entries")
    public List<AccountingEntry> getEntries(@PathVariable String tenantId,
                                             @RequestParam int year,
                                             @RequestParam int month) {
        return accountingService.getEntries(tenantId, year, month);
    }

    @PostMapping("/generate")
    public List<AccountingEntry> generateEntries(@PathVariable String tenantId,
                                                   @RequestParam int year,
                                                   @RequestParam int month) {
        return accountingService.generateEntries(tenantId, year, month);
    }
}
