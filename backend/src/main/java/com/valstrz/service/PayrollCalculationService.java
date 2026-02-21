package com.valstrz.service;

import com.valstrz.entity.calendar.MonthlyCalendar;
import com.valstrz.entity.calendar.WorkSchedule;
import com.valstrz.entity.insurance.InsuranceContributions;
import com.valstrz.entity.insurance.InsuranceRates;
import com.valstrz.entity.insurance.InsuranceThreshold;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.payroll.PayrollSnapshot.PayrollLine;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.entity.personnel.Garnishment;
import com.valstrz.entity.personnel.MonthlyTimesheet;
import com.valstrz.entity.personnel.MonthlyTimesheet.DailyEntry;
import com.valstrz.util.MoneyUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Stateless изчислителен двигател за заплати.
 * Няма зависимости към repositories - чист вход → изход.
 * Всички парични изчисления минават през MoneyUtil (HALF_UP).
 */
@Service
public class PayrollCalculationService {

    private final GarnishmentService garnishmentService;

    public PayrollCalculationService(GarnishmentService garnishmentService) {
        this.garnishmentService = garnishmentService;
    }

    // ── Входни данни (record) ──

    public record CalculationInput(
        Employee employee,
        Employment employment,
        MonthlyTimesheet timesheet,
        MonthlyCalendar calendar,
        WorkSchedule workSchedule,
        InsuranceRates rates,
        InsuranceContributions contributions,
        InsuranceThreshold threshold,   // може да е null
        int year,
        int month,
        List<AdditionalEarning> additionalEarnings,
        List<AdditionalDeduction> additionalDeductions,
        List<Garnishment> garnishments // Списък със запори
    ) {}

    public record AdditionalEarning(String code, String name, String type, BigDecimal value) {}
    public record AdditionalDeduction(String code, String name, BigDecimal amount) {}

    private record InsuranceResult(
        BigDecimal insurableIncome,
        BigDecimal totalEmployeeInsurance,
        List<PayrollLine> deductionLines
    ) {}

    // ── Кодове на системни пера (номенклатура Тереза) ──

    // Начисления
    private static final String CODE_BASE_SALARY   = "101";  // Работна заплата
    private static final String CODE_SENIORITY     = "201";  // Клас
    private static final String CODE_OVERTIME_WORK  = "211";  // Извънреден труд 50%
    private static final String CODE_OVERTIME_WKND  = "212";  // Извънреден труд 75%
    private static final String CODE_OVERTIME_HOL   = "213";  // Празничен труд 100%
    private static final String CODE_NIGHT_WORK     = "214";  // Нощен труд
    private static final String CODE_PAID_LEAVE     = "321";  // Редовен отпуск
    private static final String CODE_SICK_LEAVE_EMP = "160";  // Болничен работодател

    // Удръжки - работник (3-та категория)
    private static final String CODE_DOO_PENSION_EE = "221";  // ДОО-Пнс,ОЗМ,Бзрб 3 Лс
    private static final String CODE_DOO_SICKN_EE   = "255";  // ДОО-ОЗМ 3 Л
    private static final String CODE_DOO_UNEMPL_EE  = "261";  // ДОО-Безраб. 3 Л
    private static final String CODE_DZPO_EE        = "281";  // ДОО+СР-УПФ Л
    private static final String CODE_HEALTH_EE      = "201";  // ЗО+СР Л (деление: удръжки)
    private static final String CODE_INCOME_TAX     = "500";  // ДОД

    // Осигуровки - работодател (3-та категория)
    private static final String CODE_DOO_PENSION_ER = "621";  // ДОО-Пнс,ОЗМ,Бзрб 3 Рс
    private static final String CODE_DOO_SICKN_ER   = "655";  // ДОО-ОЗМ 3 Р
    private static final String CODE_DOO_UNEMPL_ER  = "661";  // ДОО-Безраб. 3 Р
    private static final String CODE_DZPO_ER        = "687";  // ДЗПО-2 кт Р
    private static final String CODE_HEALTH_ER      = "601";  // ЗО+СР Р
    private static final String CODE_WORK_ACCIDENT   = "685";  // ДОО-ТЗПБ Р
    private static final String CODE_PROF_PENSION_ER = "688";  // ДЗПО-1 кт Р
    private static final String CODE_TEACHER_PENSION_ER = "791"; // Учителски ПФ Р

    // Коефициенти за извънреден труд и нощен труд
    private static final BigDecimal OVERTIME_WEEKDAY_RATE = new BigDecimal("0.50");
    private static final BigDecimal OVERTIME_WEEKEND_RATE = new BigDecimal("0.75");
    private static final BigDecimal OVERTIME_HOLIDAY_RATE = new BigDecimal("1.00");
    private static final BigDecimal NIGHT_WORK_RATE       = new BigDecimal("0.143");
    private static final BigDecimal SICK_LEAVE_EMPLOYER_RATE = new BigDecimal("70");

    // ── Основен метод ──

    /**
     * 10-стъпков алгоритъм за изчисляване на заплата за един служител.
     */
    public PayrollSnapshot calculate(CalculationInput input) {
        Employee emp = input.employee();
        Employment empl = input.employment();
        MonthlyTimesheet ts = input.timesheet();
        MonthlyCalendar cal = input.calendar();
        InsuranceRates rates = input.rates();
        InsuranceContributions contrib = input.contributions();

        int workingDays = cal.getWorkingDays();
        int workedDays = ts.getTotalWorkedDays();
        BigDecimal baseSalary = empl.getBaseSalary();

        List<PayrollLine> earnings = new ArrayList<>();
        List<PayrollLine> deductions = new ArrayList<>();

        // Стъпка 2: Основна заплата пропорционално на отработени дни
        PayrollLine baseLine = calculateBaseSalary(baseSalary, workedDays, workingDays);
        earnings.add(baseLine);

        // Стъпка 3: ДТВ за ТСПО (стаж)
        BigDecimal seniorityPercent = empl.getSeniorityBonusPercent();
        if (seniorityPercent != null && MoneyUtil.isPositive(seniorityPercent)) {
            PayrollLine seniorityLine = calculateSeniorityBonus(baseSalary, seniorityPercent, workedDays, workingDays);
            earnings.add(seniorityLine);
        }

        // Стъпка 4: Извънреден труд + нощен труд
        BigDecimal baseForHourly = MoneyUtil.add(baseLine.getAmount(),
                seniorityPercent != null ? MoneyUtil.percentOfRounded(baseSalary, seniorityPercent) : BigDecimal.ZERO);
        BigDecimal totalWorkingHours = cal.getTotalWorkingHours() != null
                ? cal.getTotalWorkingHours()
                : BigDecimal.valueOf(workingDays).multiply(
                    input.workSchedule() != null && input.workSchedule().getHoursPerDay() != null
                        ? input.workSchedule().getHoursPerDay() : BigDecimal.valueOf(8));
        BigDecimal hourly = MoneyUtil.hourlyRate(baseForHourly, totalWorkingHours);

        List<PayrollLine> overtimeLines = calculateOvertimeAndNight(hourly, ts);
        earnings.addAll(overtimeLines);

        // Стъпка 5: Обезщетения за отпуск (платен) и болнични (работодател)
        BigDecimal avgDaily = MoneyUtil.dailyRate(baseForHourly, workingDays);
        List<PayrollLine> leaveLines = calculateLeaveCompensation(ts, avgDaily);
        earnings.addAll(leaveLines);

        // Допълнителни начисления (СБКО, бонуси и др.)
        if (input.additionalEarnings() != null) {
            for (AdditionalEarning ae : input.additionalEarnings()) {
                PayrollLine line = new PayrollLine();
                line.setCode(ae.code());
                line.setName(ae.name());
                line.setType(ae.type());
                line.setAmount(MoneyUtil.round(ae.value()));
                earnings.add(line);
            }
        }

        // Стъпка 6: Брутно
        BigDecimal gross = BigDecimal.ZERO;
        for (PayrollLine line : earnings) {
            gross = MoneyUtil.add(gross, line.getAmount());
        }
        gross = MoneyUtil.round(gross);

        // Стъпка 7: Осигуровки работник
        InsuranceResult insResult = calculateEmployeeInsurance(gross, contrib, rates, input.threshold());

        deductions.addAll(insResult.deductionLines());

        // Стъпка 8: Данъчна основа
        BigDecimal taxBase = calculateTaxBase(gross, insResult.totalEmployeeInsurance(),
                empl.isDisability50Plus(), rates.getDisabilityTaxExemption());

        // Стъпка 9: ДОД
        BigDecimal incomeTax = calculateIncomeTax(taxBase, rates.getFlatTaxRate());
        PayrollLine taxLine = new PayrollLine();
        taxLine.setCode(CODE_INCOME_TAX);
        taxLine.setName("Данък общ доход");
        taxLine.setType("CALCULATED");
        taxLine.setBase(taxBase);
        taxLine.setRate(rates.getFlatTaxRate());
        taxLine.setAmount(incomeTax);
        deductions.add(taxLine);

        // Допълнителни удръжки (аванс, запори)
        BigDecimal otherDeductions = BigDecimal.ZERO;
        if (input.additionalDeductions() != null) {
            for (AdditionalDeduction ad : input.additionalDeductions()) {
                PayrollLine line = new PayrollLine();
                line.setCode(ad.code());
                line.setName(ad.name());
                line.setType("FIXED");
                line.setAmount(MoneyUtil.round(ad.amount()));
                deductions.add(line);
                otherDeductions = MoneyUtil.add(otherDeductions, ad.amount());
            }
        }

        // Стъпка 10: Нето (първоначално без запори)
        BigDecimal totalBasicDeductions = MoneyUtil.add(
                MoneyUtil.add(insResult.totalEmployeeInsurance(), incomeTax), otherDeductions);
        BigDecimal initialNet = MoneyUtil.round(MoneyUtil.subtract(gross, totalBasicDeductions));

        // Стъпка 10.1: Запори (изчисляват се върху нетото)
        BigDecimal garnishmentsAmount = BigDecimal.ZERO;
        if (input.garnishments() != null && !input.garnishments().isEmpty()) {
            List<GarnishmentService.GarnishmentDeduction> gDeductions = 
                garnishmentService.distribute(input.garnishments(), initialNet, rates.getMinimumWage());
            
            for (GarnishmentService.GarnishmentDeduction gd : gDeductions) {
                PayrollLine line = new PayrollLine();
                line.setCode("451"); // системно перо за запори
                line.setName("Запор: " + gd.name());
                line.setType("FIXED");
                line.setAmount(gd.amount());
                line.setMetadata(Map.of("garnishmentId", gd.garnishmentId()));
                deductions.add(line);
                garnishmentsAmount = MoneyUtil.add(garnishmentsAmount, gd.amount());
            }
        }

        BigDecimal net = MoneyUtil.round(MoneyUtil.subtract(initialNet, garnishmentsAmount));
        BigDecimal totalDeductionsAmount = MoneyUtil.add(totalBasicDeductions, garnishmentsAmount);

        // Осигуровки работодател (отделно, не влияе на нетото)
        List<PayrollLine> employerContribs = calculateEmployerContributions(insResult.insurableIncome(), contrib);
        BigDecimal totalEmployerIns = BigDecimal.ZERO;
        for (PayrollLine line : employerContribs) {
            totalEmployerIns = MoneyUtil.add(totalEmployerIns, line.getAmount());
        }
        totalEmployerIns = MoneyUtil.round(totalEmployerIns);

        // ── Сглобяване на PayrollSnapshot ──
        PayrollSnapshot snapshot = new PayrollSnapshot();
        snapshot.setTenantId(emp.getTenantId());
        snapshot.setEmployeeId(emp.getId());
        snapshot.setYear(input.year());
        snapshot.setMonth(input.month());
        snapshot.setCalculatedAt(LocalDateTime.now());
        snapshot.setStatus("CALCULATED");

        snapshot.setEmployeeData(buildEmployeeDataSnapshot(emp, empl));
        snapshot.setLegislationParams(buildLegislationSnapshot(rates, contrib, input.threshold(), cal));
        snapshot.setTimesheetData(buildTimesheetSnapshot(ts));

        snapshot.setEarnings(earnings);
        snapshot.setDeductions(deductions);
        snapshot.setEmployerContributions(employerContribs);

        snapshot.setGrossSalary(gross);
        snapshot.setInsurableIncome(insResult.insurableIncome());
        snapshot.setTotalEmployeeInsurance(MoneyUtil.round(insResult.totalEmployeeInsurance()));
        snapshot.setTaxBase(MoneyUtil.round(taxBase));
        snapshot.setIncomeTax(incomeTax);
        snapshot.setTotalDeductions(MoneyUtil.round(totalDeductionsAmount));
        snapshot.setNetSalary(net);
        snapshot.setTotalEmployerInsurance(totalEmployerIns);
        snapshot.setTotalEmployerCost(MoneyUtil.round(MoneyUtil.add(gross, totalEmployerIns)));

        return snapshot;
    }

    // ── Стъпка 2: Основна заплата ──

    private PayrollLine calculateBaseSalary(BigDecimal baseSalary, int workedDays, int workingDays) {
        PayrollLine line = new PayrollLine();
        line.setCode(CODE_BASE_SALARY);
        line.setName("Основно възнаграждение");
        line.setType("CALCULATED");
        line.setBase(baseSalary);
        line.setQuantity(BigDecimal.valueOf(workedDays));

        BigDecimal amount;
        if (workingDays == 0) {
            amount = BigDecimal.ZERO;
        } else if (workedDays >= workingDays) {
            amount = baseSalary;
        } else {
            amount = MoneyUtil.round(
                    MoneyUtil.multiply(baseSalary, BigDecimal.valueOf(workedDays))
                            .divide(BigDecimal.valueOf(workingDays), MoneyUtil.CALC_SCALE, MoneyUtil.ROUNDING));
        }
        line.setAmount(MoneyUtil.round(amount));
        return line;
    }

    // ── Стъпка 3: ДТВ за ТСПО ──

    private PayrollLine calculateSeniorityBonus(BigDecimal baseSalary, BigDecimal percent,
                                                  int workedDays, int workingDays) {
        PayrollLine line = new PayrollLine();
        line.setCode(CODE_SENIORITY);
        line.setName("ДТВ за ТСПО");
        line.setType("PERCENT");
        line.setBase(baseSalary);
        line.setRate(percent);
        line.setQuantity(BigDecimal.valueOf(workedDays));

        BigDecimal fullAmount = MoneyUtil.percentOf(baseSalary, percent);
        BigDecimal amount;
        if (workingDays == 0) {
            amount = BigDecimal.ZERO;
        } else if (workedDays >= workingDays) {
            amount = fullAmount;
        } else {
            amount = MoneyUtil.multiply(fullAmount, BigDecimal.valueOf(workedDays))
                    .divide(BigDecimal.valueOf(workingDays), MoneyUtil.CALC_SCALE, MoneyUtil.ROUNDING);
        }
        line.setAmount(MoneyUtil.round(amount));
        return line;
    }

    // ── Стъпка 4: Извънреден и нощен труд ──

    private List<PayrollLine> calculateOvertimeAndNight(BigDecimal hourlyRate, MonthlyTimesheet ts) {
        List<PayrollLine> lines = new ArrayList<>();

        // Разбиваме извънредните часове по тип ден
        BigDecimal overtimeWeekday = BigDecimal.ZERO;
        BigDecimal overtimeWeekend = BigDecimal.ZERO;
        BigDecimal overtimeHoliday = BigDecimal.ZERO;

        if (ts.getDays() != null) {
            for (DailyEntry day : ts.getDays()) {
                BigDecimal ot = day.getOvertimeHours();
                if (ot == null || !MoneyUtil.isPositive(ot)) continue;
                String dayType = day.getDayType();
                if ("WEEKEND".equals(dayType)) {
                    overtimeWeekend = MoneyUtil.add(overtimeWeekend, ot);
                } else if ("HOLIDAY".equals(dayType)) {
                    overtimeHoliday = MoneyUtil.add(overtimeHoliday, ot);
                } else {
                    overtimeWeekday = MoneyUtil.add(overtimeWeekday, ot);
                }
            }
        }

        if (MoneyUtil.isPositive(overtimeWeekday)) {
            PayrollLine line = new PayrollLine();
            line.setCode(CODE_OVERTIME_WORK);
            line.setName("Извънреден труд - работен ден");
            line.setType("CALCULATED");
            line.setBase(hourlyRate);
            line.setRate(OVERTIME_WEEKDAY_RATE);
            line.setQuantity(overtimeWeekday);
            line.setAmount(MoneyUtil.round(MoneyUtil.multiply(hourlyRate, MoneyUtil.multiply(overtimeWeekday, OVERTIME_WEEKDAY_RATE))));
            lines.add(line);
        }
        if (MoneyUtil.isPositive(overtimeWeekend)) {
            PayrollLine line = new PayrollLine();
            line.setCode(CODE_OVERTIME_WKND);
            line.setName("Извънреден труд - почивен ден");
            line.setType("CALCULATED");
            line.setBase(hourlyRate);
            line.setRate(OVERTIME_WEEKEND_RATE);
            line.setQuantity(overtimeWeekend);
            line.setAmount(MoneyUtil.round(MoneyUtil.multiply(hourlyRate, MoneyUtil.multiply(overtimeWeekend, OVERTIME_WEEKEND_RATE))));
            lines.add(line);
        }
        if (MoneyUtil.isPositive(overtimeHoliday)) {
            PayrollLine line = new PayrollLine();
            line.setCode(CODE_OVERTIME_HOL);
            line.setName("Извънреден труд - празник");
            line.setType("CALCULATED");
            line.setBase(hourlyRate);
            line.setRate(OVERTIME_HOLIDAY_RATE);
            line.setQuantity(overtimeHoliday);
            line.setAmount(MoneyUtil.round(MoneyUtil.multiply(hourlyRate, MoneyUtil.multiply(overtimeHoliday, OVERTIME_HOLIDAY_RATE))));
            lines.add(line);
        }

        // Нощен труд
        BigDecimal nightHours = ts.getTotalNightHours();
        if (nightHours != null && MoneyUtil.isPositive(nightHours)) {
            PayrollLine line = new PayrollLine();
            line.setCode(CODE_NIGHT_WORK);
            line.setName("Нощен труд");
            line.setType("CALCULATED");
            line.setBase(hourlyRate);
            line.setRate(NIGHT_WORK_RATE);
            line.setQuantity(nightHours);
            line.setAmount(MoneyUtil.round(MoneyUtil.multiply(hourlyRate, MoneyUtil.multiply(nightHours, NIGHT_WORK_RATE))));
            lines.add(line);
        }

        return lines;
    }

    // ── Стъпка 5: Обезщетения за отпуск и болнични ──

    private List<PayrollLine> calculateLeaveCompensation(MonthlyTimesheet ts, BigDecimal avgDaily) {
        List<PayrollLine> lines = new ArrayList<>();
        if (ts.getDays() == null) return lines;

        int paidLeaveDays = 0;
        int sickLeaveDaysEmployer = 0;

        for (DailyEntry day : ts.getDays()) {
            if (!"ABSENCE".equals(day.getDayType()) || day.getAbsenceCode() == null) continue;
            String code = day.getAbsenceCode();

            // Платен отпуск (кодове 321-329)
            int codeNum = 0;
            try { codeNum = Integer.parseInt(code); } catch (NumberFormatException ignored) {}
            if (codeNum >= 321 && codeNum <= 329) {
                paidLeaveDays++;
            }
            // Болничен - първите 3 дни за сметка на работодател (код 160)
            if ("160".equals(code) || (codeNum >= 301 && codeNum <= 315)) {
                sickLeaveDaysEmployer++;
            }
        }

        // Ограничаваме болничните до 3 дни за работодателя
        sickLeaveDaysEmployer = Math.min(sickLeaveDaysEmployer, 3);

        if (paidLeaveDays > 0) {
            PayrollLine line = new PayrollLine();
            line.setCode(CODE_PAID_LEAVE);
            line.setName("Платен годишен отпуск");
            line.setType("CALCULATED");
            line.setBase(avgDaily);
            line.setQuantity(BigDecimal.valueOf(paidLeaveDays));
            line.setAmount(MoneyUtil.round(MoneyUtil.multiply(avgDaily, BigDecimal.valueOf(paidLeaveDays))));
            lines.add(line);
        }

        if (sickLeaveDaysEmployer > 0) {
            PayrollLine line = new PayrollLine();
            line.setCode(CODE_SICK_LEAVE_EMP);
            line.setName("Болничен - работодател (70%)");
            line.setType("CALCULATED");
            line.setBase(avgDaily);
            line.setRate(SICK_LEAVE_EMPLOYER_RATE);
            line.setQuantity(BigDecimal.valueOf(sickLeaveDaysEmployer));
            BigDecimal dailySick = MoneyUtil.percentOf(avgDaily, SICK_LEAVE_EMPLOYER_RATE);
            line.setAmount(MoneyUtil.round(MoneyUtil.multiply(dailySick, BigDecimal.valueOf(sickLeaveDaysEmployer))));
            lines.add(line);
        }

        return lines;
    }

    // ── Стъпка 7: Осигуровки работник ──

    private InsuranceResult calculateEmployeeInsurance(BigDecimal gross,
                                                        InsuranceContributions c,
                                                        InsuranceRates rates,
                                                        InsuranceThreshold threshold) {
        // Осигурителен доход: min(max(gross, праг), таван)
        BigDecimal minThreshold = threshold != null ? threshold.getMinInsurableIncome() : BigDecimal.ZERO;
        BigDecimal insurableIncome = gross.compareTo(minThreshold) < 0 ? minThreshold : gross;
        if (rates.getMaxInsurableIncome() != null && insurableIncome.compareTo(rates.getMaxInsurableIncome()) > 0) {
            insurableIncome = rates.getMaxInsurableIncome();
        }
        insurableIncome = MoneyUtil.round(insurableIncome);

        List<PayrollLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // ДОО - Пенсии (работник)
        BigDecimal pensionEE = addInsuranceLine(lines, CODE_DOO_PENSION_EE,
                "ДОО - Пенсии (работник)", insurableIncome, c.getPensionEmployee());
        total = MoneyUtil.add(total, pensionEE);

        // ДОО - ОЗМ (работник)
        BigDecimal sicknessEE = addInsuranceLine(lines, CODE_DOO_SICKN_EE,
                "ДОО - ОЗМ (работник)", insurableIncome, c.getSicknessEmployee());
        total = MoneyUtil.add(total, sicknessEE);

        // ДОО - Безработица (работник)
        BigDecimal unemplEE = addInsuranceLine(lines, CODE_DOO_UNEMPL_EE,
                "ДОО - Безработица (работник)", insurableIncome, c.getUnemploymentEmployee());
        total = MoneyUtil.add(total, unemplEE);

        // ДЗПО (само за след 1960)
        if (c.getSupplementaryPensionEmployee() != null && MoneyUtil.isPositive(c.getSupplementaryPensionEmployee())) {
            BigDecimal dzpoEE = addInsuranceLine(lines, CODE_DZPO_EE,
                    "ДЗПО - УПФ (работник)", insurableIncome, c.getSupplementaryPensionEmployee());
            total = MoneyUtil.add(total, dzpoEE);
        }

        // Здравно (работник)
        BigDecimal healthEE = addInsuranceLine(lines, CODE_HEALTH_EE,
                "Здравно осигуряване (работник)", insurableIncome, c.getHealthEmployee());
        total = MoneyUtil.add(total, healthEE);

        return new InsuranceResult(insurableIncome, MoneyUtil.round(total), lines);
    }

    private BigDecimal addInsuranceLine(List<PayrollLine> lines, String code, String name,
                                          BigDecimal insurableIncome, BigDecimal rate) {
        if (rate == null || !MoneyUtil.isPositive(rate)) return BigDecimal.ZERO;
        BigDecimal amount = MoneyUtil.percentOfRounded(insurableIncome, rate);
        PayrollLine line = new PayrollLine();
        line.setCode(code);
        line.setName(name);
        line.setType("CALCULATED");
        line.setBase(insurableIncome);
        line.setRate(rate);
        line.setAmount(amount);
        lines.add(line);
        return amount;
    }

    // ── Стъпка 8: Данъчна основа ──

    private BigDecimal calculateTaxBase(BigDecimal gross, BigDecimal totalInsurance,
                                          boolean disability50Plus, BigDecimal exemption) {
        BigDecimal taxBase = MoneyUtil.subtract(gross, totalInsurance);
        if (disability50Plus && exemption != null && MoneyUtil.isPositive(exemption)) {
            taxBase = MoneyUtil.subtract(taxBase, exemption);
        }
        return taxBase.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : taxBase;
    }

    // ── Стъпка 9: ДОД ──

    private BigDecimal calculateIncomeTax(BigDecimal taxBase, BigDecimal flatTaxRate) {
        return MoneyUtil.percentOfRounded(taxBase, flatTaxRate);
    }

    // ── Осигуровки работодател ──

    private List<PayrollLine> calculateEmployerContributions(BigDecimal insurableIncome,
                                                               InsuranceContributions c) {
        List<PayrollLine> lines = new ArrayList<>();

        addInsuranceLine(lines, CODE_DOO_PENSION_ER, "ДОО - Пенсии (работодател)",
                insurableIncome, c.getPensionEmployer());
        addInsuranceLine(lines, CODE_DOO_SICKN_ER, "ДОО - ОЗМ (работодател)",
                insurableIncome, c.getSicknessEmployer());
        addInsuranceLine(lines, CODE_DOO_UNEMPL_ER, "ДОО - Безработица (работодател)",
                insurableIncome, c.getUnemploymentEmployer());

        if (c.getSupplementaryPensionEmployer() != null && MoneyUtil.isPositive(c.getSupplementaryPensionEmployer())) {
            addInsuranceLine(lines, CODE_DZPO_ER, "ДЗПО - УПФ (работодател)",
                    insurableIncome, c.getSupplementaryPensionEmployer());
        }

        addInsuranceLine(lines, CODE_HEALTH_ER, "Здравно осигуряване (работодател)",
                insurableIncome, c.getHealthEmployer());
        addInsuranceLine(lines, CODE_WORK_ACCIDENT, "ТЗПБ (работодател)",
                insurableIncome, c.getWorkAccidentEmployer());

        // ППФ - Професионален пенсионен фонд (1-ва и 2-ра категория)
        if (c.getProfessionalPensionEmployer() != null && MoneyUtil.isPositive(c.getProfessionalPensionEmployer())) {
            addInsuranceLine(lines, CODE_PROF_PENSION_ER, "ППФ (работодател)",
                    insurableIncome, c.getProfessionalPensionEmployer());
        }

        // Учителски пенсионен фонд (вид осигурен 08)
        if (c.getTeacherPensionEmployer() != null && MoneyUtil.isPositive(c.getTeacherPensionEmployer())) {
            addInsuranceLine(lines, CODE_TEACHER_PENSION_ER, "Учителски пенс. фонд (работодател)",
                    insurableIncome, c.getTeacherPensionEmployer());
        }

        return lines;
    }

    // ── Snapshot builders ──

    private Map<String, Object> buildEmployeeDataSnapshot(Employee e, Employment empl) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("firstName", e.getFirstName());
        data.put("middleName", e.getMiddleName());
        data.put("lastName", e.getLastName());
        data.put("fullName", e.getFullName());
        data.put("egn", e.getEgn());
        data.put("jobTitle", empl.getJobTitle());
        data.put("nkpdCode", empl.getNkpdCode());
        data.put("departmentId", empl.getDepartmentId());
        data.put("contractNumber", empl.getContractNumber());
        data.put("startDate", empl.getStartDate() != null ? empl.getStartDate().toString() : null);
        data.put("baseSalary", empl.getBaseSalary());
        data.put("seniorityBonusPercent", empl.getSeniorityBonusPercent());
        data.put("paymentType", empl.getPaymentType());
        data.put("workScheduleCode", empl.getWorkScheduleCode());
        data.put("personnelGroup", empl.getPersonnelGroup());
        data.put("disability50Plus", empl.isDisability50Plus());
        data.put("insuranceType", empl.getInsuranceType());
        data.put("insuredType", empl.getInsuredType());
        return data;
    }

    private Map<String, Object> buildLegislationSnapshot(InsuranceRates r,
                                                           InsuranceContributions c,
                                                           InsuranceThreshold t,
                                                           MonthlyCalendar cal) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("minimumWage", r.getMinimumWage());
        data.put("maxInsurableIncome", r.getMaxInsurableIncome());
        data.put("flatTaxRate", r.getFlatTaxRate());
        data.put("disabilityTaxExemption", r.getDisabilityTaxExemption());
        data.put("insuranceCategory", c.getCategory());
        data.put("insuredType", c.getInsuredType());
        data.put("pensionEmployer", c.getPensionEmployer());
        data.put("pensionEmployee", c.getPensionEmployee());
        data.put("sicknessEmployer", c.getSicknessEmployer());
        data.put("sicknessEmployee", c.getSicknessEmployee());
        data.put("unemploymentEmployer", c.getUnemploymentEmployer());
        data.put("unemploymentEmployee", c.getUnemploymentEmployee());
        data.put("supplementaryPensionEmployer", c.getSupplementaryPensionEmployer());
        data.put("supplementaryPensionEmployee", c.getSupplementaryPensionEmployee());
        data.put("healthEmployer", c.getHealthEmployer());
        data.put("healthEmployee", c.getHealthEmployee());
        data.put("workAccidentEmployer", c.getWorkAccidentEmployer());
        data.put("professionalPensionEmployer", c.getProfessionalPensionEmployer());
        data.put("teacherPensionEmployer", c.getTeacherPensionEmployer());
        if (t != null) {
            data.put("minInsurableIncome", t.getMinInsurableIncome());
            data.put("thresholdPersonnelGroup", t.getPersonnelGroup());
        }
        data.put("workingDays", cal.getWorkingDays());
        data.put("totalWorkingHours", cal.getTotalWorkingHours());
        return data;
    }

    private Map<String, Object> buildTimesheetSnapshot(MonthlyTimesheet ts) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalWorkedDays", ts.getTotalWorkedDays());
        data.put("totalWorkedHours", ts.getTotalWorkedHours());
        data.put("totalOvertimeHours", ts.getTotalOvertimeHours());
        data.put("totalNightHours", ts.getTotalNightHours());
        data.put("totalAbsenceDays", ts.getTotalAbsenceDays());
        return data;
    }
}
