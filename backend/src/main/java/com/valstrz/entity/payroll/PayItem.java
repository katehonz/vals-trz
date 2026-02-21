package com.valstrz.entity.payroll;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

/**
 * Перо за възнаграждение.
 * Типове: PERCENT (% от основна), PER_UNIT (лева по), FIXED (фиксирана сума), CALCULATED (изчисляемо).
 */
@Document("payItems")
public class PayItem extends BaseEntity {

    private String code;          // системен код (напр. 351001)
    private String name;          // наименование (напр. "Основно възнаграждение")
    private PayItemType type;     // тип на перото
    private boolean system;       // системно (неизтриваемо) или потребителско
    private boolean active;       // активно/неактивно

    public PayItem() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public PayItemType getType() { return type; }
    public void setType(PayItemType type) { this.type = type; }

    public boolean isSystem() { return system; }
    public void setSystem(boolean system) { this.system = system; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public enum PayItemType {
        PERCENT,     // % от (основна заплата или друго перо)
        PER_UNIT,    // лева по (бройка, час и т.н.)
        FIXED,       // зададена като сума
        CALCULATED   // изчисляемо по формула
    }
}
