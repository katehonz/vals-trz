package com.valstrz.entity.nomenclature;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Икономическа дейност (КИД) и свързаните с нея параметри (ТЗПБ).
 * Съответства на файл mod.csv.
 */
@Document("economicActivities")
public class EconomicActivity extends BaseEntity {

    private String code;          // KID Code (e.g., "01")
    private String name;          // Activity Name
    private BigDecimal modAmount; // Minimum Insurable Income base
    private BigDecimal tzpbPercent; // TZPB Percentage (e.g., 0.9)
    private int year;             // Valid for year
    private boolean active;       // Дали фирмата реално извършва тази дейност

    public EconomicActivity() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getModAmount() {
        return modAmount;
    }

    public void setModAmount(BigDecimal modAmount) {
        this.modAmount = modAmount;
    }

    public BigDecimal getTzpbPercent() {
        return tzpbPercent;
    }

    public void setTzpbPercent(BigDecimal tzpbPercent) {
        this.tzpbPercent = tzpbPercent;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
