package com.valstrz.entity.nomenclature;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.util.List;

/**
 * Номенклатура (класификатор) - служебна или потребителска.
 * Примери: населени места, банки, основания за ТД, видове отпуск.
 */
@Document("nomenclatures")
public class Nomenclature extends BaseEntity {

    private String code;           // уникален код на номенклатурата
    private String name;           // наименование
    private NomenclatureType type; // служебна или потребителска
    private List<NomenclatureEntry> entries;

    public Nomenclature() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public NomenclatureType getType() { return type; }
    public void setType(NomenclatureType type) { this.type = type; }

    public List<NomenclatureEntry> getEntries() { return entries; }
    public void setEntries(List<NomenclatureEntry> entries) { this.entries = entries; }

    public enum NomenclatureType {
        SYSTEM,   // служебна (предефинирана)
        USER      // потребителска
    }

    public static class NomenclatureEntry {
        private String code;
        private String value;
        private String description;
        private boolean active;

        public NomenclatureEntry() {}

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }
}
