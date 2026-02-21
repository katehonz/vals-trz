package com.valstrz.service;

import com.valstrz.entity.nomenclature.Nomenclature;
import com.valstrz.entity.nomenclature.Nomenclature.NomenclatureEntry;
import com.valstrz.entity.nomenclature.Nomenclature.NomenclatureType;
import com.valstrz.repository.NomenclatureRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class NomenclatureService {

    private final NomenclatureRepository repository;

    public NomenclatureService(NomenclatureRepository repository) {
        this.repository = repository;
    }

    public void importNkpd(String tenantId, MultipartFile file) throws IOException {
        List<NomenclatureEntry> entries = parseCsv(new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)));
        saveNkpd(tenantId, entries);
    }
    
    public void importNkpdFromFileSystem(String tenantId, java.io.InputStream inputStream) throws IOException {
         List<NomenclatureEntry> entries = parseCsv(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));
         saveNkpd(tenantId, entries);
    }

    private List<NomenclatureEntry> parseCsv(BufferedReader reader) throws IOException {
        List<NomenclatureEntry> entries = new ArrayList<>();
        String line;
        boolean isFirstLine = true;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            if (isFirstLine) { 
                // Skip header if looks like header
                if (line.contains("Код") || line.contains("Code") || line.contains("Наименование")) {
                     isFirstLine = false;
                     continue;
                }
                isFirstLine = false;
            }
            
            String[] parts = line.split("	");
            if (parts.length < 2) {
                 // Try comma split as fallback
                 // Simple regex split to handle quoted commas could be better but sticking to simple split for now
                 parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            }

            if (parts.length >= 2) {
                NomenclatureEntry entry = new NomenclatureEntry();
                entry.setCode(parts[0].trim());

                String name = parts[1].trim();
                // Remove surrounding quotes if present
                if (name.startsWith("\"") && name.endsWith("\"")) {
                    name = name.substring(1, name.length() - 1);
                }
                // Also handle " inside the string (CSV doubling quotes)
                name = name.replace("\"\"", "\"");
                
                entry.setValue(name);
                entry.setActive(true);
                entries.add(entry);
            }
        }
        return entries;
    }

    private void saveNkpd(String tenantId, List<NomenclatureEntry> entries) {
        Iterable<Nomenclature> existing = repository.findByTenantIdAndCode(tenantId, "NKPD");
        Nomenclature nomenclature;
        if (existing.iterator().hasNext()) {
            nomenclature = existing.iterator().next();
        } else {
            nomenclature = new Nomenclature();
            nomenclature.setTenantId(tenantId);
            nomenclature.setCode("NKPD");
            nomenclature.setName("Национална класификация на професиите и длъжностите");
            nomenclature.setType(NomenclatureType.SYSTEM);
        }
        
        nomenclature.setEntries(entries);
        repository.save(nomenclature);
    }
}
