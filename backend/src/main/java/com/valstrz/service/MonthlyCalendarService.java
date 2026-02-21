package com.valstrz.service;

import com.valstrz.entity.calendar.AnnualCalendar;
import com.valstrz.entity.calendar.MonthlyCalendar;
import com.valstrz.entity.calendar.WorkSchedule;
import com.valstrz.repository.AnnualCalendarRepository;
import com.valstrz.repository.MonthlyCalendarRepository;
import com.valstrz.repository.WorkScheduleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;

@Service
public class MonthlyCalendarService {

    private static final BigDecimal DEFAULT_HOURS = new BigDecimal("8");

    private final MonthlyCalendarRepository monthlyRepository;
    private final AnnualCalendarRepository annualRepository;
    private final WorkScheduleRepository workScheduleRepository;

    public MonthlyCalendarService(MonthlyCalendarRepository monthlyRepository,
                                   AnnualCalendarRepository annualRepository,
                                   WorkScheduleRepository workScheduleRepository) {
        this.monthlyRepository = monthlyRepository;
        this.annualRepository = annualRepository;
        this.workScheduleRepository = workScheduleRepository;
    }

    public MonthlyCalendar generateCalendar(String tenantId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        int calendarDays = yearMonth.lengthOfMonth();
        
        Set<LocalDate> holidays = getHolidays(tenantId, year, month);
        
        int workingDays = 0;
        int holidayCount = 0;
        for (int day = 1; day <= calendarDays; day++) {
            LocalDate date = LocalDate.of(year, month, day);
            DayOfWeek dow = date.getDayOfWeek();
            
            boolean isHoliday = holidays.contains(date);
            boolean isWeekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
            
            if (isHoliday) holidayCount++;
            
            if (!isWeekend && !isHoliday) {
                workingDays++;
            }
        }

        MonthlyCalendar mc = new MonthlyCalendar();
        mc.setTenantId(tenantId);
        mc.setYear(year);
        mc.setMonth(month);
        mc.setCalendarDays(calendarDays);
        mc.setWorkingDays(workingDays);
        mc.setHolidays(holidayCount);
        BigDecimal defaultHours = getDefaultHoursPerDay(tenantId);
        mc.setWorkingHoursPerDay(defaultHours);
        mc.setTotalWorkingHours(new BigDecimal(workingDays).multiply(defaultHours));
        
        // Find existing to update
        Iterable<MonthlyCalendar> existingList = monthlyRepository.findByTenantIdAndYearAndMonth(tenantId, year, month);
        if (existingList.iterator().hasNext()) {
            mc.setId(existingList.iterator().next().getId());
        }

        return monthlyRepository.save(mc);
    }

    public void generateYearlyCalendar(String tenantId, int year) {
        for (int m = 1; m <= 12; m++) {
            generateCalendar(tenantId, year, m);
        }
    }

    /**
     * Връща подразбиращите се часове/ден за tenant-а.
     * Взима първата налична часова схема с код "8" или "FULL", иначе 8.
     */
    private BigDecimal getDefaultHoursPerDay(String tenantId) {
        for (WorkSchedule ws : workScheduleRepository.findByTenantId(tenantId)) {
            if (ws.getHoursPerDay() != null) {
                // Ползваме първата намерена схема с пълно работно време
                String code = ws.getCode() != null ? ws.getCode().toUpperCase() : "";
                if (code.contains("8") || code.contains("FULL") || code.contains("ПЪЛНО")) {
                    return ws.getHoursPerDay();
                }
            }
        }
        return DEFAULT_HOURS;
    }

    private Set<LocalDate> getHolidays(String tenantId, int year, int month) {
        Set<LocalDate> dates = new HashSet<>();
        Iterable<AnnualCalendar> annuals = annualRepository.findByTenantIdAndYear(tenantId, year);
        for (AnnualCalendar ac : annuals) {
            if (ac.getHolidays() != null) {
                for (AnnualCalendar.HolidayEntry h : ac.getHolidays()) {
                    if (h.getDate().getMonthValue() == month) {
                        dates.add(h.getDate());
                    }
                }
            }
        }
        return dates;
    }
}
