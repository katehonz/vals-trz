package com.valstrz.entity.calendar;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.time.LocalDate;
import java.util.List;

/**
 * Годишен работен календар - официални празници и специфични за фирмата неработни дни.
 */
@Document("annualCalendars")
public class AnnualCalendar extends BaseEntity {

    private int year;
    private List<HolidayEntry> holidays;

    public AnnualCalendar() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public List<HolidayEntry> getHolidays() { return holidays; }
    public void setHolidays(List<HolidayEntry> holidays) { this.holidays = holidays; }

    public static class HolidayEntry {
        private LocalDate date;
        private String name;       // наименование на празника
        private boolean official;  // официален празник (true) или фирмен (false)

        public HolidayEntry() {}

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isOfficial() { return official; }
        public void setOfficial(boolean official) { this.official = official; }
    }
}
