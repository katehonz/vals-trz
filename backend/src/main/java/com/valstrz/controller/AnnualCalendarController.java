package com.valstrz.controller;

import com.valstrz.entity.calendar.AnnualCalendar;
import com.valstrz.repository.AnnualCalendarRepository;
import com.valstrz.service.CalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/annual-calendars")
public class AnnualCalendarController {

    private final AnnualCalendarRepository repository;
    private final CalendarService calendarService;

    public AnnualCalendarController(AnnualCalendarRepository repository, CalendarService calendarService) {
        this.repository = repository;
        this.calendarService = calendarService;
    }

    @GetMapping
    public Iterable<AnnualCalendar> getByYear(@PathVariable String tenantId,
                                                @RequestParam(required = false) Integer year) {
        if (year != null) {
            return repository.findByTenantIdAndYear(tenantId, year);
        }
        return repository.findByTenantId(tenantId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnnualCalendar> getById(@PathVariable String id) {
        Optional<AnnualCalendar> ac = repository.findById(id);
        return ac.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AnnualCalendar> create(@PathVariable String tenantId,
                                                   @RequestBody AnnualCalendar calendar) {
        calendar.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(calendar));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AnnualCalendar> update(@PathVariable String tenantId,
                                                   @PathVariable String id,
                                                   @RequestBody AnnualCalendar calendar) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        calendar.setId(id);
        calendar.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(calendar));
    }

    @PostMapping("/seed")
    public ResponseEntity<AnnualCalendar> seedBgHolidays(@PathVariable String tenantId,
                                                           @RequestParam int year) {
        return ResponseEntity.ok(calendarService.seedBulgarianHolidays(tenantId, year));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
