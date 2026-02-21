package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.time.LocalDate;
import java.util.Map;

/**
 * Допълнително споразумение (ДС) към трудов договор.
 * Пази snapshot на променените условия преди ДС-то.
 */
@Document("amendments")
public class Amendment extends BaseEntity {

    private String employeeId;
    private String employmentId;

    private String number;                // номер на ДС
    private LocalDate date;               // дата на ДС
    private LocalDate effectiveDate;      // дата на влизане в сила
    private String basis;                 // основание (код)
    private LocalDate endDate;            // до дата (ако е приложимо)
    private String specificText;

    // Snapshot на предишните стойности (преди промяната)
    private Map<String, Object> previousValues;

    // Snapshot на новите стойности (какво се променя)
    private Map<String, Object> newValues;

    public Amendment() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmploymentId() { return employmentId; }
    public void setEmploymentId(String employmentId) { this.employmentId = employmentId; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getSpecificText() { return specificText; }
    public void setSpecificText(String specificText) { this.specificText = specificText; }

    public Map<String, Object> getPreviousValues() { return previousValues; }
    public void setPreviousValues(Map<String, Object> previousValues) { this.previousValues = previousValues; }

    public Map<String, Object> getNewValues() { return newValues; }
    public void setNewValues(Map<String, Object> newValues) { this.newValues = newValues; }
}
