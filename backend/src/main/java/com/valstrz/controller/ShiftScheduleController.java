package com.valstrz.controller;

import com.valstrz.entity.calendar.ShiftSchedule;
import com.valstrz.repository.ShiftScheduleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.StreamSupport;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','HR_MANAGER')")
@RestController
@RequestMapping("/api/companies/{tenantId}/shift-schedules")
public class ShiftScheduleController {

    private final ShiftScheduleRepository repository;

    public ShiftScheduleController(ShiftScheduleRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ShiftSchedule> getAll(@PathVariable String tenantId,
                                       @RequestParam(required = false) Boolean active) {
        Iterable<ShiftSchedule> result = (active != null)
                ? repository.findByTenantIdAndActive(tenantId, active)
                : repository.findByTenantId(tenantId);
        return StreamSupport.stream(result.spliterator(), false).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftSchedule> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ShiftSchedule> create(@PathVariable String tenantId,
                                                  @RequestBody ShiftSchedule schedule) {
        schedule.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(schedule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShiftSchedule> update(@PathVariable String tenantId,
                                                  @PathVariable String id,
                                                  @RequestBody ShiftSchedule schedule) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        schedule.setId(id);
        schedule.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(schedule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Зарежда предефинирани сменни графици за фирмата.
     */
    @PostMapping("/seed")
    public List<ShiftSchedule> seed(@PathVariable String tenantId) {
        List<ShiftSchedule> existing = StreamSupport.stream(
                repository.findByTenantId(tenantId).spliterator(), false).toList();
        if (!existing.isEmpty()) return existing;

        List<ShiftSchedule> templates = List.of(
                buildTemplate(tenantId, "12/24", "SHIFT_12_24", "ROTATING", 4,
                        List.of(shift(1, "Дневна 12ч", "07:00", "19:00", 12, 0)),
                        List.of(1, 0, 0)),
                buildTemplate(tenantId, "12/24/12/48", "SHIFT_12_48", "ROTATING", 4,
                        List.of(shift(1, "Дневна 12ч", "07:00", "19:00", 12, 0),
                                shift(2, "Нощна 12ч", "19:00", "07:00", 12, 8)),
                        List.of(1, 0, 2, 0, 0)),
                buildTemplate(tenantId, "Двусменен 8ч", "SHIFT_2x8", "ROTATING", 1,
                        List.of(shift(1, "Първа смяна", "06:00", "14:00", 8, 0),
                                shift(2, "Втора смяна", "14:00", "22:00", 8, 0)),
                        List.of(1, 1, 1, 1, 1, 0, 0)),
                buildTemplate(tenantId, "Трисменен 8ч", "SHIFT_3x8", "ROTATING", 3,
                        List.of(shift(1, "Първа смяна", "06:00", "14:00", 8, 0),
                                shift(2, "Втора смяна", "14:00", "22:00", 8, 0),
                                shift(3, "Нощна смяна", "22:00", "06:00", 8, 8)),
                        List.of(1, 1, 1, 1, 1, 0, 0))
        );

        return templates.stream().map(repository::save).toList();
    }

    // ── Помощни методи ──

    private ShiftSchedule buildTemplate(String tenantId, String name, String code, String type,
                                          int refMonths, List<ShiftSchedule.ShiftDefinition> shifts,
                                          List<Integer> pattern) {
        ShiftSchedule s = new ShiftSchedule();
        s.setTenantId(tenantId);
        s.setName(name);
        s.setCode(code);
        s.setType(type);
        s.setReferenceMonths(refMonths);
        s.setShifts(shifts);
        s.setRotationPattern(pattern);
        s.setActive(true);
        return s;
    }

    private static ShiftSchedule.ShiftDefinition shift(int index, String name,
                                                          String start, String end,
                                                          double totalH, double nightH) {
        ShiftSchedule.ShiftDefinition sd = new ShiftSchedule.ShiftDefinition();
        sd.setIndex(index);
        sd.setName(name);
        sd.setStartTime(java.time.LocalTime.parse(start));
        sd.setEndTime(java.time.LocalTime.parse(end));
        sd.setTotalHours(totalH);
        sd.setNightHours(nightH);
        return sd;
    }
}
