package com.valstrz.entity.personnel;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

/**
 * Полагаеми отпуски на служител за година.
 * По чл. 155, 156 от КТ и вътрешни фирмени правила.
 */
@Document("leaveEntitlements")
public class LeaveEntitlement extends BaseEntity {

    private String employeeId;
    private int year;

    // По чл. 155 КТ - основен платен годишен отпуск
    private int basicLeaveDays;            // полагаеми
    private int basicLeaveUsed;            // използвани
    private int basicLeaveRemaining;       // остатък

    // По чл. 156 ал.1 - допълнителен (вредни условия)
    private int additionalLeaveDays;
    private int additionalLeaveUsed;

    // По чл. 156 ал.2 - ненормиран работен ден
    private int irregularLeaveDays;
    private int irregularLeaveUsed;

    // По чл. 156а - по споразумение м/у страните
    private int agreedLeaveDays;
    private int agreedLeaveUsed;

    // Прехвърлени от предходна година
    private int carriedOverDays;

    public LeaveEntitlement() {}

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getBasicLeaveDays() { return basicLeaveDays; }
    public void setBasicLeaveDays(int basicLeaveDays) { this.basicLeaveDays = basicLeaveDays; }

    public int getBasicLeaveUsed() { return basicLeaveUsed; }
    public void setBasicLeaveUsed(int basicLeaveUsed) { this.basicLeaveUsed = basicLeaveUsed; }

    public int getBasicLeaveRemaining() { return basicLeaveRemaining; }
    public void setBasicLeaveRemaining(int basicLeaveRemaining) { this.basicLeaveRemaining = basicLeaveRemaining; }

    public int getAdditionalLeaveDays() { return additionalLeaveDays; }
    public void setAdditionalLeaveDays(int additionalLeaveDays) { this.additionalLeaveDays = additionalLeaveDays; }

    public int getAdditionalLeaveUsed() { return additionalLeaveUsed; }
    public void setAdditionalLeaveUsed(int additionalLeaveUsed) { this.additionalLeaveUsed = additionalLeaveUsed; }

    public int getIrregularLeaveDays() { return irregularLeaveDays; }
    public void setIrregularLeaveDays(int irregularLeaveDays) { this.irregularLeaveDays = irregularLeaveDays; }

    public int getIrregularLeaveUsed() { return irregularLeaveUsed; }
    public void setIrregularLeaveUsed(int irregularLeaveUsed) { this.irregularLeaveUsed = irregularLeaveUsed; }

    public int getAgreedLeaveDays() { return agreedLeaveDays; }
    public void setAgreedLeaveDays(int agreedLeaveDays) { this.agreedLeaveDays = agreedLeaveDays; }

    public int getAgreedLeaveUsed() { return agreedLeaveUsed; }
    public void setAgreedLeaveUsed(int agreedLeaveUsed) { this.agreedLeaveUsed = agreedLeaveUsed; }

    public int getCarriedOverDays() { return carriedOverDays; }
    public void setCarriedOverDays(int carriedOverDays) { this.carriedOverDays = carriedOverDays; }

    public int getTotalEntitled() {
        return basicLeaveDays + additionalLeaveDays + irregularLeaveDays + agreedLeaveDays + carriedOverDays;
    }

    public int getTotalUsed() {
        return basicLeaveUsed + additionalLeaveUsed + irregularLeaveUsed + agreedLeaveUsed;
    }
}
