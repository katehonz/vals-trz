package com.valstrz.entity.payroll;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * Месечна ведомост - жив обект, проследяващ статуса на заплатите
 * за конкретен месец. Когато се затвори, се създават snapshot-и.
 */
@Document("payrolls")
public class Payroll extends BaseEntity {

    private int year;
    private int month;
    private String status;              // OPEN, CALCULATED, CLOSED
    private LocalDateTime calculatedAt;
    private LocalDateTime closedAt;
    private String closedBy;
    private int employeeCount;
    private java.math.BigDecimal totalGross;
    private java.math.BigDecimal totalNet;
    private java.math.BigDecimal totalEmployerCost;

    public Payroll() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }

    public java.math.BigDecimal getTotalGross() { return totalGross; }
    public void setTotalGross(java.math.BigDecimal totalGross) { this.totalGross = totalGross; }

    public java.math.BigDecimal getTotalNet() { return totalNet; }
    public void setTotalNet(java.math.BigDecimal totalNet) { this.totalNet = totalNet; }

    public java.math.BigDecimal getTotalEmployerCost() { return totalEmployerCost; }
    public void setTotalEmployerCost(java.math.BigDecimal totalEmployerCost) { this.totalEmployerCost = totalEmployerCost; }
}
