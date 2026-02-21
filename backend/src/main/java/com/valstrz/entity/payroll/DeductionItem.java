package com.valstrz.entity.payroll;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

/**
 * Перо за удръжка от заплата.
 * Типове: FIXED (фиксирана сума), CALCULATED (изчисляемо - напр. ДОД, осигуровки).
 */
@Document("deductionItems")
public class DeductionItem extends BaseEntity {

    private String code;              // системен код (напр. 351982 за ДОД)
    private String name;              // наименование
    private DeductionType type;       // тип
    private boolean system;           // системно перо
    private boolean active;

    public DeductionItem() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DeductionType getType() { return type; }
    public void setType(DeductionType type) { this.type = type; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public enum DeductionType {
        FIXED,       // фиксирана сума
        CALCULATED   // изчисляемо
    }
}
