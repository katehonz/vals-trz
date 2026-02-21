package com.valstrz.entity.payroll;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Счетоводна контировка - осчетоводяване на заплати.
 * Генерира се при затваряне на месец.
 */
@Document("accountingEntries")
public class AccountingEntry extends BaseEntity {

    private int year;
    private int month;
    private String accountCode;     // сметка (напр. "604", "421", "461")
    private String accountName;     // наименование на сметката
    private BigDecimal debit;       // дебит
    private BigDecimal credit;      // кредит
    private String description;     // описание на контировката
    private String category;        // SALARY, INSURANCE_EMPLOYEE, INSURANCE_EMPLOYER, TAX, NET_PAY

    public AccountingEntry() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
