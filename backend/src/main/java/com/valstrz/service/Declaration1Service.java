package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.declaration.NapSubmission;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Генериране на Декларация обр. 1 — месечни данни за осигурени лица.
 * Формат: 53 полета, разделени с „;", кодиране Windows-1251, ред \r\n.
 */
@Service
public class Declaration1Service {

    private final PayrollSnapshotRepository snapshotRepo;
    private final CompanyRepository companyRepo;
    private final EmployeeRepository employeeRepo;
    private final EmploymentRepository employmentRepo;
    private final NapSubmissionRepository submissionRepo;

    public Declaration1Service(PayrollSnapshotRepository snapshotRepo,
                               CompanyRepository companyRepo,
                               EmployeeRepository employeeRepo,
                               EmploymentRepository employmentRepo,
                               NapSubmissionRepository submissionRepo) {
        this.snapshotRepo = snapshotRepo;
        this.companyRepo = companyRepo;
        this.employeeRepo = employeeRepo;
        this.employmentRepo = employmentRepo;
        this.submissionRepo = submissionRepo;
    }

    // ── DTO за преглед ──

    public record D1Record(
            String employeeId,
            String employeeName,
            String egn,
            List<String> fields,
            List<String> validationErrors
    ) {
        public String toFileLine() {
            List<String> formatted = new ArrayList<>();
            for (int i = 0; i < fields.size(); i++) {
                String val = fields.get(i);
                if (isQuotedField(i)) {
                    formatted.add("\"" + val + "\"");
                } else {
                    formatted.add(val);
                }
            }
            return String.join(",", formatted);
        }

        private boolean isQuotedField(int index) {
            // Indices 0-based.
            // 2=BULSTAT, 3=EGN, 5=Surname, 6=Initials
            // 27=QualGroup, 28=KID, 29=Activity?, 30=WorkSchedule
            // 50=FundCode, 52=SourceBULSTAT
            return index == 2 || index == 3 || index == 5 || index == 6 ||
                   index == 27 || index == 28 || index == 29 || index == 30 ||
                   index == 50 || index == 52;
        }
    }

    public record ValidationError(
            String employeeId,
            String employeeName,
            String field,
            String message
    ) {}

    // ── Публични методи ──

    public List<D1Record> preview(String tenantId, int year, int month) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) return List.of();

        List<PayrollSnapshot> snapshots = getSnapshots(tenantId, year, month);
        List<D1Record> records = new ArrayList<>();

        for (PayrollSnapshot snapshot : snapshots) {
            Employee employee = employeeRepo.findById(snapshot.getEmployeeId()).orElse(null);
            Employment employment = findCurrentEmployment(tenantId, snapshot.getEmployeeId());
            records.add(buildRecord(snapshot, company, employee, employment, 0));
        }
        return records;
    }

    public NapSubmission generate(String tenantId, int year, int month, int correctionCode) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) throw new RuntimeException("Фирмата не е намерена: " + tenantId);

        List<PayrollSnapshot> snapshots = getSnapshots(tenantId, year, month);
        if (snapshots.isEmpty()) throw new RuntimeException("Няма изчислени заплати за " + month + "/" + year);

        List<D1Record> records = new ArrayList<>();
        List<String> employeeIds = new ArrayList<>();

        for (PayrollSnapshot snapshot : snapshots) {
            Employee employee = employeeRepo.findById(snapshot.getEmployeeId()).orElse(null);
            Employment employment = findCurrentEmployment(tenantId, snapshot.getEmployeeId());
            records.add(buildRecord(snapshot, company, employee, employment, correctionCode));
            employeeIds.add(snapshot.getEmployeeId());
        }

        StringBuilder sb = new StringBuilder();
        List<String> allErrors = new ArrayList<>();
        for (D1Record r : records) {
            sb.append(r.toFileLine()).append("\r\n");
            if (!r.validationErrors().isEmpty()) {
                allErrors.addAll(r.validationErrors());
            }
        }

        String bulstat = safe(company.getBulstat());
        String fileName = String.format("EMPL%d_%s_%02d.TXT", year, bulstat, month);

        NapSubmission submission = new NapSubmission();
        submission.setTenantId(tenantId);
        submission.setType("D1");
        submission.setYear(year);
        submission.setMonth(month);
        submission.setFileName(fileName);
        submission.setFileContent(sb.toString());
        submission.setRecordCount(records.size());
        submission.setStatus("DRAFT");
        submission.setCorrectionCode(correctionCode);
        submission.setGeneratedAt(LocalDateTime.now());
        submission.setValidationErrors(allErrors);
        submission.setEmployeeIds(employeeIds);

        return submissionRepo.save(submission);
    }

    public List<ValidationError> validate(String tenantId, int year, int month) {
        List<D1Record> records = preview(tenantId, year, month);
        List<ValidationError> errors = new ArrayList<>();

        for (D1Record r : records) {
            for (String err : r.validationErrors()) {
                errors.add(new ValidationError(r.employeeId(), r.employeeName(), "", err));
            }
        }
        return errors;
    }

    // ── Построяване на 53-полен запис ──

    private D1Record buildRecord(PayrollSnapshot snapshot, Company company,
                                  Employee employee, Employment employment,
                                  int correctionCode) {
        List<String> fields = new ArrayList<>(Collections.nCopies(53, ""));
        List<String> errors = new ArrayList<>();

        Map<String, Object> empData = snapshot.getEmployeeData() != null ? snapshot.getEmployeeData() : Map.of();
        Map<String, Object> legParams = snapshot.getLegislationParams() != null ? snapshot.getLegislationParams() : Map.of();
        Map<String, Object> tsData = snapshot.getTimesheetData() != null ? snapshot.getTimesheetData() : Map.of();

        // Поле 1: Месец
        fields.set(0, String.valueOf(snapshot.getMonth()));
        // Поле 2: Година
        fields.set(1, String.valueOf(snapshot.getYear()));
        // Поле 3: ЕИК на осигурителя
        String bulstat = safe(company.getBulstat());
        fields.set(2, bulstat);
        if (bulstat.isEmpty()) errors.add("Липсва БУЛСТАТ на фирмата");

        // Поле 4: ЕГН/ЛНЧ
        String egn = "";
        String idType = "0";
        if (employee != null) {
            if (employee.getEgn() != null && !employee.getEgn().isEmpty()) {
                egn = employee.getEgn();
                idType = "0";
            } else if (employee.getLnch() != null && !employee.getLnch().isEmpty()) {
                egn = employee.getLnch();
                idType = "1";
            }
        }
        fields.set(3, egn);
        if (egn.isEmpty()) errors.add("Липсва ЕГН/ЛНЧ");

        // Поле 5: Тип идентификатор
        fields.set(4, idType);

        // Поле 6: Фамилия (до 25 знака)
        String surname = employee != null ? safe(employee.getLastName()) : "";
        if (surname.length() > 25) surname = surname.substring(0, 25);
        fields.set(5, surname);

        // Поле 7: Инициали (firstName[0] + middleName[0])
        String initials = "";
        if (employee != null) {
            String fn = safe(employee.getFirstName());
            String mn = safe(employee.getMiddleName());
            if (!fn.isEmpty()) initials += fn.charAt(0);
            if (!mn.isEmpty()) initials += mn.charAt(0);
        }
        fields.set(6, initials);

        // Поле 8: Вид осигурен
        String insuranceType = getStr(empData, "insuranceType");
        if (insuranceType.isEmpty() && employment != null) {
            insuranceType = safe(employment.getInsuranceType());
        }
        fields.set(7, insuranceType);
        if (insuranceType.isEmpty()) errors.add("Липсва вид осигурен");

        // Полета 9-18: Периоди на осигуряване (5 двойки от-до ден)
        fillPeriodFields(fields, snapshot, employment);

        // Поле 19: Общо дни в осигуряване
        int totalInsuredDays = getInt(tsData, "totalWorkedDays")
                + getInt(tsData, "totalAbsenceDays");
        fields.set(18, String.format("%04d", totalInsuredDays));

        // Поле 20: Отработени дни с осигурителни вноски
        fields.set(19, String.valueOf(getInt(tsData, "totalWorkedDays")));

        // Поле 21: Дни за временна нетрудоспособност (болничен)
        int sickDays = getInt(tsData, "sickLeaveDays");
        fields.set(20, String.valueOf(sickDays));

        // Поле 22: Дни за грижа за малко дете
        fields.set(21, "0");

        // Поле 23: Дни без осигурителни вноски, зачетени за стаж
        fields.set(22, "0");

        // Поле 24: Дни неплатен отпуск
        int unpaidDays = getInt(tsData, "unpaidLeaveDays");
        fields.set(23, String.valueOf(unpaidDays));

        // Поле 25: Дни ВН за сметка на работодателя (първите 3 дни)
        int employerSickDays = Math.min(sickDays, 3);
        fields.set(24, String.valueOf(employerSickDays));

        // Поле 26: Общо часове
        int totalHours = getInt(tsData, "totalWorkedHours");
        fields.set(25, String.valueOf(totalHours));

        // Поле 27: Извънредни часове
        int overtimeHours = getInt(tsData, "totalOvertimeHours");
        fields.set(26, String.valueOf(overtimeHours));

        // Поле 28: Квалификационна група (1-ви знак от НКПД)
        String nkpdCode = getStr(empData, "nkpdCode");
        if (nkpdCode.isEmpty() && employment != null) {
            nkpdCode = safe(employment.getNkpdCode());
        }
        String qualGroup = nkpdCode.length() > 0 ? String.valueOf(nkpdCode.charAt(0)) : "0";
        fields.set(27, qualGroup);

        // Поле 29: КИД код на икономическа дейност
        String kidCode = "";
        if (employment != null && employment.getKidCode() != null && !employment.getKidCode().isEmpty()) {
            kidCode = employment.getKidCode();
        } else if (company.getNkidCode() != null) {
            kidCode = company.getNkidCode();
        }
        fields.set(28, kidCode);

        // Поле 30: Основна икономическа дейност (първите 2 цифри от НКИД)
        String mainActivity = "";
        if (company.getNkidCode() != null && company.getNkidCode().length() >= 2) {
            mainActivity = company.getNkidCode().substring(0, 2);
        }
        fields.set(29, mainActivity);

        // Поле 31: Код работно време
        String workScheduleCode = getStr(empData, "workScheduleCode");
        if (workScheduleCode.isEmpty() && employment != null) {
            workScheduleCode = safe(employment.getWorkScheduleCode());
        }
        fields.set(30, workScheduleCode);

        // Полета 32-33: ЗО доход и %
        BigDecimal insurableIncome = snapshot.getInsurableIncome() != null ? snapshot.getInsurableIncome() : BigDecimal.ZERO;
        fields.set(31, formatMoney(insurableIncome));
        BigDecimal healthEe = getDec(legParams, "healthEmployee");
        BigDecimal healthEr = getDec(legParams, "healthEmployer");
        fields.set(32, formatPercent(healthEe.add(healthEr)));

        // Полета 34-35: ДОО доход Пенсии - работник %
        fields.set(33, formatMoney(insurableIncome));
        fields.set(34, formatPercent(getDec(legParams, "pensionEmployee")));

        // Поле 36: Пенсии - работодател %
        fields.set(35, formatPercent(getDec(legParams, "pensionEmployer")));

        // Полета 37-38: ОЗМ %
        fields.set(36, formatPercent(getDec(legParams, "sicknessEmployee")));
        fields.set(37, formatPercent(getDec(legParams, "sicknessEmployer")));

        // Полета 39-40: Безработица %
        fields.set(38, formatPercent(getDec(legParams, "unemploymentEmployee")));
        fields.set(39, formatPercent(getDec(legParams, "unemploymentEmployer")));

        // Полета 41-43: ДЗПО (само за родени след 1960)
        String insCategory = getStr(legParams, "insuranceCategory");
        if ("after1960".equals(insCategory)) {
            fields.set(40, formatMoney(insurableIncome));
            fields.set(41, formatPercent(getDec(legParams, "supplementaryPensionEmployee")));
            fields.set(42, formatPercent(getDec(legParams, "supplementaryPensionEmployer")));
        }
        // Иначе полета 41-43 остават празни

        // Полета 44-45: Само ЗО доход и % (за лица само здравно осигурени)
        // Типично празни за стандартни служители
        fields.set(43, "");
        fields.set(44, "");

        // Поле 46: Брутна заплата
        fields.set(45, formatMoney(snapshot.getGrossSalary()));

        // Поле 47: ТЗПБ %
        fields.set(46, formatPercent(getDec(legParams, "workAccidentEmployer")));

        // Поле 48: Облагаем доход
        fields.set(47, formatMoney(snapshot.getTaxBase()));

        // Поле 49: ДОД (данък върху доходите)
        fields.set(48, formatMoney(snapshot.getIncomeTax()));

        // Поле 50: Нето заплата
        fields.set(49, formatMoney(snapshot.getNetSalary()));

        // Поле 51: Код осигурителен фонд
        fields.set(50, "000");

        // Поле 52: Код корекция
        fields.set(51, String.valueOf(correctionCode));

        // Поле 53: ЕИК източник
        fields.set(52, bulstat);

        String employeeName = employee != null ? employee.getFullName() : "N/A";
        return new D1Record(snapshot.getEmployeeId(), employeeName, egn, fields, errors);
    }

    // ── Периоди (полета 9-18) ──

    private void fillPeriodFields(List<String> fields, PayrollSnapshot snapshot, Employment employment) {
        int year = snapshot.getYear();
        int month = snapshot.getMonth();
        YearMonth ym = YearMonth.of(year, month);
        int lastDay = ym.lengthOfMonth();

        int startDay = 1;
        int endDay = lastDay;

        if (employment != null) {
            LocalDate startDate = employment.getStartDate();
            if (startDate != null && startDate.getYear() == year && startDate.getMonthValue() == month) {
                startDay = startDate.getDayOfMonth();
            }
            LocalDate termDate = employment.getTerminationDate();
            if (termDate != null && termDate.getYear() == year && termDate.getMonthValue() == month) {
                endDay = termDate.getDayOfMonth();
            }
        }

        // Период 1 (полета 9-10)
        fields.set(8, String.valueOf(startDay));
        fields.set(9, String.valueOf(endDay));

        // Периоди 2-5 (полета 11-18) — празни
        for (int i = 10; i <= 17; i++) {
            fields.set(i, "");
        }
    }

    // ── Помощни методи ──

    private List<PayrollSnapshot> getSnapshots(String tenantId, int year, int month) {
        // Опитваме първо CLOSED, после CALCULATED
        List<PayrollSnapshot> closed = StreamSupport.stream(
                snapshotRepo.findByTenantIdAndYearAndMonthAndStatus(tenantId, year, month, "CLOSED").spliterator(), false
        ).toList();
        if (!closed.isEmpty()) return closed;

        return StreamSupport.stream(
                snapshotRepo.findByTenantIdAndYearAndMonthAndStatus(tenantId, year, month, "CALCULATED").spliterator(), false
        ).toList();
    }

    private Employment findCurrentEmployment(String tenantId, String employeeId) {
        List<Employment> employments = StreamSupport.stream(
                employmentRepo.findByTenantIdAndEmployeeIdAndCurrent(tenantId, employeeId, true).spliterator(), false
        ).toList();
        if (!employments.isEmpty()) return employments.get(0);

        // Fallback: всички
        employments = StreamSupport.stream(
                employmentRepo.findByTenantIdAndEmployeeId(tenantId, employeeId).spliterator(), false
        ).toList();
        return employments.isEmpty() ? null : employments.get(0);
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private BigDecimal getDec(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return new BigDecimal(val.toString());
        try { return new BigDecimal(val.toString()); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) return "0.00";
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
