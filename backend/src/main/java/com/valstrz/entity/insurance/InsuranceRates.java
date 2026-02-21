package com.valstrz.entity.insurance;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Основни осигурителни параметри за годината.
 * МРЗ, максимален осиг. доход, плосък данък, необлагаема сума.
 */
@Document("insuranceRates")
public class InsuranceRates extends BaseEntity {

    private int year;
    private BigDecimal minimumWage;           // МРЗ
    private BigDecimal maxInsurableIncome;     // максимален осигурителен доход
    private BigDecimal flatTaxRate;            // процент плосък данък (10%)
    private BigDecimal disabilityTaxExemption; // необлагаема сума за ДОД на инвалид
    private BigDecimal voluntaryDeductionPercent; // % за доброволни осиг. вноски преди ДОД
    private BigDecimal socialExpenseExemption;    // сума за соц. разходи без ДОД

    public InsuranceRates() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public BigDecimal getMinimumWage() { return minimumWage; }
    public void setMinimumWage(BigDecimal minimumWage) { this.minimumWage = minimumWage; }

    public BigDecimal getMaxInsurableIncome() { return maxInsurableIncome; }
    public void setMaxInsurableIncome(BigDecimal maxInsurableIncome) { this.maxInsurableIncome = maxInsurableIncome; }

    public BigDecimal getFlatTaxRate() { return flatTaxRate; }
    public void setFlatTaxRate(BigDecimal flatTaxRate) { this.flatTaxRate = flatTaxRate; }

    public BigDecimal getDisabilityTaxExemption() { return disabilityTaxExemption; }
    public void setDisabilityTaxExemption(BigDecimal disabilityTaxExemption) { this.disabilityTaxExemption = disabilityTaxExemption; }

    public BigDecimal getVoluntaryDeductionPercent() { return voluntaryDeductionPercent; }
    public void setVoluntaryDeductionPercent(BigDecimal voluntaryDeductionPercent) { this.voluntaryDeductionPercent = voluntaryDeductionPercent; }

    public BigDecimal getSocialExpenseExemption() { return socialExpenseExemption; }
    public void setSocialExpenseExemption(BigDecimal socialExpenseExemption) { this.socialExpenseExemption = socialExpenseExemption; }
}
