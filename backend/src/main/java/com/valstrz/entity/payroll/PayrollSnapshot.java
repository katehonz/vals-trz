package com.valstrz.entity.payroll;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Пълен snapshot на заплатата за един служител за един месец.
 *
 * Философия: Когато месецът се "затвори", този документ съдържа ВСИЧКО
 * необходимо за да се разбере как е изчислена заплатата - независимо от
 * бъдещи промени в законодателството, осигурителни ставки или настройки.
 *
 * Един JSON = пълна истина за този месец, завинаги.
 */
@Document("payrollSnapshots")
public class PayrollSnapshot extends BaseEntity {

    // === ИДЕНТИФИКАЦИЯ ===
    private String employeeId;
    private int year;
    private int month;
    private LocalDateTime calculatedAt;      // кога е изчислено
    private LocalDateTime closedAt;          // кога е затворен месецът
    private String status;                   // OPEN, CALCULATED, CLOSED, CORRECTED

    // === SNAPSHOT НА СЛУЖИТЕЛЯ (към момента на изчислението) ===
    private Map<String, Object> employeeData;  // лични + служебни данни
    // Включва: име, ЕГН, длъжност, НКПД, дата на постъпване, вид ТД,
    // основно възнаграждение, вид осигуряване, часова схема, отдел, и др.

    // === SNAPSHOT НА ПАРАМЕТРИТЕ (законодателство + фирмени настройки) ===
    private Map<String, Object> legislationParams;
    // Включва:
    // - МРЗ, максимален осиг. доход
    // - плосък данък %
    // - осигурителни проценти (ДОО, ДЗПО, ЗО) - работодател + работник
    // - мин. осиг. праг за тази длъжност/НКПД
    // - процент ДТВ за ТСПО
    // - работни дни/часове в месеца
    // - часова схема (часове/ден)

    // === ВХОДНИ ДАННИ (какво е отработено) ===
    private Map<String, Object> timesheetData;
    // Включва:
    // - отработени дни и часове
    // - извънреден труд (работни дни, почивни, празници)
    // - нощен труд часове
    // - отсъствия (вид, от-до, дни)
    // - дни в болничен (за сметка на работодател / НОИ)

    // === ИЗЧИСЛЕНИЕ: НАЧИСЛЕНИЯ ===
    private List<PayrollLine> earnings;
    // Всяко перо: код, име, база, количество/процент, сума

    // === ИЗЧИСЛЕНИЕ: УДРЪЖКИ ===
    private List<PayrollLine> deductions;
    // ДОО работник, ДЗПО работник, ЗО работник, ДОД, аванс, запори...

    // === ОСИГУРОВКИ ЗА СМЕТКА НА РАБОТОДАТЕЛЯ ===
    private List<PayrollLine> employerContributions;
    // ДОО работодател, ДЗПО работодател, ЗО работодател, ТЗПБ

    // === ОБОБЩЕНИ СУМИ ===
    private BigDecimal grossSalary;            // брутна заплата
    private BigDecimal insurableIncome;        // осигурителен доход
    private BigDecimal totalEmployeeInsurance;  // общо осигуровки работник
    private BigDecimal taxBase;                // данъчна основа
    private BigDecimal incomeTax;              // ДОД
    private BigDecimal totalDeductions;        // общо удръжки
    private BigDecimal netSalary;              // нето за получаване
    private BigDecimal totalEmployerCost;      // общ разход за работодателя
    private BigDecimal totalEmployerInsurance;  // осигуровки работодател

    public PayrollSnapshot() {}

    // === Getters/Setters ===

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Map<String, Object> getEmployeeData() { return employeeData; }
    public void setEmployeeData(Map<String, Object> employeeData) { this.employeeData = employeeData; }

    public Map<String, Object> getLegislationParams() { return legislationParams; }
    public void setLegislationParams(Map<String, Object> legislationParams) { this.legislationParams = legislationParams; }

    public Map<String, Object> getTimesheetData() { return timesheetData; }
    public void setTimesheetData(Map<String, Object> timesheetData) { this.timesheetData = timesheetData; }

    public List<PayrollLine> getEarnings() { return earnings; }
    public void setEarnings(List<PayrollLine> earnings) { this.earnings = earnings; }

    public List<PayrollLine> getDeductions() { return deductions; }
    public void setDeductions(List<PayrollLine> deductions) { this.deductions = deductions; }

    public List<PayrollLine> getEmployerContributions() { return employerContributions; }
    public void setEmployerContributions(List<PayrollLine> employerContributions) { this.employerContributions = employerContributions; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }

    public BigDecimal getInsurableIncome() { return insurableIncome; }
    public void setInsurableIncome(BigDecimal insurableIncome) { this.insurableIncome = insurableIncome; }

    public BigDecimal getTotalEmployeeInsurance() { return totalEmployeeInsurance; }
    public void setTotalEmployeeInsurance(BigDecimal totalEmployeeInsurance) { this.totalEmployeeInsurance = totalEmployeeInsurance; }

    public BigDecimal getTaxBase() { return taxBase; }
    public void setTaxBase(BigDecimal taxBase) { this.taxBase = taxBase; }

    public BigDecimal getIncomeTax() { return incomeTax; }
    public void setIncomeTax(BigDecimal incomeTax) { this.incomeTax = incomeTax; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }

    public BigDecimal getTotalEmployerCost() { return totalEmployerCost; }
    public void setTotalEmployerCost(BigDecimal totalEmployerCost) { this.totalEmployerCost = totalEmployerCost; }

    public BigDecimal getTotalEmployerInsurance() { return totalEmployerInsurance; }
    public void setTotalEmployerInsurance(BigDecimal totalEmployerInsurance) { this.totalEmployerInsurance = totalEmployerInsurance; }

    /**
     * Един ред от изчислението - перо за начисление, удръжка или осигуровка.
     */
    public static class PayrollLine {
        private String code;          // код на перото
        private String name;          // наименование
        private String type;          // тип (PERCENT, FIXED, CALCULATED...)
        private BigDecimal base;      // база за изчисление
        private BigDecimal rate;      // процент или коефициент
        private BigDecimal quantity;  // количество (дни, часове)
        private BigDecimal amount;    // крайна сума
        private Map<String, String> metadata; // допълнителна информация (напр. garnishmentId)

        public PayrollLine() {}

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public BigDecimal getBase() { return base; }
        public void setBase(BigDecimal base) { this.base = base; }

        public BigDecimal getRate() { return rate; }
        public void setRate(BigDecimal rate) { this.rate = rate; }

        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }

        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }
}
