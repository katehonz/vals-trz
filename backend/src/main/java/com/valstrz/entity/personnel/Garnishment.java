package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Запор на служител.
 */
@Document("garnishments")
public class Garnishment extends BaseEntity {

    private String employeeId;
    private GarnishmentType type;
    private String description;        // напр. номер на дело
    private String creditorName;       // взискател
    private String bailiffName;        // ЧСИ / Публичен изпълнител
    
    private BigDecimal totalAmount;    // обща сума на дълга
    private BigDecimal paidAmount;     // вече удържана сума
    private BigDecimal monthlyAmount;  // фиксирана месечна сума (при издръжка)
    
    private int priority;              // приоритет (по-малко число = по-висок приоритет)
    private LocalDate startDate;
    private LocalDate endDate;
    
    private boolean active;
    private boolean hasChildren;       // важно за определяне на несеквестируемата част по ГПК

    public Garnishment() {
        this.active = true;
        this.paidAmount = BigDecimal.ZERO;
    }

    public enum GarnishmentType {
        CHSI,        // частен съдия изпълнител
        PUBLIC,      // публичен изпълнител (държавни вземания)
        ALIMONY      // издръжка
    }

    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public GarnishmentType getType() { return type; }
    public void setType(GarnishmentType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreditorName() { return creditorName; }
    public void setCreditorName(String creditorName) { this.creditorName = creditorName; }

    public String getBailiffName() { return bailiffName; }
    public void setBailiffName(String bailiffName) { this.bailiffName = bailiffName; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getMonthlyAmount() { return monthlyAmount; }
    public void setMonthlyAmount(BigDecimal monthlyAmount) { this.monthlyAmount = monthlyAmount; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isHasChildren() { return hasChildren; }
    public void setHasChildren(boolean hasChildren) { this.hasChildren = hasChildren; }
    
    public BigDecimal getRemainingAmount() {
        if (totalAmount == null) return null;
        return totalAmount.subtract(paidAmount);
    }
}
