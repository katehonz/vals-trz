package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Индивидуална удръжка за служител.
 * Свързва DeductionItem с конкретен служител и задава сума/период.
 */
@Document("employeeDeductions")
public class EmployeeDeduction extends BaseEntity {

    private String employeeId;
    private String deductionCode;      // код на перото (от DeductionItem)
    private String deductionName;      // наименование
    private BigDecimal amount;         // сума за удръжка
    private int fromYear;
    private int fromMonth;
    private int toYear;                // 0 = безсрочно
    private int toMonth;               // 0 = безсрочно
    private boolean active;

    public EmployeeDeduction() { this.active = true; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDeductionCode() { return deductionCode; }
    public void setDeductionCode(String deductionCode) { this.deductionCode = deductionCode; }

    public String getDeductionName() { return deductionName; }
    public void setDeductionName(String deductionName) { this.deductionName = deductionName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public int getFromYear() { return fromYear; }
    public void setFromYear(int fromYear) { this.fromYear = fromYear; }

    public int getFromMonth() { return fromMonth; }
    public void setFromMonth(int fromMonth) { this.fromMonth = fromMonth; }

    public int getToYear() { return toYear; }
    public void setToYear(int toYear) { this.toYear = toYear; }

    public int getToMonth() { return toMonth; }
    public void setToMonth(int toMonth) { this.toMonth = toMonth; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isValidFor(int year, int month) {
        if (!active) return false;
        int from = fromYear * 100 + fromMonth;
        int current = year * 100 + month;
        if (current < from) return false;
        if (toYear > 0 && toMonth > 0) {
            int to = toYear * 100 + toMonth;
            return current <= to;
        }
        return true;
    }
}
