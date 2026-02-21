package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Трудово правоотношение - трудов договор и текущи служебни данни.
 * При ДС (допълнително споразумение) - старите данни остават в Amendment history,
 * а тук се обновяват текущите.
 */
@Document("employments")
public class Employment extends BaseEntity {

    private String employeeId;
    private String departmentId;

    // Трудов договор
    private String contractNumber;         // номер на ТД
    private LocalDate contractDate;        // дата на ТД
    private LocalDate startDate;           // дата на постъпване
    private String contractBasis;          // основание (код по КТ, напр. чл.67)
    private String contractType;           // вид ТД (безсрочен, срочен, допълнителен)
    private LocalDate contractEndDate;     // до дата (за срочни)
    private String contractSpecificText;   // специфичен текст

    // Длъжност
    private String jobTitle;               // длъжност (текст)
    private String nkpdCode;               // код по НКПД
    private String kidCode;                // код по КИД

    // Работно място и условия
    private String workplace;              // месторабота
    private String workPhone;              // служебен телефон
    private String workTimeType;           // вид работно време (пълно, непълно, ненормирано)
    private String workScheduleCode;       // код на часова схема
    private String insuranceType;          // вид осигурителна вноска
    private String insuredType;            // вид осигурен

    // Възнаграждение
    private BigDecimal baseSalary;         // основно месечно възнаграждение
    private String paymentType;            // вид заплащане (месечно, почасово, сделно)
    private String workCardType;           // вид работна карта (за сделно)

    // Стаж
    private BigDecimal previousExperienceYears;  // стаж до постъпване (години)
    private BigDecimal seniorityBonusYears;       // стаж за ДТВ за ТСПО
    private BigDecimal seniorityBonusPercent;     // текущ % за ТСПО

    // Персонал категория
    private int personnelGroup;            // група персонал (1-9 по НКПД)
    private String personnelType;          // вид персонал

    // Работно време - часове
    private BigDecimal workingHoursPerDay; // работни часове на ден (напр. 4 при непълно)

    // Срок на предизвестие
    private int noticePeriodDays;          // срок на предизвестие (дни)

    // Отпуски (по договор)
    private int basicAnnualLeaveDays;      // основен платен годишен отпуск (дни)
    private int additionalAnnualLeaveDays; // допълнителен платен годишен отпуск (дни)

    // Допълнително възнаграждение
    private String additionalPayItemCode;  // перо за доп. възнаграждение
    private String additionalPayItemName;  // наименование на доп. възнаграждение
    private BigDecimal additionalPayAmount;// сума на доп. възнаграждение

    // Флагове
    private boolean pensioner;             // пенсионер
    private boolean disability50Plus;      // лице с инвалидност > 50%
    private boolean zgvrsArticle7;         // по чл.7 от ЗГВРС
    private LocalDate zgvrsFromDate;

    // Свободни текстови полета (условия на ТД)
    private String text1;
    private String text2;
    private String text3;
    private String text4;

    // Статус
    private boolean current;               // текущо (активно) правоотношение
    private LocalDate terminationDate;     // дата на прекратяване (ако е прекратено)

    public Employment() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public String getContractBasis() { return contractBasis; }
    public void setContractBasis(String contractBasis) { this.contractBasis = contractBasis; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public LocalDate getContractEndDate() { return contractEndDate; }
    public void setContractEndDate(LocalDate contractEndDate) { this.contractEndDate = contractEndDate; }

    public String getContractSpecificText() { return contractSpecificText; }
    public void setContractSpecificText(String contractSpecificText) { this.contractSpecificText = contractSpecificText; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getNkpdCode() { return nkpdCode; }
    public void setNkpdCode(String nkpdCode) { this.nkpdCode = nkpdCode; }

    public String getKidCode() { return kidCode; }
    public void setKidCode(String kidCode) { this.kidCode = kidCode; }

    public String getWorkplace() { return workplace; }
    public void setWorkplace(String workplace) { this.workplace = workplace; }

    public String getWorkPhone() { return workPhone; }
    public void setWorkPhone(String workPhone) { this.workPhone = workPhone; }

    public String getWorkTimeType() { return workTimeType; }
    public void setWorkTimeType(String workTimeType) { this.workTimeType = workTimeType; }

    public String getWorkScheduleCode() { return workScheduleCode; }
    public void setWorkScheduleCode(String workScheduleCode) { this.workScheduleCode = workScheduleCode; }

    public String getInsuranceType() { return insuranceType; }
    public void setInsuranceType(String insuranceType) { this.insuranceType = insuranceType; }

    public String getInsuredType() { return insuredType; }
    public void setInsuredType(String insuredType) { this.insuredType = insuredType; }

    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getWorkCardType() { return workCardType; }
    public void setWorkCardType(String workCardType) { this.workCardType = workCardType; }

    public BigDecimal getPreviousExperienceYears() { return previousExperienceYears; }
    public void setPreviousExperienceYears(BigDecimal previousExperienceYears) { this.previousExperienceYears = previousExperienceYears; }

    public BigDecimal getSeniorityBonusYears() { return seniorityBonusYears; }
    public void setSeniorityBonusYears(BigDecimal seniorityBonusYears) { this.seniorityBonusYears = seniorityBonusYears; }

    public BigDecimal getSeniorityBonusPercent() { return seniorityBonusPercent; }
    public void setSeniorityBonusPercent(BigDecimal seniorityBonusPercent) { this.seniorityBonusPercent = seniorityBonusPercent; }

    public int getPersonnelGroup() { return personnelGroup; }
    public void setPersonnelGroup(int personnelGroup) { this.personnelGroup = personnelGroup; }

    public String getPersonnelType() { return personnelType; }
    public void setPersonnelType(String personnelType) { this.personnelType = personnelType; }

    public BigDecimal getWorkingHoursPerDay() { return workingHoursPerDay; }
    public void setWorkingHoursPerDay(BigDecimal workingHoursPerDay) { this.workingHoursPerDay = workingHoursPerDay; }

    public int getNoticePeriodDays() { return noticePeriodDays; }
    public void setNoticePeriodDays(int noticePeriodDays) { this.noticePeriodDays = noticePeriodDays; }

    public int getBasicAnnualLeaveDays() { return basicAnnualLeaveDays; }
    public void setBasicAnnualLeaveDays(int basicAnnualLeaveDays) { this.basicAnnualLeaveDays = basicAnnualLeaveDays; }

    public int getAdditionalAnnualLeaveDays() { return additionalAnnualLeaveDays; }
    public void setAdditionalAnnualLeaveDays(int additionalAnnualLeaveDays) { this.additionalAnnualLeaveDays = additionalAnnualLeaveDays; }

    public String getAdditionalPayItemCode() { return additionalPayItemCode; }
    public void setAdditionalPayItemCode(String additionalPayItemCode) { this.additionalPayItemCode = additionalPayItemCode; }

    public String getAdditionalPayItemName() { return additionalPayItemName; }
    public void setAdditionalPayItemName(String additionalPayItemName) { this.additionalPayItemName = additionalPayItemName; }

    public BigDecimal getAdditionalPayAmount() { return additionalPayAmount; }
    public void setAdditionalPayAmount(BigDecimal additionalPayAmount) { this.additionalPayAmount = additionalPayAmount; }

    public boolean isPensioner() { return pensioner; }
    public void setPensioner(boolean pensioner) { this.pensioner = pensioner; }

    public boolean isDisability50Plus() { return disability50Plus; }
    public void setDisability50Plus(boolean disability50Plus) { this.disability50Plus = disability50Plus; }

    public boolean isZgvrsArticle7() { return zgvrsArticle7; }
    public void setZgvrsArticle7(boolean zgvrsArticle7) { this.zgvrsArticle7 = zgvrsArticle7; }

    public LocalDate getZgvrsFromDate() { return zgvrsFromDate; }
    public void setZgvrsFromDate(LocalDate zgvrsFromDate) { this.zgvrsFromDate = zgvrsFromDate; }

    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }

    public String getText1() { return text1; }
    public void setText1(String text1) { this.text1 = text1; }

    public String getText2() { return text2; }
    public void setText2(String text2) { this.text2 = text2; }

    public String getText3() { return text3; }
    public void setText3(String text3) { this.text3 = text3; }

    public String getText4() { return text4; }
    public void setText4(String text4) { this.text4 = text4; }

    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }
}
