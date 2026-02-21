package com.valstrz.controller;

import com.valstrz.entity.calendar.MonthlyCalendar;
import com.valstrz.repository.MonthlyCalendarRepository;
import com.valstrz.service.MonthlyCalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/calendars")
public class MonthlyCalendarController {

    private final MonthlyCalendarRepository repository;
    private final MonthlyCalendarService service;
    private final com.valstrz.service.CalendarService calendarService;

    public MonthlyCalendarController(MonthlyCalendarRepository repository, 
                                     MonthlyCalendarService service,
                                     com.valstrz.service.CalendarService calendarService) {
        this.repository = repository;
        this.service = service;
        this.calendarService = calendarService;
    }

    @PostMapping("/{year}/{month}/generate")
    public ResponseEntity<MonthlyCalendar> generate(@PathVariable String tenantId,
                                                   @PathVariable int year,
                                                   @PathVariable int month) {
        return ResponseEntity.ok(service.generateCalendar(tenantId, year, month));
    }

    @PostMapping("/generate-year")
    public ResponseEntity<Void> generateYear(@PathVariable String tenantId, @RequestParam int year) {
        calendarService.seedBulgarianHolidays(tenantId, year);
        service.generateYearlyCalendar(tenantId, year);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public Iterable<MonthlyCalendar> getByYear(@PathVariable String tenantId,
                                                @RequestParam(required = false) Integer year) {
        if (year != null) {
            return repository.findByTenantIdAndYear(tenantId, year);
        }
        return repository.findByTenantId(tenantId);
    }

    @GetMapping("/{year}/{month}")
    public ResponseEntity<MonthlyCalendar> getByYearMonth(@PathVariable String tenantId,
                                                           @PathVariable int year,
                                                           @PathVariable int month) {
        var results = repository.findByTenantIdAndYearAndMonth(tenantId, year, month);
        var iterator = results.iterator();
        if (iterator.hasNext()) {
            return ResponseEntity.ok(iterator.next());
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<MonthlyCalendar> create(@PathVariable String tenantId,
                                                   @RequestBody MonthlyCalendar calendar) {
        calendar.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(calendar));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonthlyCalendar> update(@PathVariable String tenantId,
                                                   @PathVariable String id,
                                                   @RequestBody MonthlyCalendar calendar) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        calendar.setId(id);
        calendar.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(calendar));
    }
}
