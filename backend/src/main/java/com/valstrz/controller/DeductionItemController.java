package com.valstrz.controller;

import com.valstrz.entity.payroll.DeductionItem;
import com.valstrz.entity.payroll.DeductionItem.DeductionType;
import com.valstrz.repository.DeductionItemRepository;
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
@RequestMapping("/api/companies/{tenantId}/deduction-items")
public class DeductionItemController {

    private final DeductionItemRepository repository;

    public DeductionItemController(DeductionItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Iterable<DeductionItem> getAll(@PathVariable String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @PostMapping
    public ResponseEntity<DeductionItem> create(@PathVariable String tenantId,
                                                 @RequestBody DeductionItem item) {
        item.setTenantId(tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeductionItem> update(@PathVariable String tenantId,
                                                 @PathVariable String id,
                                                 @RequestBody DeductionItem item) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        item.setId(id);
        item.setTenantId(tenantId);
        return ResponseEntity.ok(repository.save(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Optional<DeductionItem> existing = repository.findById(id);
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
    public List<DeductionItem> seed(@PathVariable String tenantId) {
        List<DeductionItem> existing = StreamSupport.stream(
                repository.findByTenantId(tenantId).spliterator(), false)
                .collect(Collectors.toList());
        if (!existing.isEmpty()) {
            return existing;
        }

        List<DeductionItem> items = new ArrayList<>();
        DeductionType C = DeductionType.CALCULATED;
        DeductionType F = DeductionType.FIXED;

        // ── Доброволно осигуряване (160-199) ──
        items.add(di(tenantId, "160", "Добр. пенс. осиг.- група 1", F, false));
        items.add(di(tenantId, "161", "Добр. осиг.за безр.- група 1", F, false));
        items.add(di(tenantId, "165", "Добр. здр. осиг.- група 2", F, false));
        items.add(di(tenantId, "166", "Застр.\"Живот-рента\"-група 2", F, false));

        // ── Здравно осигуряване - работник (200-209) ──
        items.add(di(tenantId, "201", "ЗО+СР Л", C, true));
        items.add(di(tenantId, "202", "ЗО-СР Л", C, true));
        items.add(di(tenantId, "203", "ЗО-непл.отп. Л", C, true));

        // ── ДОО - работник, 3-та категория (220-269) ──
        items.add(di(tenantId, "221", "ДОО-Пнс,ОЗМ,Бзрб 3 Лс", C, true));
        items.add(di(tenantId, "222", "ДОО-Пнс,ОЗМ,Бзрб 3 Лп", C, true));
        items.add(di(tenantId, "223", "ДОО-Пнс-СР 3 Лс", C, true));
        items.add(di(tenantId, "224", "ДОО-Пнс-СР 3 Лп", C, true));

        // ── ДОО - работник, 1-2 категория ──
        items.add(di(tenantId, "231", "ДОО-Пнс,ОЗМ,Бзрб 1, 2 Лс", C, true));
        items.add(di(tenantId, "232", "ДОО-Пнс,ОЗМ,Бзрб 1, 2 Лп", C, true));
        items.add(di(tenantId, "233", "ДОО-Пнс-СР 1, 2 Лс", C, true));
        items.add(di(tenantId, "234", "ДОО-Пнс-СР 1, 2 Лп", C, true));

        // ── ДОО - работник, специални видове ──
        items.add(di(tenantId, "235", "ДОО-Пнс 11 3 Лс", C, false));
        items.add(di(tenantId, "236", "ДОО-Пнс 11 3 Лп", C, false));
        items.add(di(tenantId, "237", "ДОО-Пнс 11 1, 2 Лс", C, false));
        items.add(di(tenantId, "238", "ДОО-Пнс 11 1, 2 Лп", C, false));
        items.add(di(tenantId, "241", "ДОО-Пнс 14 Лс", C, false));
        items.add(di(tenantId, "242", "ДОО-Пнс 14 Лп", C, false));
        items.add(di(tenantId, "243", "ДОО-Пнс 25 Лс", C, false));
        items.add(di(tenantId, "244", "ДОО-Пнс 25 Лп", C, false));
        items.add(di(tenantId, "247", "ДОО-Пнс 27 Лс", C, false));
        items.add(di(tenantId, "248", "ДОО-Пнс 27 Лп", C, false));
        items.add(di(tenantId, "250", "ДОО-Пнс 97 Лс", C, false));
        items.add(di(tenantId, "251", "ДОО-Пнс 97 Лп", C, false));

        // ── ДОО ОЗМ/Безработица - работник ──
        items.add(di(tenantId, "255", "ДОО-ОЗМ 3 Л", C, true));
        items.add(di(tenantId, "256", "ДОО-ОЗМ 1, 2 Л", C, true));
        items.add(di(tenantId, "257", "ДОО-ОЗМ 25 Л", C, false));
        items.add(di(tenantId, "261", "ДОО-Безраб. 3 Л", C, true));
        items.add(di(tenantId, "262", "ДОО-Безраб. 1, 2 Л", C, true));

        // ── КООП ──
        items.add(di(tenantId, "271", "ДОО-Пнс,ОЗМ 9 Лс КООП", C, false));
        items.add(di(tenantId, "272", "ДОО-Пнс,ОЗМ 9 Лп КООП", C, false));

        // ── ДЗПО - работник ──
        items.add(di(tenantId, "281", "ДОО+СР-УПФ Л", C, true));
        items.add(di(tenantId, "282", "ДОО-УПФ-СР Л", C, true));

        // ── Самоосигуряващи се (300-359) ──
        items.add(di(tenantId, "301", "ЗО-самоосигуряващ", C, false));
        items.add(di(tenantId, "302", "ЗО - Болнични сам.", C, false));
        items.add(di(tenantId, "305", "ЗО-самоосигуряващ личен труд", C, false));
        items.add(di(tenantId, "306", "ЗО - Болнични сам. личен труд", C, false));
        items.add(di(tenantId, "308", "ЗО 41", C, false));
        items.add(di(tenantId, "311", "ДОО-Пнс-самоосиг.С 12", C, false));
        items.add(di(tenantId, "312", "ДОО-Пнс-самоосиг.П 12", C, false));
        items.add(di(tenantId, "313", "ДОО-Пнс-самоосиг.С 13", C, false));
        items.add(di(tenantId, "314", "ДОО-Пнс-самоосиг.П 13", C, false));
        items.add(di(tenantId, "315", "ДОО-Пнс-сам.+ОЗМ С 12", C, false));
        items.add(di(tenantId, "316", "ДОО-Пнс-сам.+ОЗМ П 12", C, false));
        items.add(di(tenantId, "317", "ДОО-Пнс-сам.+ОЗМ С 13", C, false));
        items.add(di(tenantId, "318", "ДОО-Пнс-сам.+ОЗМ П 13", C, false));
        items.add(di(tenantId, "321", "ДОО-ОЗМ-самоосиг. 12", C, false));
        items.add(di(tenantId, "322", "ДОО-ОЗМ-самоосиг. 13", C, false));
        items.add(di(tenantId, "325", "ДОО-УПФ-самоосиг.", C, false));
        items.add(di(tenantId, "327", "ДОО-Пнс 41 С", C, false));
        items.add(di(tenantId, "328", "ДОО-Пнс 41 П", C, false));
        items.add(di(tenantId, "329", "ДОО-УПФ 41", C, false));

        // ── Морски лица (330-349) ──
        items.add(di(tenantId, "331", "Пнс+ОЗМ-МЛ С 92", C, false));
        items.add(di(tenantId, "332", "Пнс+ОЗМ-МЛ П 92", C, false));
        items.add(di(tenantId, "333", "Пнс+ОЗМ+Бзрб-МЛ С 92", C, false));
        items.add(di(tenantId, "334", "Пнс+ОЗМ+Бзрб-МЛ П 92", C, false));
        items.add(di(tenantId, "335", "ДОО-УПФ-МЛ", C, false));
        items.add(di(tenantId, "337", "ЗО-МЛ", C, false));
        items.add(di(tenantId, "338", "ЗО - Болнични МЛ", C, false));
        items.add(di(tenantId, "339", "ЗО-непл.отп. Л МЛ", C, false));
        items.add(di(tenantId, "341", "Пнс+ОЗМ-МЛ 1,2 С 92", C, false));
        items.add(di(tenantId, "342", "Пнс+ОЗМ-МЛ 1,2 П 92", C, false));
        items.add(di(tenantId, "343", "Пнс+ОЗМ+Бзрб-МЛ 1,2 С 92", C, false));
        items.add(di(tenantId, "344", "Пнс+ОЗМ+Бзрб-МЛ 1,2 П 92", C, false));
        items.add(di(tenantId, "345", "ДОО-ТЗПБ-МЛ", C, false));
        items.add(di(tenantId, "346", "ДЗПО-2 кт МЛ", C, false));
        items.add(di(tenantId, "347", "ДЗПО-1 кт МЛ", C, false));

        // ── Самоосигуряващи се - личен труд (351-359) ──
        items.add(di(tenantId, "351", "ДОО-Пнс-сам.С 12 личен труд", C, false));
        items.add(di(tenantId, "352", "ДОО-Пнс-сам.П 12 личен труд", C, false));
        items.add(di(tenantId, "353", "ДОО-Пнс-сам.С 13 личен труд", C, false));
        items.add(di(tenantId, "354", "ДОО-Пнс-сам.П 13 личен труд", C, false));
        items.add(di(tenantId, "355", "ДОО-Пнс-сам.+ОЗМ С 12 л. труд", C, false));
        items.add(di(tenantId, "356", "ДОО-Пнс-сам.+ОЗМ П 12 л. труд", C, false));
        items.add(di(tenantId, "357", "ДОО-Пнс-сам.+ОЗМ С 13 л. труд", C, false));
        items.add(di(tenantId, "358", "ДОО-Пнс-сам.+ОЗМ П 13 л. труд", C, false));
        items.add(di(tenantId, "359", "ДОО-УПФ-самоосиг. личен труд", C, false));

        // ── Аванси, запори, заеми (400-499) ──
        items.add(di(tenantId, "401", "Аванс", F, true));
        items.add(di(tenantId, "451", "Запори", F, true));
        items.add(di(tenantId, "455", "Заеми", F, false));
        items.add(di(tenantId, "480", "Добр.пнс.осиг.група 1 - обл.", F, false));
        items.add(di(tenantId, "481", "Добр.осиг.безр.гр.1 - обл.", F, false));
        items.add(di(tenantId, "482", "Добр.здр.осиг.гр.2 - обл.", F, false));
        items.add(di(tenantId, "483", "Добр.осиг.Живот гр.2 - обл.", F, false));
        items.add(di(tenantId, "490", "Намаляване на данъчна основа", F, false));

        // ── Данъци (500-569) ──
        items.add(di(tenantId, "500", "ДОД", C, true));
        items.add(di(tenantId, "505", "ДОД ТЕЛК", C, true));
        items.add(di(tenantId, "548", "Данъчна основа служ.бележки", C, false));
        items.add(di(tenantId, "549", "Удържан данък служ.бележки", C, false));
        items.add(di(tenantId, "550", "Разлика годишен ДОД", C, false));
        items.add(di(tenantId, "551", "Разлика годишен ДОД ТЕЛК", C, false));
        items.add(di(tenantId, "560", "Дължим годишен ДОД", C, false));
        items.add(di(tenantId, "561", "Преизчислен годишен ДОД", C, false));
        items.add(di(tenantId, "562", "Преизчислен месечен ДОД", C, false));

        // ── Осигуровки - работодател (600-699) ──
        items.add(di(tenantId, "601", "ЗО+СР Р", C, true));
        items.add(di(tenantId, "602", "ЗО-СР Р", C, true));
        items.add(di(tenantId, "603", "ЗО-непл.отп. Р", C, true));
        items.add(di(tenantId, "604", "ЗО - Болнични Р", C, true));

        items.add(di(tenantId, "621", "ДОО-Пнс,ОЗМ,Бзрб 3 Рс", C, true));
        items.add(di(tenantId, "622", "ДОО-Пнс,ОЗМ,Бзрб 3 Рп", C, true));
        items.add(di(tenantId, "623", "ДОО-Пнс-СР 3 Рс", C, true));
        items.add(di(tenantId, "624", "ДОО-Пнс-СР 3 Рп", C, true));
        items.add(di(tenantId, "631", "ДОО-Пнс,ОЗМ,Бзрб 1, 2 Рс", C, true));
        items.add(di(tenantId, "632", "ДОО-Пнс,ОЗМ,Бзрб 1, 2 Рп", C, true));
        items.add(di(tenantId, "633", "ДОО-Пнс-СР 1, 2 Рс", C, true));
        items.add(di(tenantId, "634", "ДОО-Пнс-СР 1, 2 Рп", C, true));
        items.add(di(tenantId, "635", "ДОО-Пнс 11 3 Рс", C, false));
        items.add(di(tenantId, "636", "ДОО-Пнс 11 3 Рп", C, false));
        items.add(di(tenantId, "637", "ДОО-Пнс 11 1, 2 Рс", C, false));
        items.add(di(tenantId, "638", "ДОО-Пнс 11 1, 2 Рп", C, false));
        items.add(di(tenantId, "641", "ДОО-Пнс 14 Рс", C, false));
        items.add(di(tenantId, "642", "ДОО-Пнс 14 Рп", C, false));
        items.add(di(tenantId, "643", "ДОО-Пнс,ОЗМ 25 Рс", C, false));
        items.add(di(tenantId, "644", "ДОО-Пнс,ОЗМ 25 Рп", C, false));
        items.add(di(tenantId, "645", "ДОО-Пнс 18 (Р+Л)с", C, false));
        items.add(di(tenantId, "646", "ДОО-Пнс 18 (Р+Л)п", C, false));
        items.add(di(tenantId, "647", "ДОО-Пнс 27 Рс", C, false));
        items.add(di(tenantId, "648", "ДОО-Пнс 27 Рп", C, false));
        items.add(di(tenantId, "650", "ДОО-Пнс 97 Рс", C, false));
        items.add(di(tenantId, "651", "ДОО-Пнс 97 Рп", C, false));

        items.add(di(tenantId, "655", "ДОО-ОЗМ 3 Р", C, true));
        items.add(di(tenantId, "656", "ДОО-ОЗМ 1,2 Р", C, true));
        items.add(di(tenantId, "657", "ДОО-ОЗМ 25 Р", C, false));
        items.add(di(tenantId, "661", "ДОО-Безраб. 3 Р", C, true));
        items.add(di(tenantId, "662", "ДОО-Безраб. 1, 2 Р", C, true));
        items.add(di(tenantId, "663", "ДОО-Пнс 20 (Р+Л)с", C, false));
        items.add(di(tenantId, "664", "ДОО-Пнс 20 (Р+Л)п", C, false));

        items.add(di(tenantId, "681", "ДОО+СР-УПФ Р", C, true));
        items.add(di(tenantId, "682", "ДОО-УПФ-СР Р", C, true));
        items.add(di(tenantId, "683", "ДОО+СР-УПФ 20 Р", C, false));
        items.add(di(tenantId, "684", "ДОО-УПФ 18 Р", C, false));
        items.add(di(tenantId, "685", "ДОО-ТЗПБ Р", C, true));
        items.add(di(tenantId, "687", "ДЗПО-2 кт Р", C, true));
        items.add(di(tenantId, "688", "ДЗПО-1 кт Р", C, true));

        // ── КООП работодател ──
        items.add(di(tenantId, "691", "ДОО-Пнс,ОЗМ 9 Рс КООП", C, false));
        items.add(di(tenantId, "692", "ДОО-Пнс,ОЗМ 9 Рп КООП", C, false));

        // ── Държавни служители (700-799) ──
        items.add(di(tenantId, "701", "ЗО+СР-държавни служ. Р", C, false));
        items.add(di(tenantId, "702", "ЗО-държ. служ.-СР Р", C, false));
        items.add(di(tenantId, "703", "ЗО-държ.сл.непл.отп. Р", C, false));
        items.add(di(tenantId, "704", "ЗО-държ.сл.болнични", C, false));
        items.add(di(tenantId, "721", "ДОО-Пнс,ОЗМ,Бзрб-ДС 3 Рс", C, false));
        items.add(di(tenantId, "722", "ДОО-Пнс,ОЗМ,Бзрб-ДС 3 Рп", C, false));
        items.add(di(tenantId, "725", "ДОО-Пнс-държ.сл.СР 3 Рс", C, false));
        items.add(di(tenantId, "726", "ДОО-Пнс-държ.сл.СР 3 Рп", C, false));
        items.add(di(tenantId, "731", "ДОО-Пнс,ОЗМ,Бзрб-ДС 1, 2 Рс", C, false));
        items.add(di(tenantId, "732", "ДОО-Пнс,ОЗМ,Бзрб-ДС 1, 2 Рп", C, false));
        items.add(di(tenantId, "735", "ДОО-Пнс-държ.сл.СР 1, 2 Рс", C, false));
        items.add(di(tenantId, "736", "ДОО-Пнс-държ.сл.СР 1, 2 Рп", C, false));
        items.add(di(tenantId, "751", "ДОО-ОЗМ-държ.сл. 3 Р", C, false));
        items.add(di(tenantId, "752", "ДОО-ОЗМ-държ.сл. 1, 2 Р", C, false));
        items.add(di(tenantId, "761", "ДОО-Безраб.-държ.сл. 3 Р", C, false));
        items.add(di(tenantId, "762", "ДОО-Безраб.-държ.сл. 1, 2 Р", C, false));
        items.add(di(tenantId, "781", "ДОО+СР-УПФ-държ.сл. Р", C, false));
        items.add(di(tenantId, "782", "ДОО-УПФ-държ.сл.-СР Р", C, false));
        items.add(di(tenantId, "791", "Учителски ПФ Р", C, false));

        // ── Ваучери ──
        items.add(di(tenantId, "800", "Ваучери", F, false));

        List<DeductionItem> saved = new ArrayList<>();
        repository.saveAll(items).forEach(saved::add);
        return saved;
    }

    private DeductionItem di(String tenantId, String code, String name, DeductionType type, boolean system) {
        DeductionItem item = new DeductionItem();
        item.setTenantId(tenantId);
        item.setCode(code);
        item.setName(name);
        item.setType(type);
        item.setSystem(system);
        item.setActive(true);
        return item;
    }
}
