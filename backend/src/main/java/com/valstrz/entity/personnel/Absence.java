package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.time.LocalDate;
import java.util.Map;

/**
 * Отсъствие на служител (отпуск, болничен, майчинство, самоотлъчка).
 *
 * За болнични: допълнителни данни в sickLeaveData (Декларация 15,
 * дни за сметка на работодателя / НОИ).
 */
@Document("absences")
public class Absence extends BaseEntity {

    private String employeeId;

    private String type;               // код на вида отсъствие (351301, 351631...)
    private String typeName;           // наименование за четимост
    private LocalDate fromDate;
    private LocalDate toDate;
    private int workingDays;           // работни дни в периода
    private int calendarDays;          // календарни дни
    private int employerDays;          // работни дни за сметка на работодателя (първите 2)
    private int nssiDays;              // работни дни за сметка на НОИ
    private String status;             // REQUESTED, APPROVED, ACTIVE, COMPLETED

    // За болнични - допълнителни данни (Декларация 15, брой дни работодател/НОИ)
    private Map<String, Object> sickLeaveData;

    // За отпуски - номер и дата на заповед
    private String orderNumber;
    private LocalDate orderDate;

    // Бележки
    private String notes;

    public Absence() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public int getWorkingDays() { return workingDays; }
    public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }

    public int getCalendarDays() { return calendarDays; }
    public void setCalendarDays(int calendarDays) { this.calendarDays = calendarDays; }

    public int getEmployerDays() { return employerDays; }
    public void setEmployerDays(int employerDays) { this.employerDays = employerDays; }

    public int getNssiDays() { return nssiDays; }
    public void setNssiDays(int nssiDays) { this.nssiDays = nssiDays; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getSickLeaveData() { return sickLeaveData; }
    public void setSickLeaveData(Map<String, Object> sickLeaveData) { this.sickLeaveData = sickLeaveData; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
