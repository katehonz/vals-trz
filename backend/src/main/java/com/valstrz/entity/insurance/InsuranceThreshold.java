package com.valstrz.entity.insurance;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Минимален осигурителен праг по група НКПД за конкретна икон. дейност.
 */
@Document("insuranceThresholds")
public class InsuranceThreshold extends BaseEntity {

    private int year;
    private String nkidCode;            // код на икономическа дейност
    private int personnelGroup;         // група персонал (1-9)
    private String nkpdCode;            // код по НКПД
    private BigDecimal minInsurableIncome; // минимален осигурителен доход

    public InsuranceThreshold() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getNkidCode() { return nkidCode; }
    public void setNkidCode(String nkidCode) { this.nkidCode = nkidCode; }

    public int getPersonnelGroup() { return personnelGroup; }
    public void setPersonnelGroup(int personnelGroup) { this.personnelGroup = personnelGroup; }

    public String getNkpdCode() { return nkpdCode; }
    public void setNkpdCode(String nkpdCode) { this.nkpdCode = nkpdCode; }

    public BigDecimal getMinInsurableIncome() { return minInsurableIncome; }
    public void setMinInsurableIncome(BigDecimal minInsurableIncome) { this.minInsurableIncome = minInsurableIncome; }
}
