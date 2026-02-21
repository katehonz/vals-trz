package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.time.LocalDate;

/**
 * Заповед за прекратяване на трудово правоотношение.
 */
@Document("terminations")
public class Termination extends BaseEntity {

    private String employeeId;
    private String employmentId;

    private String orderNumber;           // номер на заповедта
    private LocalDate orderDate;          // дата на заповедта
    private LocalDate lastWorkDay;        // последен работен ден
    private String basis;                 // основание за прекратяване (код по КТ)
    private String specificText;          // специфични причини - текст

    public Termination() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmploymentId() { return employmentId; }
    public void setEmploymentId(String employmentId) { this.employmentId = employmentId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public LocalDate getLastWorkDay() { return lastWorkDay; }
    public void setLastWorkDay(LocalDate lastWorkDay) { this.lastWorkDay = lastWorkDay; }

    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }

    public String getSpecificText() { return specificText; }
    public void setSpecificText(String specificText) { this.specificText = specificText; }
}
