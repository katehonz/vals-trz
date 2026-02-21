package com.valstrz.service;

import com.valstrz.entity.payroll.AccountingEntry;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.repository.AccountingEntryRepository;
import com.valstrz.util.MoneyUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Счетоводни операции - осчетоводяване на заплати.
 * Генерира стандартни контировки по БГ сметкоплан:
 *
 * Дт 604 "Разходи за заплати"           / Кт 421 "Персонал"            (брутни заплати)
 * Дт 604 "Разходи за заплати"           / Кт 461 "Разчети с НАП - ДОО" (осиг. работодател)
 * Дт 421 "Персонал"                     / Кт 461 "Разчети с НАП - ДОО" (осиг. работник)
 * Дт 421 "Персонал"                     / Кт 454 "Разчети ДДФЛ"        (ДОД)
 * Дт 421 "Персонал"                     / Кт 501 "Каса / 503 Банка"    (нето за изплащане)
 */
@Service
public class AccountingService {

    private final AccountingEntryRepository repository;
    private final PayrollService payrollService;

    public AccountingService(AccountingEntryRepository repository, PayrollService payrollService) {
        this.repository = repository;
        this.payrollService = payrollService;
    }

    public List<AccountingEntry> getEntries(String tenantId, int year, int month) {
        return StreamSupport.stream(
                repository.findByTenantIdAndYearAndMonth(tenantId, year, month).spliterator(), false).toList();
    }

    /**
     * Генерира счетоводни контировки от ведомостта за месеца.
     */
    public List<AccountingEntry> generateEntries(String tenantId, int year, int month) {
        // Изтрий стари, ако има
        List<AccountingEntry> existing = getEntries(tenantId, year, month);
        for (AccountingEntry e : existing) {
            repository.deleteById(e.getId());
        }

        List<PayrollSnapshot> snapshots = payrollService.getPayrollSnapshots(tenantId, year, month);
        if (snapshots.isEmpty()) return List.of();

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalEmployeeIns = BigDecimal.ZERO;
        BigDecimal totalEmployerIns = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (PayrollSnapshot s : snapshots) {
            totalGross = MoneyUtil.add(totalGross, s.getGrossSalary());
            totalEmployeeIns = MoneyUtil.add(totalEmployeeIns, s.getTotalEmployeeInsurance());
            totalEmployerIns = MoneyUtil.add(totalEmployerIns, s.getTotalEmployerInsurance());
            totalTax = MoneyUtil.add(totalTax, s.getIncomeTax());
            totalNet = MoneyUtil.add(totalNet, s.getNetSalary());
        }

        List<AccountingEntry> entries = new ArrayList<>();

        // 1. Брутни заплати: Дт 604 / Кт 421
        entries.add(entry(tenantId, year, month, "604", "Разходи за заплати",
                MoneyUtil.round(totalGross), null, "Начислени брутни заплати", "SALARY"));
        entries.add(entry(tenantId, year, month, "421", "Персонал - възнаграждения",
                null, MoneyUtil.round(totalGross), "Начислени брутни заплати", "SALARY"));

        // 2. Осигуровки работодател: Дт 604 / Кт 461
        entries.add(entry(tenantId, year, month, "604", "Разходи за заплати",
                MoneyUtil.round(totalEmployerIns), null, "Осигуровки за сметка на работодателя", "INSURANCE_EMPLOYER"));
        entries.add(entry(tenantId, year, month, "461", "Разчети с НАП - ДОО/ДЗПО/ЗО",
                null, MoneyUtil.round(totalEmployerIns), "Осигуровки за сметка на работодателя", "INSURANCE_EMPLOYER"));

        // 3. Осигуровки работник: Дт 421 / Кт 461
        entries.add(entry(tenantId, year, month, "421", "Персонал - възнаграждения",
                MoneyUtil.round(totalEmployeeIns), null, "Осигуровки за сметка на работника", "INSURANCE_EMPLOYEE"));
        entries.add(entry(tenantId, year, month, "461", "Разчети с НАП - ДОО/ДЗПО/ЗО",
                null, MoneyUtil.round(totalEmployeeIns), "Осигуровки за сметка на работника", "INSURANCE_EMPLOYEE"));

        // 4. ДОД: Дт 421 / Кт 454
        entries.add(entry(tenantId, year, month, "421", "Персонал - възнаграждения",
                MoneyUtil.round(totalTax), null, "Данък общ доход", "TAX"));
        entries.add(entry(tenantId, year, month, "454", "Разчети за ДДФЛ",
                null, MoneyUtil.round(totalTax), "Данък общ доход", "TAX"));

        // 5. Нето: Дт 421 / Кт 501
        entries.add(entry(tenantId, year, month, "421", "Персонал - възнаграждения",
                MoneyUtil.round(totalNet), null, "Нетни заплати за изплащане", "NET_PAY"));
        entries.add(entry(tenantId, year, month, "501", "Каса / Разплащателна сметка",
                null, MoneyUtil.round(totalNet), "Нетни заплати за изплащане", "NET_PAY"));

        return entries.stream().map(repository::save).toList();
    }

    private AccountingEntry entry(String tenantId, int year, int month,
                                   String accountCode, String accountName,
                                   BigDecimal debit, BigDecimal credit,
                                   String description, String category) {
        AccountingEntry e = new AccountingEntry();
        e.setTenantId(tenantId);
        e.setYear(year);
        e.setMonth(month);
        e.setAccountCode(accountCode);
        e.setAccountName(accountName);
        e.setDebit(debit);
        e.setCredit(credit);
        e.setDescription(description);
        e.setCategory(category);
        return e;
    }
}
