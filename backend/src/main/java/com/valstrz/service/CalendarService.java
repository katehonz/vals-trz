package com.valstrz.service;

import com.valstrz.entity.calendar.AnnualCalendar;
import com.valstrz.entity.calendar.AnnualCalendar.HolidayEntry;
import com.valstrz.repository.AnnualCalendarRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private final AnnualCalendarRepository annualRepository;

    public CalendarService(AnnualCalendarRepository annualRepository) {
        this.annualRepository = annualRepository;
    }

    public AnnualCalendar seedBulgarianHolidays(String tenantId, int year) {
        List<HolidayEntry> entries = getBulgarianHolidays(year);

        Iterable<AnnualCalendar> existing = annualRepository.findByTenantIdAndYear(tenantId, year);
        AnnualCalendar ac = existing.iterator().hasNext() ? existing.iterator().next() : new AnnualCalendar();

        ac.setTenantId(tenantId);
        ac.setYear(year);
        ac.setHolidays(entries);
        return annualRepository.save(ac);
    }

    private List<HolidayEntry> getBulgarianHolidays(int year) {
        // Фиксирани празници с имена
        Map<LocalDate, String> fixedHolidays = new LinkedHashMap<>();
        fixedHolidays.put(LocalDate.of(year, Month.JANUARY, 1), "Нова година");
        fixedHolidays.put(LocalDate.of(year, Month.MARCH, 3), "Ден на Освобождението на България");
        fixedHolidays.put(LocalDate.of(year, Month.MAY, 1), "Ден на труда и международната работническа солидарност");
        fixedHolidays.put(LocalDate.of(year, Month.MAY, 6), "Гергьовден - Ден на храбростта и Българската армия");
        fixedHolidays.put(LocalDate.of(year, Month.MAY, 24), "Ден на светите братя Кирил и Методий, на българската азбука, просвета и култура");
        fixedHolidays.put(LocalDate.of(year, Month.SEPTEMBER, 6), "Ден на Съединението");
        fixedHolidays.put(LocalDate.of(year, Month.SEPTEMBER, 22), "Ден на Независимостта на България");
        fixedHolidays.put(LocalDate.of(year, Month.NOVEMBER, 1), "Ден на народните будители");
        fixedHolidays.put(LocalDate.of(year, Month.DECEMBER, 24), "Бъдни вечер");
        fixedHolidays.put(LocalDate.of(year, Month.DECEMBER, 25), "Рождество Христово (Коледа)");
        fixedHolidays.put(LocalDate.of(year, Month.DECEMBER, 26), "Втори ден на Коледа");

        // Православен Великден (изчислен по алгоритъм)
        LocalDate easter = orthodoxEaster(year);
        fixedHolidays.put(easter.minusDays(2), "Велики петък");
        fixedHolidays.put(easter.minusDays(1), "Велика събота");
        fixedHolidays.put(easter, "Великден");
        fixedHolidays.put(easter.plusDays(1), "Втори ден на Великден");

        // Чл. 154 ал. 2 КТ — когато празник е в събота/неделя, следващият работен ден е почивен
        Set<LocalDate> allDates = new HashSet<>(fixedHolidays.keySet());
        Map<LocalDate, String> compensated = new LinkedHashMap<>();

        for (Map.Entry<LocalDate, String> entry : fixedHolidays.entrySet()) {
            LocalDate date = entry.getKey();
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                LocalDate comp = date;
                do {
                    comp = comp.plusDays(1);
                } while (comp.getDayOfWeek() == DayOfWeek.SATURDAY
                        || comp.getDayOfWeek() == DayOfWeek.SUNDAY
                        || allDates.contains(comp)
                        || compensated.containsKey(comp));
                compensated.put(comp, "Почивен ден (преместен от " + entry.getValue() + ")");
                allDates.add(comp);
            }
        }

        // Събираме всички в списък
        List<HolidayEntry> entries = new ArrayList<>();
        for (Map.Entry<LocalDate, String> e : fixedHolidays.entrySet()) {
            entries.add(makeEntry(e.getKey(), e.getValue(), true));
        }
        for (Map.Entry<LocalDate, String> e : compensated.entrySet()) {
            entries.add(makeEntry(e.getKey(), e.getValue(), true));
        }
        entries.sort(Comparator.comparing(HolidayEntry::getDate));
        return entries;
    }

    /**
     * Православен пасхален алгоритъм (Meeus's Julian algorithm + Gregorian offset).
     * Връща датата на Великден по юлианския календар, конвертирана в григориански.
     */
    static LocalDate orthodoxEaster(int year) {
        int a = year % 4;
        int b = year % 7;
        int c = year % 19;
        int d = (19 * c + 15) % 30;
        int e = (2 * a + 4 * b - d + 34) % 7;
        int month = (d + e + 114) / 31;  // 3 = March, 4 = April (Julian)
        int day = ((d + e + 114) % 31) + 1;

        // Julian date → Gregorian: add offset (13 days for 1900-2099)
        LocalDate julianDate = LocalDate.of(year, month, day);
        return julianDate.plusDays(13);
    }

    private HolidayEntry makeEntry(LocalDate date, String name, boolean official) {
        HolidayEntry entry = new HolidayEntry();
        entry.setDate(date);
        entry.setName(name);
        entry.setOfficial(official);
        return entry;
    }
}
