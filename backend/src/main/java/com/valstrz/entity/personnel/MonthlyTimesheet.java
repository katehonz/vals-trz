package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * Часова месечна карта на служител.
 * Масив от дневни записи - какво е работено/отсъствано всеки ден.
 */
@Document("monthlyTimesheets")
public class MonthlyTimesheet extends BaseEntity {

    private String employeeId;
    private int year;
    private int month;

    private List<DailyEntry> days;

    // Обобщение за месеца
    private int totalWorkedDays;
    private BigDecimal totalWorkedHours;
    private BigDecimal totalOvertimeHours;
    private BigDecimal totalNightHours;
    private int totalAbsenceDays;
    private int sickLeaveDays;
    private int unpaidLeaveDays;

    public MonthlyTimesheet() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public List<DailyEntry> getDays() { return days; }
    public void setDays(List<DailyEntry> days) { this.days = days; }

    public int getTotalWorkedDays() { return totalWorkedDays; }
    public void setTotalWorkedDays(int totalWorkedDays) { this.totalWorkedDays = totalWorkedDays; }

    public BigDecimal getTotalWorkedHours() { return totalWorkedHours; }
    public void setTotalWorkedHours(BigDecimal totalWorkedHours) { this.totalWorkedHours = totalWorkedHours; }

    public BigDecimal getTotalOvertimeHours() { return totalOvertimeHours; }
    public void setTotalOvertimeHours(BigDecimal totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }

    public BigDecimal getTotalNightHours() { return totalNightHours; }
    public void setTotalNightHours(BigDecimal totalNightHours) { this.totalNightHours = totalNightHours; }

    public int getTotalAbsenceDays() { return totalAbsenceDays; }
    public void setTotalAbsenceDays(int totalAbsenceDays) { this.totalAbsenceDays = totalAbsenceDays; }

    public int getSickLeaveDays() { return sickLeaveDays; }
    public void setSickLeaveDays(int sickLeaveDays) { this.sickLeaveDays = sickLeaveDays; }

    public int getUnpaidLeaveDays() { return unpaidLeaveDays; }
    public void setUnpaidLeaveDays(int unpaidLeaveDays) { this.unpaidLeaveDays = unpaidLeaveDays; }

    /**
     * Запис за един ден от месеца.
     */
    public static class DailyEntry {
        private int day;                     // 1-31
        private String dayType;              // WORK, WEEKEND, HOLIDAY, ABSENCE
        private BigDecimal workedHours;      // отработени часове
        private BigDecimal overtimeHours;    // извънреден труд
        private BigDecimal nightHours;       // нощен труд
        private String absenceCode;          // код на отсъствие (ако има)

        public DailyEntry() {}

        public int getDay() { return day; }
        public void setDay(int day) { this.day = day; }

        public String getDayType() { return dayType; }
        public void setDayType(String dayType) { this.dayType = dayType; }

        public BigDecimal getWorkedHours() { return workedHours; }
        public void setWorkedHours(BigDecimal workedHours) { this.workedHours = workedHours; }

        public BigDecimal getOvertimeHours() { return overtimeHours; }
        public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

        public BigDecimal getNightHours() { return nightHours; }
        public void setNightHours(BigDecimal nightHours) { this.nightHours = nightHours; }

        public String getAbsenceCode() { return absenceCode; }
        public void setAbsenceCode(String absenceCode) { this.absenceCode = absenceCode; }
    }
}
