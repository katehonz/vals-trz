package com.valstrz.entity.company;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Вид персонал с минимален осигурителен доход по НКПД група.
 * Напр.: 1-Ръководители, 2-Специалисти, 3-Техници и приложни специалисти и т.н.
 */
@Document("personnelTypes")
public class PersonnelType extends BaseEntity {

    private int number;              // пореден номер (1-9)
    private String name;             // наименование
    private String nkpdCode;         // код по НКПД
    private BigDecimal minInsurableIncome; // мин. осигурителен доход

    public PersonnelType() {}

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNkpdCode() { return nkpdCode; }
    public void setNkpdCode(String nkpdCode) { this.nkpdCode = nkpdCode; }

    public BigDecimal getMinInsurableIncome() { return minInsurableIncome; }
    public void setMinInsurableIncome(BigDecimal minInsurableIncome) { this.minInsurableIncome = minInsurableIncome; }
}
