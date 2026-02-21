package com.valstrz.entity.calendar;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Времеви данни за конкретен месец - работен календар.
 * Съдържа брой работни дни, часове, дати на изплащане.
 */
@Document("monthlyCalendars")
public class MonthlyCalendar extends BaseEntity {

    private int year;
    private int month;
    private int workingDays;             // брой работни дни в месеца
    private int calendarDays;            // брой календарни дни
    private int holidays;                // почивни и празнични дни
    private BigDecimal workingHoursPerDay; // работни часове на ден за фирмата (напр. 8)
    private BigDecimal totalWorkingHours;  // общо работни часове в месеца

    private LocalDate advancePaymentDate;  // дата на аванс
    private LocalDate salaryPaymentDate;   // дата на заплата

    // Работни дни в предходни месеци (за справки)
    private int[] previousMonthsWorkingDays; // 12 елемента за предходните месеци

    public MonthlyCalendar() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getWorkingDays() { return workingDays; }
    public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }

    public int getCalendarDays() { return calendarDays; }
    public void setCalendarDays(int calendarDays) { this.calendarDays = calendarDays; }

    public int getHolidays() { return holidays; }
    public void setHolidays(int holidays) { this.holidays = holidays; }

    public BigDecimal getWorkingHoursPerDay() { return workingHoursPerDay; }
    public void setWorkingHoursPerDay(BigDecimal workingHoursPerDay) { this.workingHoursPerDay = workingHoursPerDay; }

    public BigDecimal getTotalWorkingHours() { return totalWorkingHours; }
    public void setTotalWorkingHours(BigDecimal totalWorkingHours) { this.totalWorkingHours = totalWorkingHours; }

    public LocalDate getAdvancePaymentDate() { return advancePaymentDate; }
    public void setAdvancePaymentDate(LocalDate advancePaymentDate) { this.advancePaymentDate = advancePaymentDate; }

    public LocalDate getSalaryPaymentDate() { return salaryPaymentDate; }
    public void setSalaryPaymentDate(LocalDate salaryPaymentDate) { this.salaryPaymentDate = salaryPaymentDate; }

    public int[] getPreviousMonthsWorkingDays() { return previousMonthsWorkingDays; }
    public void setPreviousMonthsWorkingDays(int[] previousMonthsWorkingDays) { this.previousMonthsWorkingDays = previousMonthsWorkingDays; }
}
