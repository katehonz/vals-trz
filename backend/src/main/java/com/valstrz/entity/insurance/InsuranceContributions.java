package com.valstrz.entity.insurance;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * Осигурителни вноски - проценти за ДОО, ДЗПО, ЗО.
 * Разделени за родените преди 1960 и след 1960, работодател и работник.
 */
@Document("insuranceContributions")
public class InsuranceContributions extends BaseEntity {

    private int year;
    private String category;  // "before1960" или "after1960"
    private String insuredType = "01"; // вид осигурен: 01, 02, 03, 05, 08, 10, 12, 14, 24, 27

    // ДОО - фонд Пенсии
    private BigDecimal pensionEmployer;
    private BigDecimal pensionEmployee;

    // ДОО - фонд Общо заболяване и майчинство
    private BigDecimal sicknessEmployer;
    private BigDecimal sicknessEmployee;

    // ДОО - фонд Безработица
    private BigDecimal unemploymentEmployer;
    private BigDecimal unemploymentEmployee;

    // ДЗПО - Универсален пенсионен фонд (само за след 1960)
    private BigDecimal supplementaryPensionEmployer;
    private BigDecimal supplementaryPensionEmployee;

    // Здравно осигуряване
    private BigDecimal healthEmployer;
    private BigDecimal healthEmployee;

    // Фонд ТЗПБ (трудова злополука и проф. болест) - само работодател
    private BigDecimal workAccidentEmployer;

    // Професионален пенсионен фонд (ППФ) - за 1-ва и 2-ра категория, само работодател
    private BigDecimal professionalPensionEmployer;

    // Учителски пенсионен фонд - за вид осигурен 08, само работодател
    private BigDecimal teacherPensionEmployer;

    public InsuranceContributions() {}

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getInsuredType() { return insuredType; }
    public void setInsuredType(String insuredType) { this.insuredType = insuredType; }

    public BigDecimal getPensionEmployer() { return pensionEmployer; }
    public void setPensionEmployer(BigDecimal pensionEmployer) { this.pensionEmployer = pensionEmployer; }

    public BigDecimal getPensionEmployee() { return pensionEmployee; }
    public void setPensionEmployee(BigDecimal pensionEmployee) { this.pensionEmployee = pensionEmployee; }

    public BigDecimal getSicknessEmployer() { return sicknessEmployer; }
    public void setSicknessEmployer(BigDecimal sicknessEmployer) { this.sicknessEmployer = sicknessEmployer; }

    public BigDecimal getSicknessEmployee() { return sicknessEmployee; }
    public void setSicknessEmployee(BigDecimal sicknessEmployee) { this.sicknessEmployee = sicknessEmployee; }

    public BigDecimal getUnemploymentEmployer() { return unemploymentEmployer; }
    public void setUnemploymentEmployer(BigDecimal unemploymentEmployer) { this.unemploymentEmployer = unemploymentEmployer; }

    public BigDecimal getUnemploymentEmployee() { return unemploymentEmployee; }
    public void setUnemploymentEmployee(BigDecimal unemploymentEmployee) { this.unemploymentEmployee = unemploymentEmployee; }

    public BigDecimal getSupplementaryPensionEmployer() { return supplementaryPensionEmployer; }
    public void setSupplementaryPensionEmployer(BigDecimal supplementaryPensionEmployer) { this.supplementaryPensionEmployer = supplementaryPensionEmployer; }

    public BigDecimal getSupplementaryPensionEmployee() { return supplementaryPensionEmployee; }
    public void setSupplementaryPensionEmployee(BigDecimal supplementaryPensionEmployee) { this.supplementaryPensionEmployee = supplementaryPensionEmployee; }

    public BigDecimal getHealthEmployer() { return healthEmployer; }
    public void setHealthEmployer(BigDecimal healthEmployer) { this.healthEmployer = healthEmployer; }

    public BigDecimal getHealthEmployee() { return healthEmployee; }
    public void setHealthEmployee(BigDecimal healthEmployee) { this.healthEmployee = healthEmployee; }

    public BigDecimal getWorkAccidentEmployer() { return workAccidentEmployer; }
    public void setWorkAccidentEmployer(BigDecimal workAccidentEmployer) { this.workAccidentEmployer = workAccidentEmployer; }

    public BigDecimal getProfessionalPensionEmployer() { return professionalPensionEmployer; }
    public void setProfessionalPensionEmployer(BigDecimal professionalPensionEmployer) { this.professionalPensionEmployer = professionalPensionEmployer; }

    public BigDecimal getTeacherPensionEmployer() { return teacherPensionEmployer; }
    public void setTeacherPensionEmployer(BigDecimal teacherPensionEmployer) { this.teacherPensionEmployer = teacherPensionEmployer; }
}
