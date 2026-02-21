package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Индивидуално перо за възнаграждение на служител.
 * Свързва PayItem с конкретен служител и задава стойност/период.
 */
@Document("employeePayItems")
public class EmployeePayItem extends BaseEntity {

    private String employeeId;
    private String payItemCode;        // код на перото (от PayItem)
    private String payItemName;        // наименование (копирано за удобство)
    private String type;               // PERCENT, PER_UNIT, FIXED
    private BigDecimal value;          // стойност (процент или сума)
    private int fromYear;              // от година
    private int fromMonth;             // от месец
    private int toYear;                // до година (0 = безсрочно)
    private int toMonth;               // до месец (0 = безсрочно)
    private boolean active;

    public EmployeePayItem() { this.active = true; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getPayItemCode() { return payItemCode; }
    public void setPayItemCode(String payItemCode) { this.payItemCode = payItemCode; }

    public String getPayItemName() { return payItemName; }
    public void setPayItemName(String payItemName) { this.payItemName = payItemName; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

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

    /**
     * Проверява дали перото е валидно за дадения месец/година.
     */
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
