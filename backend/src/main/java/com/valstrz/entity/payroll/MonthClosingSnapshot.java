package com.valstrz.entity.payroll;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Snapshot на цялата ведомост при затваряне на месец.
 *
 * Когато се затвори месецът, тук се запазват ВСИЧКИ параметри на ниво фирма,
 * които са били в сила. Индивидуалните snapshot-и на служителите са в PayrollSnapshot.
 *
 * Комбинацията MonthClosingSnapshot + PayrollSnapshot[] = пълна одитна следа.
 * Можеш да реконструираш цялата ведомост за всеки минал месец,
 * без значение какви промени е имало след това.
 */
@Document("monthClosingSnapshots")
public class MonthClosingSnapshot extends BaseEntity {

    private int year;
    private int month;
    private LocalDateTime closedAt;
    private String closedBy;              // потребител затворил месеца

    // Пълен snapshot на фирмените настройки към момента на затваряне
    private Map<String, Object> companyData;

    // Законодателни параметри
    private Map<String, Object> insuranceRates;        // МРЗ, макс. доход, данък
    private Map<String, Object> insuranceContributions; // ДОО, ДЗПО, ЗО проценти
    private Map<String, Object> insuranceThresholds;    // мин. осиг. прагове

    // Работен календар
    private Map<String, Object> calendarData;           // работни дни, часове, схеми

    // Пера (какви пера е имало и какви са техните настройки)
    private Map<String, Object> payItemsConfig;
    private Map<String, Object> deductionItemsConfig;

    // Обобщени резултати
    private int employeeCount;            // брой обработени служители
    private Map<String, Object> totals;   // общи суми по пера

    public MonthClosingSnapshot() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public Map<String, Object> getCompanyData() { return companyData; }
    public void setCompanyData(Map<String, Object> companyData) { this.companyData = companyData; }

    public Map<String, Object> getInsuranceRates() { return insuranceRates; }
    public void setInsuranceRates(Map<String, Object> insuranceRates) { this.insuranceRates = insuranceRates; }

    public Map<String, Object> getInsuranceContributions() { return insuranceContributions; }
    public void setInsuranceContributions(Map<String, Object> insuranceContributions) { this.insuranceContributions = insuranceContributions; }

    public Map<String, Object> getInsuranceThresholds() { return insuranceThresholds; }
    public void setInsuranceThresholds(Map<String, Object> insuranceThresholds) { this.insuranceThresholds = insuranceThresholds; }

    public Map<String, Object> getCalendarData() { return calendarData; }
    public void setCalendarData(Map<String, Object> calendarData) { this.calendarData = calendarData; }

    public Map<String, Object> getPayItemsConfig() { return payItemsConfig; }
    public void setPayItemsConfig(Map<String, Object> payItemsConfig) { this.payItemsConfig = payItemsConfig; }

    public Map<String, Object> getDeductionItemsConfig() { return deductionItemsConfig; }
    public void setDeductionItemsConfig(Map<String, Object> deductionItemsConfig) { this.deductionItemsConfig = deductionItemsConfig; }

    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }

    public Map<String, Object> getTotals() { return totals; }
    public void setTotals(Map<String, Object> totals) { this.totals = totals; }
}
