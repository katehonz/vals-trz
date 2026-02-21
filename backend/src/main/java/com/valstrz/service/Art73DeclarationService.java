package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.declaration.NapSubmission;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.repository.*;
import com.valstrz.util.MoneyUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Справка по чл. 73, ал. 6 от ЗДДФЛ — годишна справка
 * за изплатени доходи на физически лица.
 *
 * Генерира обобщени данни за всички изплатени трудови възнаграждения
 * по служители за цяла година.
 */
@Service
public class Art73DeclarationService {

    private final PayrollSnapshotRepository snapshotRepo;
    private final CompanyRepository companyRepo;
    private final EmployeeRepository employeeRepo;
    private final EmploymentRepository employmentRepo;
    private final NapSubmissionRepository submissionRepo;

    public Art73DeclarationService(PayrollSnapshotRepository snapshotRepo,
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

    // ── DTO ──

    public record Art73Record(
            String employeeId,
            String employeeName,
            String egn,
            String idType,
            BigDecimal totalGross,
            BigDecimal totalInsurableIncome,
            BigDecimal totalEmployeeInsurance,
            BigDecimal totalTaxBase,
            BigDecimal totalIncomeTax,
            BigDecimal totalNet,
            int monthsWorked,
            List<Art73MonthDetail> months
    ) {}

    public record Art73MonthDetail(
            int month,
            BigDecimal gross,
            BigDecimal insurableIncome,
            BigDecimal employeeInsurance,
            BigDecimal taxBase,
            BigDecimal incomeTax,
            BigDecimal net
    ) {}

    public record Art73Summary(
            int year,
            String companyName,
            String bulstat,
            int totalEmployees,
            BigDecimal totalGross,
            BigDecimal totalInsurance,
            BigDecimal totalTax,
            BigDecimal totalNet,
            List<Art73Record> records
    ) {}

    // ── Публични методи ──

    public Art73Summary preview(String tenantId, int year) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) return new Art73Summary(year, "", "", 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());

        // Събираме всички snapshots за годината
        Map<String, List<PayrollSnapshot>> byEmployee = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            List<PayrollSnapshot> monthSnapshots = getSnapshots(tenantId, year, m);
            for (PayrollSnapshot s : monthSnapshots) {
                byEmployee.computeIfAbsent(s.getEmployeeId(), k -> new ArrayList<>()).add(s);
            }
        }

        List<Art73Record> records = new ArrayList<>();
        BigDecimal sumGross = BigDecimal.ZERO;
        BigDecimal sumInsurance = BigDecimal.ZERO;
        BigDecimal sumTax = BigDecimal.ZERO;
        BigDecimal sumNet = BigDecimal.ZERO;

        for (Map.Entry<String, List<PayrollSnapshot>> entry : byEmployee.entrySet()) {
            String empId = entry.getKey();
            List<PayrollSnapshot> snapshots = entry.getValue();

            Employee employee = employeeRepo.findById(empId).orElse(null);
            String name = employee != null ? employee.getFullName() : empId;
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

            BigDecimal totalGross = BigDecimal.ZERO;
            BigDecimal totalInsurableIncome = BigDecimal.ZERO;
            BigDecimal totalEmployeeIns = BigDecimal.ZERO;
            BigDecimal totalTaxBase = BigDecimal.ZERO;
            BigDecimal totalIncomeTax = BigDecimal.ZERO;
            BigDecimal totalNet = BigDecimal.ZERO;
            List<Art73MonthDetail> months = new ArrayList<>();

            for (PayrollSnapshot s : snapshots) {
                BigDecimal gross = s.getGrossSalary() != null ? s.getGrossSalary() : BigDecimal.ZERO;
                BigDecimal insIncome = s.getInsurableIncome() != null ? s.getInsurableIncome() : BigDecimal.ZERO;
                BigDecimal empIns = s.getTotalEmployeeInsurance() != null ? s.getTotalEmployeeInsurance() : BigDecimal.ZERO;
                BigDecimal taxBase = s.getTaxBase() != null ? s.getTaxBase() : BigDecimal.ZERO;
                BigDecimal tax = s.getIncomeTax() != null ? s.getIncomeTax() : BigDecimal.ZERO;
                BigDecimal net = s.getNetSalary() != null ? s.getNetSalary() : BigDecimal.ZERO;

                totalGross = MoneyUtil.add(totalGross, gross);
                totalInsurableIncome = MoneyUtil.add(totalInsurableIncome, insIncome);
                totalEmployeeIns = MoneyUtil.add(totalEmployeeIns, empIns);
                totalTaxBase = MoneyUtil.add(totalTaxBase, taxBase);
                totalIncomeTax = MoneyUtil.add(totalIncomeTax, tax);
                totalNet = MoneyUtil.add(totalNet, net);

                months.add(new Art73MonthDetail(
                        s.getMonth(),
                        MoneyUtil.round(gross),
                        MoneyUtil.round(insIncome),
                        MoneyUtil.round(empIns),
                        MoneyUtil.round(taxBase),
                        MoneyUtil.round(tax),
                        MoneyUtil.round(net)
                ));
            }

            // Сортираме по месец
            months.sort(Comparator.comparingInt(Art73MonthDetail::month));

            records.add(new Art73Record(
                    empId, name, egn, idType,
                    MoneyUtil.round(totalGross),
                    MoneyUtil.round(totalInsurableIncome),
                    MoneyUtil.round(totalEmployeeIns),
                    MoneyUtil.round(totalTaxBase),
                    MoneyUtil.round(totalIncomeTax),
                    MoneyUtil.round(totalNet),
                    snapshots.size(),
                    months
            ));

            sumGross = MoneyUtil.add(sumGross, totalGross);
            sumInsurance = MoneyUtil.add(sumInsurance, totalEmployeeIns);
            sumTax = MoneyUtil.add(sumTax, totalIncomeTax);
            sumNet = MoneyUtil.add(sumNet, totalNet);
        }

        return new Art73Summary(
                year,
                company.getName() != null ? company.getName() : "",
                company.getBulstat() != null ? company.getBulstat() : "",
                records.size(),
                MoneyUtil.round(sumGross),
                MoneyUtil.round(sumInsurance),
                MoneyUtil.round(sumTax),
                MoneyUtil.round(sumNet),
                records
        );
    }

    public NapSubmission generate(String tenantId, int year) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) throw new RuntimeException("Фирмата не е намерена: " + tenantId);

        Art73Summary summary = preview(tenantId, year);
        if (summary.records().isEmpty()) {
            throw new RuntimeException("Няма данни за " + year + " година.");
        }

        // Генерираме CSV файл
        StringBuilder sb = new StringBuilder();
        // Header
        sb.append("ЕИК;ЕГН/ЛНЧ;Тип ид.;Име;Брутно;Осиг.доход;Осиг.вноски;Облагаем доход;Данък;Нето;Месеци\r\n");

        for (Art73Record r : summary.records()) {
            sb.append(safe(summary.bulstat())).append(";");
            sb.append(safe(r.egn())).append(";");
            sb.append(r.idType()).append(";");
            sb.append(safe(r.employeeName())).append(";");
            sb.append(fmtDec(r.totalGross())).append(";");
            sb.append(fmtDec(r.totalInsurableIncome())).append(";");
            sb.append(fmtDec(r.totalEmployeeInsurance())).append(";");
            sb.append(fmtDec(r.totalTaxBase())).append(";");
            sb.append(fmtDec(r.totalIncomeTax())).append(";");
            sb.append(fmtDec(r.totalNet())).append(";");
            sb.append(r.monthsWorked());
            sb.append("\r\n");
        }

        String bulstat = safe(company.getBulstat());
        String fileName = String.format("ART73_%d_%s.CSV", year, bulstat);

        NapSubmission submission = new NapSubmission();
        submission.setTenantId(tenantId);
        submission.setType("ART73");
        submission.setYear(year);
        submission.setMonth(0); // годишна
        submission.setFileName(fileName);
        submission.setFileContent(sb.toString());
        submission.setRecordCount(summary.records().size());
        submission.setStatus("DRAFT");
        submission.setGeneratedAt(LocalDateTime.now());
        submission.setValidationErrors(List.of());
        submission.setEmployeeIds(summary.records().stream().map(Art73Record::employeeId).toList());

        return submissionRepo.save(submission);
    }

    // ── Помощни методи ──

    private List<PayrollSnapshot> getSnapshots(String tenantId, int year, int month) {
        List<PayrollSnapshot> closed = StreamSupport.stream(
                snapshotRepo.findByTenantIdAndYearAndMonthAndStatus(tenantId, year, month, "CLOSED").spliterator(), false
        ).toList();
        if (!closed.isEmpty()) return closed;

        return StreamSupport.stream(
                snapshotRepo.findByTenantIdAndYearAndMonthAndStatus(tenantId, year, month, "CALCULATED").spliterator(), false
        ).toList();
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private String fmtDec(BigDecimal val) {
        if (val == null) return "0.00";
        return val.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
