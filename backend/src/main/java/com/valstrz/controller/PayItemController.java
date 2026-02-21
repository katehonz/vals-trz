package com.valstrz.controller;

import com.valstrz.entity.payroll.PayItem;
import com.valstrz.entity.payroll.PayItem.PayItemType;
import com.valstrz.repository.PayItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
@RestController
@RequestMapping("/api/companies/{tenantId}/pay-items")
public class PayItemController {

    private final PayItemRepository repository;

    public PayItemController(PayItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<PayItem> getAll(@PathVariable String tenantId,
                                     @RequestParam(required = false) Boolean active) {
        if (active != null) {
            return repository.findByTenantIdAndActive(tenantId, active);
        }
        return repository.findByTenantId(tenantId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayItem> getById(@PathVariable String id) {
        Optional<PayItem> item = repository.findById(id);
        return item.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PayItem> create(@PathVariable String tenantId,
                                           @RequestBody PayItem item) {
        item.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PayItem> update(@PathVariable String tenantId,
                                           @PathVariable String id,
                                           @RequestBody PayItem item) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        item.setId(id);
        item.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Optional<PayItem> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (existing.get().isSystem()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/seed")
    public List<PayItem> seed(@PathVariable String tenantId) {
        // Проверка дали вече има данни
        List<PayItem> existing = StreamSupport.stream(
                repository.findByTenantId(tenantId).spliterator(), false)
                .collect(Collectors.toList());
        if (!existing.isEmpty()) {
            return existing;
        }

        List<PayItem> items = new ArrayList<>();

        // ── Заплати (100-199) ──
        items.add(pi(tenantId, "101", "Работна заплата", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "102", "Заплата доп.тр.дог.", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "103", "Сделна заплата", PayItemType.PER_UNIT, true));
        items.add(pi(tenantId, "160", "Болничен работодател", PayItemType.CALCULATED, true));

        // ── Допълнителни (200-299) ──
        items.add(pi(tenantId, "201", "Клас", PayItemType.PERCENT, true));
        items.add(pi(tenantId, "211", "Извънреден труд 50%", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "212", "Извънреден труд 75%", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "213", "Празничен труд 100%", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "214", "Нощен труд", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "221", "Премии", PayItemType.FIXED, false));
        items.add(pi(tenantId, "231", "Престой", PayItemType.CALCULATED, true));

        // ── Болнични (300-319) ──
        items.add(pi(tenantId, "301", "Болничен - ОЗ", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "302", "Болничен - бременност 410", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "303", "Болничен - битова злополука", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "304", "Болничен - гл. болен над 18", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "305", "Болничен - проф. болест", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "306", "Болничен - труд. злополука", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "307", "Болничен - бременност", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "308", "Болничен - гл. болен до 18", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "309", "Болничен - баща 15 дни", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "310", "Болничен - баща 410 дни", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "311", "Болничен - отпуск осиновяване 2-5 г.", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "312", "Болничен - отпуск осиновяване 2-5 г. баща", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "313", "Болничен - отпуск осиновяване 8 г. баща", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "315", "Болничен - неплатен", PayItemType.CALCULATED, true));

        // ── Отпуски (320-339) ──
        items.add(pi(tenantId, "321", "Редовен отпуск", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "322", "Служебен отпуск", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "323", "Отпуск кръводаряване", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "324", "Ученически отпуск", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "325", "Отпуск - дете до 2 г.", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "326", "Отпуск - дете 2-8 г.", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "327", "Отпуск по чл. 157 КТ", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "328", "Допълнителен отпуск", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "329", "Баща - гледане дете до 8 г.", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "331", "Неплатен със стаж - работодател", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "332", "Неплатен без стаж - работодател", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "333", "Неплатен със стаж - лично", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "334", "Неплатен без стаж - лично", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "335", "Неплатен без стаж - без осиг.", PayItemType.CALCULATED, true));

        // ── Отсъствия (340-349) ──
        items.add(pi(tenantId, "341", "Без болничен", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "342", "Недефинирано отсъствие", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "343", "Самоотлъчка", PayItemType.CALCULATED, true));

        // ── Обезщетения (400-499) ──
        items.add(pi(tenantId, "401", "Обезщ.чл.220-несп.пр", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "402", "Обезщ.чл.221-без пр.", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "403", "Обезщ.чл.222,ал.1", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "404", "Обезщ.чл.222,ал.3", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "405", "Обезщ.чл.224", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "406", "Обезщ.чл.217", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "407", "Обезщ.чл.222,ал.2", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "413", "Обезщ.чл.222,ал.1 пр.", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "414", "Обезщ. чл. 225 КТ", PayItemType.CALCULATED, true));
        items.add(pi(tenantId, "451", "За минал месец ФРЗ", PayItemType.FIXED, false));
        items.add(pi(tenantId, "460", "Болничен работодател (корекция)", PayItemType.CALCULATED, false));

        // ── Социални (500-599) ──
        items.add(pi(tenantId, "500", "Ваучери", PayItemType.FIXED, false));
        items.add(pi(tenantId, "501", "Социални разходи", PayItemType.FIXED, false));

        // ── Самоосигуряващи се (600-699) ──
        items.add(pi(tenantId, "601", "Осигур.доход самоосигуряващ пенсии", PayItemType.FIXED, false));
        items.add(pi(tenantId, "602", "Осигур.доход самоосигуряващ пенсии+ОЗМ", PayItemType.FIXED, false));
        items.add(pi(tenantId, "603", "Осигур.доход 41 пенсии", PayItemType.FIXED, false));
        items.add(pi(tenantId, "605", "Самоосигуряващ личен труд", PayItemType.FIXED, false));
        items.add(pi(tenantId, "620", "Отсъствие стажанти", PayItemType.CALCULATED, false));

        // ── Бази за осигуряване (700-799) ──
        items.add(pi(tenantId, "701", "База за осиг.за непл.отп. - л", PayItemType.CALCULATED, false));
        items.add(pi(tenantId, "702", "База за осиг.за непл.отп. - р.", PayItemType.CALCULATED, false));
        items.add(pi(tenantId, "703", "База за осиг.за болнични", PayItemType.CALCULATED, false));

        // ── Доход от друг работодател (800-899) ──
        items.add(pi(tenantId, "800", "Осигур.доход друг работодател", PayItemType.FIXED, false));
        items.add(pi(tenantId, "890", "Доход друг работодател", PayItemType.FIXED, false));

        List<PayItem> saved = new ArrayList<>();
        repository.saveAll(items).forEach(saved::add);
        return saved;
    }

    private PayItem pi(String tenantId, String code, String name, PayItemType type, boolean system) {
        PayItem item = new PayItem();
        item.setTenantId(tenantId);
        item.setCode(code);
        item.setName(name);
        item.setType(type);
        item.setSystem(system);
        item.setActive(true);
        return item;
    }
}
