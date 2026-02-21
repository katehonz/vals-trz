package com.valstrz.service;

import com.valstrz.entity.company.SeniorityBonusConfig;
import com.valstrz.entity.insurance.InsuranceContributions;
import com.valstrz.entity.insurance.InsuranceRates;
import com.valstrz.entity.insurance.InsuranceThreshold;
import com.valstrz.entity.payroll.MonthClosingSnapshot;
import com.valstrz.entity.payroll.Payroll;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.repository.*;
import com.valstrz.util.MoneyUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MonthClosingService {

    private final PayrollService payrollService;
    private final PayrollRepository payrollRepository;
    private final PayrollSnapshotRepository snapshotRepository;
    private final MonthClosingSnapshotRepository closingRepository;
    private final CompanyRepository companyRepository;
    private final InsuranceRatesRepository ratesRepository;
    private final InsuranceContributionsRepository contributionsRepository;
    private final InsuranceThresholdRepository thresholdRepository;
    private final MonthlyCalendarRepository calendarRepository;
    private final PayItemRepository payItemRepository;
    private final DeductionItemRepository deductionItemRepository;
    private final EmployeeRepository employeeRepository;
    private final EmploymentRepository employmentRepository;
    private final SeniorityBonusConfigRepository seniorityConfigRepository;
    private final GarnishmentRepository garnishmentRepository;
    private final MonthlyCalendarService monthlyCalendarService;
    private final MonthlyTimesheetService monthlyTimesheetService;
    private final AuditService auditService;

    public MonthClosingService(PayrollService payrollService,
                                PayrollRepository payrollRepository,
                                PayrollSnapshotRepository snapshotRepository,
                                MonthClosingSnapshotRepository closingRepository,
                                CompanyRepository companyRepository,
                                InsuranceRatesRepository ratesRepository,
                                InsuranceContributionsRepository contributionsRepository,
                                InsuranceThresholdRepository thresholdRepository,
                                MonthlyCalendarRepository calendarRepository,
                                PayItemRepository payItemRepository,
                                DeductionItemRepository deductionItemRepository,
                                EmployeeRepository employeeRepository,
                                EmploymentRepository employmentRepository,
                                SeniorityBonusConfigRepository seniorityConfigRepository,
                                GarnishmentRepository garnishmentRepository,
                                MonthlyCalendarService monthlyCalendarService,
                                MonthlyTimesheetService monthlyTimesheetService,
                                AuditService auditService) {
        this.payrollService = payrollService;
        this.payrollRepository = payrollRepository;
        this.snapshotRepository = snapshotRepository;
        this.closingRepository = closingRepository;
        this.companyRepository = companyRepository;
        this.ratesRepository = ratesRepository;
        this.contributionsRepository = contributionsRepository;
        this.thresholdRepository = thresholdRepository;
        this.calendarRepository = calendarRepository;
        this.payItemRepository = payItemRepository;
        this.deductionItemRepository = deductionItemRepository;
        this.employeeRepository = employeeRepository;
        this.employmentRepository = employmentRepository;
        this.seniorityConfigRepository = seniorityConfigRepository;
        this.garnishmentRepository = garnishmentRepository;
        this.monthlyCalendarService = monthlyCalendarService;
        this.monthlyTimesheetService = monthlyTimesheetService;
        this.auditService = auditService;
    }

    /**
     * Подготвя нов месец: генерира календар и празни присъствени форми (timesheets)
     * за всички активни служители.
     */
    public void startNewMonth(String tenantId, int year, int month) {
        // 1. Генерираме календар
        monthlyCalendarService.generateCalendar(tenantId, year, month);

        // 2. Намираме всички активни служители
        Iterable<Employee> activeEmployees = employeeRepository.findByTenantIdAndActive(tenantId, true);
        
        // 3. Създаваме празни timesheets
        for (Employee emp : activeEmployees) {
            monthlyTimesheetService.getOrCreateTimesheet(tenantId, emp.getId(), year, month);
        }

        // 4. Създаваме запис за Payroll
        payrollService.getOrCreatePayroll(tenantId, year, month);

        auditService.log(tenantId, "MONTH_START", "Payroll",
                year + "/" + month, "Подготвен месец " + month + "/" + year, null);
    }

    public boolean isMonthClosed(String tenantId, int year, int month) {
        Iterable<MonthClosingSnapshot> existing = closingRepository.findByTenantIdAndYearAndMonth(tenantId, year, month);
        return existing.iterator().hasNext();
    }

    /**
     * Затваря месец: изчислява всички служители (ако не е направено),
     * създава MonthClosingSnapshot, маркира всичко като CLOSED.
     */
    public MonthClosingSnapshot closeMonth(String tenantId, int year, int month) {
        if (isMonthClosed(tenantId, year, month)) {
            throw new IllegalStateException("Месец " + year + "/" + month + " вече е затворен.");
        }

        // Изчисляваме (или преизчисляваме)
        List<PayrollSnapshot> snapshots = payrollService.calculateAll(tenantId, year, month);

        // Маркираме snapshot-ите като CLOSED и обновяваме запорите
        LocalDateTime now = LocalDateTime.now();
        for (PayrollSnapshot s : snapshots) {
            s.setStatus("CLOSED");
            s.setClosedAt(now);
            snapshotRepository.save(s);
            
            // Обновяване на платената сума по запорите
            if (s.getDeductions() != null) {
                for (PayrollSnapshot.PayrollLine line : s.getDeductions()) {
                    if (line.getMetadata() != null && line.getMetadata().containsKey("garnishmentId")) {
                        String gId = line.getMetadata().get("garnishmentId");
                        garnishmentRepository.findById(gId).ifPresent(g -> {
                            g.setPaidAmount(MoneyUtil.add(g.getPaidAmount(), line.getAmount()));
                            // Ако е изплатен изцяло, го деактивираме
                            if (g.getTotalAmount() != null && g.getPaidAmount().compareTo(g.getTotalAmount()) >= 0) {
                                g.setActive(false);
                            }
                            garnishmentRepository.save(g);
                        });
                    }
                }
            }
        }

        // Обновяваме Payroll статуса
        Payroll payroll = payrollService.getOrCreatePayroll(tenantId, year, month);
        payroll.setStatus("CLOSED");
        payroll.setClosedAt(now);
        payrollRepository.save(payroll);

        // Създаваме MonthClosingSnapshot
        MonthClosingSnapshot closing = buildClosingSnapshot(tenantId, year, month, snapshots);
        closing.setClosedAt(now);
        MonthClosingSnapshot saved = closingRepository.save(closing);

        // Автоматично обновяване на ДТВ за ТСПО
        updateSeniorityBonuses(tenantId, year, month);

        auditService.log(tenantId, "MONTH_CLOSE", "MonthClosingSnapshot",
                saved.getId(), "Затворен месец " + month + "/" + year,
                Map.of("employeeCount", snapshots.size()));

        return saved;
    }

    /**
     * Отваря затворен месец (admin).
     */
    public void reopenMonth(String tenantId, int year, int month) {
        // Изтриваме MonthClosingSnapshot
        Iterable<MonthClosingSnapshot> closings = closingRepository.findByTenantIdAndYearAndMonth(tenantId, year, month);
        closingRepository.deleteAll(closings);

        // Връщаме snapshot-ите на CALCULATED и възстановяваме сумите по запорите
        Iterable<PayrollSnapshot> snapshots = snapshotRepository.findByTenantIdAndYearAndMonth(tenantId, year, month);
        for (PayrollSnapshot s : snapshots) {
            s.setStatus("CALCULATED");
            s.setClosedAt(null);
            snapshotRepository.save(s);
            
            // Връщаме сумите по запорите
            if (s.getDeductions() != null) {
                for (PayrollSnapshot.PayrollLine line : s.getDeductions()) {
                    if (line.getMetadata() != null && line.getMetadata().containsKey("garnishmentId")) {
                        String gId = line.getMetadata().get("garnishmentId");
                        garnishmentRepository.findById(gId).ifPresent(g -> {
                            g.setPaidAmount(MoneyUtil.subtract(g.getPaidAmount(), line.getAmount()));
                            // Ако е бил деактивиран поради изплащане, го активираме отново
                            if (g.getTotalAmount() != null && g.getPaidAmount().compareTo(g.getTotalAmount()) < 0) {
                                g.setActive(true);
                            }
                            garnishmentRepository.save(g);
                        });
                    }
                }
            }
        }

        // Обновяваме Payroll статуса
        Payroll payroll = payrollService.getOrCreatePayroll(tenantId, year, month);
        payroll.setStatus("CALCULATED");
        payroll.setClosedAt(null);
        payrollRepository.save(payroll);

        auditService.log(tenantId, "MONTH_REOPEN", "Payroll",
                year + "/" + month, "Отворен отново месец " + month + "/" + year, null);
    }

    /**
     * Обновява ДТВ за ТСПО на всички активни служители спрямо SeniorityBonusConfig.
     * Изчислява общия стаж = previousExperienceYears + години от startDate до края на месеца.
     * Ако стажът е увеличен, обновява seniorityBonusPercent по brackets таблицата.
     */
    private void updateSeniorityBonuses(String tenantId, int year, int month) {
        SeniorityBonusConfig config = null;
        for (SeniorityBonusConfig c : seniorityConfigRepository.findByTenantId(tenantId)) {
            config = c;
            break;
        }
        if (config == null || !config.isAutoUpdateOnMonthClose()) return;

        LocalDate monthEnd = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
        BigDecimal percentPerYear = config.getPercentPerYear();
        List<SeniorityBonusConfig.SeniorityBracket> brackets = config.getBrackets();

        Iterable<Employee> employees = employeeRepository.findByTenantIdAndActive(tenantId, true);
        for (Employee emp : employees) {
            Iterable<Employment> empls = employmentRepository.findByTenantIdAndEmployeeIdAndCurrent(
                    tenantId, emp.getId(), true);
            for (Employment empl : empls) {
                if (empl.getStartDate() == null) continue;

                // Общ стаж = previousExperience + текущ стаж
                BigDecimal prevYears = empl.getPreviousExperienceYears() != null
                        ? empl.getPreviousExperienceYears() : BigDecimal.ZERO;
                long daysSinceStart = ChronoUnit.DAYS.between(empl.getStartDate(), monthEnd);
                BigDecimal currentYears = BigDecimal.valueOf(daysSinceStart)
                        .divide(BigDecimal.valueOf(365.25), 6, RoundingMode.HALF_UP);
                BigDecimal totalYears = prevYears.add(currentYears);

                // Определяне на процент
                BigDecimal newPercent = BigDecimal.ZERO;
                if (brackets != null && !brackets.isEmpty()) {
                    for (SeniorityBonusConfig.SeniorityBracket b : brackets) {
                        if (totalYears.compareTo(b.getFromYears()) >= 0
                                && (b.getToYears() == null || totalYears.compareTo(b.getToYears()) <= 0)) {
                            newPercent = b.getPercent();
                        }
                    }
                } else if (percentPerYear != null) {
                    // Просто: totalYears * percentPerYear
                    newPercent = totalYears.setScale(0, RoundingMode.DOWN)
                            .multiply(percentPerYear)
                            .setScale(2, RoundingMode.HALF_UP);
                }

                // Обновяваме само ако има промяна
                BigDecimal oldPercent = empl.getSeniorityBonusPercent() != null
                        ? empl.getSeniorityBonusPercent() : BigDecimal.ZERO;
                if (newPercent.compareTo(oldPercent) != 0) {
                    empl.setSeniorityBonusYears(totalYears.setScale(2, RoundingMode.HALF_UP));
                    empl.setSeniorityBonusPercent(newPercent);
                    employmentRepository.save(empl);
                }
            }
        }
    }

    private MonthClosingSnapshot buildClosingSnapshot(String tenantId, int year, int month,
                                                        List<PayrollSnapshot> snapshots) {
        MonthClosingSnapshot closing = new MonthClosingSnapshot();
        closing.setTenantId(tenantId);
        closing.setYear(year);
        closing.setMonth(month);
        closing.setEmployeeCount(snapshots.size());

        // Snapshot на фирмените данни
        companyRepository.findById(tenantId).ifPresent(company -> {
            Map<String, Object> companyData = new LinkedHashMap<>();
            companyData.put("name", company.getName());
            companyData.put("bulstat", company.getBulstat());
            companyData.put("nkidCode", company.getNkidCode());
            closing.setCompanyData(companyData);
        });

        // Snapshot на осигурителни параметри
        ratesRepository.findByTenantIdAndYear(tenantId, year).forEach(r -> {
            Map<String, Object> ratesMap = new LinkedHashMap<>();
            ratesMap.put("minimumWage", r.getMinimumWage());
            ratesMap.put("maxInsurableIncome", r.getMaxInsurableIncome());
            ratesMap.put("flatTaxRate", r.getFlatTaxRate());
            closing.setInsuranceRates(ratesMap);
        });

        // Обобщени суми
        Map<String, Object> totals = new LinkedHashMap<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalEmployerCost = BigDecimal.ZERO;
        BigDecimal totalIncomeTax = BigDecimal.ZERO;
        BigDecimal totalEmployeeIns = BigDecimal.ZERO;
        BigDecimal totalEmployerIns = BigDecimal.ZERO;

        for (PayrollSnapshot s : snapshots) {
            totalGross = MoneyUtil.add(totalGross, s.getGrossSalary());
            totalNet = MoneyUtil.add(totalNet, s.getNetSalary());
            totalEmployerCost = MoneyUtil.add(totalEmployerCost, s.getTotalEmployerCost());
            totalIncomeTax = MoneyUtil.add(totalIncomeTax, s.getIncomeTax());
            totalEmployeeIns = MoneyUtil.add(totalEmployeeIns, s.getTotalEmployeeInsurance());
            totalEmployerIns = MoneyUtil.add(totalEmployerIns, s.getTotalEmployerInsurance());
        }

        totals.put("totalGross", MoneyUtil.round(totalGross));
        totals.put("totalNet", MoneyUtil.round(totalNet));
        totals.put("totalEmployerCost", MoneyUtil.round(totalEmployerCost));
        totals.put("totalIncomeTax", MoneyUtil.round(totalIncomeTax));
        totals.put("totalEmployeeInsurance", MoneyUtil.round(totalEmployeeIns));
        totals.put("totalEmployerInsurance", MoneyUtil.round(totalEmployerIns));
        closing.setTotals(totals);

        return closing;
    }
}
