package com.valstrz.service;

import com.valstrz.entity.calendar.AnnualCalendar;
import com.valstrz.entity.calendar.WorkSchedule;
import com.valstrz.entity.personnel.Absence;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.entity.personnel.MonthlyTimesheet;
import com.valstrz.entity.personnel.MonthlyTimesheet.DailyEntry;
import com.valstrz.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class MonthlyTimesheetService {

    private static final BigDecimal DEFAULT_HOURS = new BigDecimal("8");

    private final MonthlyTimesheetRepository timesheetRepository;
    private final EmploymentRepository employmentRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final AbsenceRepository absenceRepository;
    private final AnnualCalendarRepository annualCalendarRepository;

    public MonthlyTimesheetService(MonthlyTimesheetRepository timesheetRepository,
                                    EmploymentRepository employmentRepository,
                                    WorkScheduleRepository workScheduleRepository,
                                    AbsenceRepository absenceRepository,
                                    AnnualCalendarRepository annualCalendarRepository) {
        this.timesheetRepository = timesheetRepository;
        this.employmentRepository = employmentRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.absenceRepository = absenceRepository;
        this.annualCalendarRepository = annualCalendarRepository;
    }

    public MonthlyTimesheet getOrCreateTimesheet(String tenantId, String employeeId, int year, int month) {
        Iterable<MonthlyTimesheet> existing = timesheetRepository.findByTenantIdAndEmployeeIdAndYearAndMonth(tenantId, employeeId, year, month);
        if (existing.iterator().hasNext()) {
            return existing.iterator().next();
        }

        BigDecimal hoursPerDay = resolveHoursPerDay(tenantId, employeeId);
        Set<LocalDate> holidays = getHolidays(tenantId, year);
        Map<LocalDate, Absence> absenceMap = getAbsenceMap(tenantId, employeeId, year, month);

        MonthlyTimesheet ts = new MonthlyTimesheet();
        ts.setTenantId(tenantId);
        ts.setEmployeeId(employeeId);
        ts.setYear(year);
        ts.setMonth(month);

        List<DailyEntry> days = new ArrayList<>();
        int daysInMonth = LocalDate.of(year, month, 1).lengthOfMonth();
        int workedDays = 0;
        int absenceDays = 0;
        int sickLeaveDays = 0;
        int unpaidLeaveDays = 0;
        BigDecimal totalWorkedHours = BigDecimal.ZERO;

        for (int i = 1; i <= daysInMonth; i++) {
            LocalDate date = LocalDate.of(year, month, i);
            DailyEntry entry = new DailyEntry();
            entry.setDay(i);
            entry.setOvertimeHours(BigDecimal.ZERO);
            entry.setNightHours(BigDecimal.ZERO);

            boolean isWeekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean isHoliday = holidays.contains(date);

            if (isWeekend) {
                entry.setDayType("WEEKEND");
                entry.setWorkedHours(BigDecimal.ZERO);
            } else if (isHoliday) {
                entry.setDayType("HOLIDAY");
                entry.setWorkedHours(BigDecimal.ZERO);
            } else if (absenceMap.containsKey(date)) {
                Absence abs = absenceMap.get(date);
                entry.setDayType("ABSENCE");
                entry.setAbsenceCode(abs.getType());
                entry.setWorkedHours(BigDecimal.ZERO);
                absenceDays++;

                // Класифициране на отсъствието
                String absType = abs.getType() != null ? abs.getType() : "";
                if (absType.startsWith("3516")) {
                    // Болнични (351631-351640)
                    sickLeaveDays++;
                } else if ("351305".equals(absType) || "351306".equals(absType)) {
                    // Неплатен отпуск
                    unpaidLeaveDays++;
                }
            } else {
                entry.setDayType("WORK");
                entry.setWorkedHours(hoursPerDay);
                workedDays++;
                totalWorkedHours = totalWorkedHours.add(hoursPerDay);
            }

            days.add(entry);
        }

        ts.setDays(days);
        ts.setTotalWorkedDays(workedDays);
        ts.setTotalWorkedHours(totalWorkedHours);
        ts.setTotalAbsenceDays(absenceDays);
        ts.setSickLeaveDays(sickLeaveDays);
        ts.setUnpaidLeaveDays(unpaidLeaveDays);

        return timesheetRepository.save(ts);
    }

    /**
     * Определя часове на ден за конкретен служител, базирано на часовата му схема.
     */
    private BigDecimal resolveHoursPerDay(String tenantId, String employeeId) {
        // Търсим текущо employment
        for (Employment empl : employmentRepository.findByTenantIdAndEmployeeIdAndCurrent(tenantId, employeeId, true)) {
            String wsCode = empl.getWorkScheduleCode();
            if (wsCode != null && !wsCode.isEmpty()) {
                for (WorkSchedule ws : workScheduleRepository.findByTenantId(tenantId)) {
                    if (wsCode.equals(ws.getCode()) && ws.getHoursPerDay() != null) {
                        return ws.getHoursPerDay();
                    }
                }
            }
        }
        return DEFAULT_HOURS;
    }

    /**
     * Зарежда празничните дни от годишния календар.
     */
    private Set<LocalDate> getHolidays(String tenantId, int year) {
        Set<LocalDate> dates = new HashSet<>();
        for (AnnualCalendar ac : annualCalendarRepository.findByTenantIdAndYear(tenantId, year)) {
            if (ac.getHolidays() != null) {
                for (AnnualCalendar.HolidayEntry h : ac.getHolidays()) {
                    dates.add(h.getDate());
                }
            }
        }
        return dates;
    }

    /**
     * Изгражда карта дата -> отсъствие за даден месец.
     */
    private Map<LocalDate, Absence> getAbsenceMap(String tenantId, String employeeId, int year, int month) {
        Map<LocalDate, Absence> map = new LinkedHashMap<>();
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        for (Absence abs : absenceRepository.findByTenantIdAndEmployeeId(tenantId, employeeId)) {
            if (abs.getFromDate() == null || abs.getToDate() == null) continue;
            if (!"APPROVED".equals(abs.getStatus()) && !"ACTIVE".equals(abs.getStatus()) && !"COMPLETED".equals(abs.getStatus())) continue;

            // Определяме припокриването с текущия месец
            LocalDate start = abs.getFromDate().isBefore(monthStart) ? monthStart : abs.getFromDate();
            LocalDate end = abs.getToDate().isAfter(monthEnd) ? monthEnd : abs.getToDate();

            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                DayOfWeek dow = d.getDayOfWeek();
                if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                    map.put(d, abs);
                }
            }
        }
        return map;
    }
}
