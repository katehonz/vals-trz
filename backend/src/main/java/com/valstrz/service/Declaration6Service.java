package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.declaration.NapSubmission;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.repository.CompanyRepository;
import com.valstrz.repository.NapSubmissionRepository;
import com.valstrz.repository.PayrollSnapshotRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Генериране на Декларация обр. 6 — обобщени дължими вноски.
 * Два файла: осигуровки (_O.TXT) и данъци (_D.TXT).
 */
@Service
public class Declaration6Service {

    private final PayrollSnapshotRepository snapshotRepo;
    private final CompanyRepository companyRepo;
    private final NapSubmissionRepository submissionRepo;

    public Declaration6Service(PayrollSnapshotRepository snapshotRepo,
                               CompanyRepository companyRepo,
                               NapSubmissionRepository submissionRepo) {
        this.snapshotRepo = snapshotRepo;
        this.companyRepo = companyRepo;
        this.submissionRepo = submissionRepo;
    }

    // ── DTO ──

    public record D6Data(
            String companyBulstat,
            int year,
            int month,
            int employeeCount,
            BigDecimal totalPensionEmployer,
            BigDecimal totalPensionEmployee,
            BigDecimal totalSicknessEmployer,
            BigDecimal totalSicknessEmployee,
            BigDecimal totalUnemploymentEmployer,
            BigDecimal totalUnemploymentEmployee,
            BigDecimal totalSupplementaryEmployer,
            BigDecimal totalSupplementaryEmployee,
            BigDecimal totalHealthEmployer,
            BigDecimal totalHealthEmployee,
            BigDecimal totalWorkAccident,
            BigDecimal totalIncomeTax,
            BigDecimal grandTotalInsurance,
            BigDecimal grandTotalTax
    ) {}

    // ── Публични методи ──

    public D6Data preview(String tenantId, int year, int month) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) return null;

        List<PayrollSnapshot> snapshots = getSnapshots(tenantId, year, month);
        return aggregate(company, year, month, snapshots);
    }

    public NapSubmission generate(String tenantId, int year, int month) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) throw new RuntimeException("Фирмата не е намерена: " + tenantId);

        List<PayrollSnapshot> snapshots = getSnapshots(tenantId, year, month);
        if (snapshots.isEmpty()) throw new RuntimeException("Няма изчислени заплати за " + month + "/" + year);

        D6Data data = aggregate(company, year, month, snapshots);
        String bulstat = company.getBulstat() != null ? company.getBulstat() : "";

        // Файл за осигуровки
        String insContent = buildInsuranceFile(data, bulstat);
        String insFileName = String.format("NRA62007_%s-%d_%02d_O.TXT", bulstat, year, month);

        NapSubmission insSubmission = new NapSubmission();
        insSubmission.setTenantId(tenantId);
        insSubmission.setType("D6_INS");
        insSubmission.setYear(year);
        insSubmission.setMonth(month);
        insSubmission.setFileName(insFileName);
        insSubmission.setFileContent(insContent);
        insSubmission.setRecordCount(data.employeeCount());
        insSubmission.setStatus("DRAFT");
        insSubmission.setCorrectionCode(0);
        insSubmission.setGeneratedAt(LocalDateTime.now());
        insSubmission.setValidationErrors(List.of());
        insSubmission.setEmployeeIds(List.of());
        submissionRepo.save(insSubmission);

        // Файл за данъци
        String taxContent = buildTaxFile(data, bulstat);
        String taxFileName = String.format("NRA2007_%s_%02d_D.TXT", bulstat, month);

        NapSubmission taxSubmission = new NapSubmission();
        taxSubmission.setTenantId(tenantId);
        taxSubmission.setType("D6_TAX");
        taxSubmission.setYear(year);
        taxSubmission.setMonth(month);
        taxSubmission.setFileName(taxFileName);
        taxSubmission.setFileContent(taxContent);
        taxSubmission.setRecordCount(data.employeeCount());
        taxSubmission.setStatus("DRAFT");
        taxSubmission.setCorrectionCode(0);
        taxSubmission.setGeneratedAt(LocalDateTime.now());
        taxSubmission.setValidationErrors(List.of());
        taxSubmission.setEmployeeIds(List.of());

        return submissionRepo.save(taxSubmission);
    }

    // ── Агрегация ──

    private D6Data aggregate(Company company, int year, int month, List<PayrollSnapshot> snapshots) {
        BigDecimal pensionEr = BigDecimal.ZERO, pensionEe = BigDecimal.ZERO;
        BigDecimal sicknessEr = BigDecimal.ZERO, sicknessEe = BigDecimal.ZERO;
        BigDecimal unemploymentEr = BigDecimal.ZERO, unemploymentEe = BigDecimal.ZERO;
        BigDecimal suppEr = BigDecimal.ZERO, suppEe = BigDecimal.ZERO;
        BigDecimal healthEr = BigDecimal.ZERO, healthEe = BigDecimal.ZERO;
        BigDecimal workAccident = BigDecimal.ZERO;
        BigDecimal incomeTax = BigDecimal.ZERO;

        for (PayrollSnapshot s : snapshots) {
            pensionEr = pensionEr.add(sumByCode(s.getEmployerContributions(), "351911"));
            pensionEe = pensionEe.add(sumByCode(s.getDeductions(), "351901"));
            sicknessEr = sicknessEr.add(sumByCode(s.getEmployerContributions(), "351912"));
            sicknessEe = sicknessEe.add(sumByCode(s.getDeductions(), "351902"));
            unemploymentEr = unemploymentEr.add(sumByCode(s.getEmployerContributions(), "351913"));
            unemploymentEe = unemploymentEe.add(sumByCode(s.getDeductions(), "351903"));
            suppEr = suppEr.add(sumByCode(s.getEmployerContributions(), "351914"));
            suppEe = suppEe.add(sumByCode(s.getDeductions(), "351904"));
            healthEr = healthEr.add(sumByCode(s.getEmployerContributions(), "351915"));
            healthEe = healthEe.add(sumByCode(s.getDeductions(), "351905"));
            workAccident = workAccident.add(sumByCode(s.getEmployerContributions(), "351916"));
            incomeTax = incomeTax.add(sumByCode(s.getDeductions(), "351982"));
        }

        BigDecimal grandInsurance = pensionEr.add(pensionEe)
                .add(sicknessEr).add(sicknessEe)
                .add(unemploymentEr).add(unemploymentEe)
                .add(suppEr).add(suppEe)
                .add(healthEr).add(healthEe)
                .add(workAccident);

        String bulstat = company.getBulstat() != null ? company.getBulstat() : "";
        return new D6Data(bulstat, year, month, snapshots.size(),
                pensionEr, pensionEe, sicknessEr, sicknessEe,
                unemploymentEr, unemploymentEe, suppEr, suppEe,
                healthEr, healthEe, workAccident, incomeTax,
                grandInsurance, incomeTax);
    }

    private BigDecimal sumByCode(List<PayrollSnapshot.PayrollLine> lines, String code) {
        if (lines == null) return BigDecimal.ZERO;
        return lines.stream()
                .filter(l -> code.equals(l.getCode()))
                .map(l -> l.getAmount() != null ? l.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Файлове ──

    private String buildInsuranceFile(D6Data data, String bulstat) {
        // Формат: ЕИК;месец;година;вид плащане;пенсии РД;пенсии РЛ;ОЗМ РД;ОЗМ РЛ;
        // безр РД;безр РЛ;ДЗПО РД;ДЗПО РЛ;ЗО РД;ЗО РЛ;ТЗПБ;Общо
        List<String> fields = new ArrayList<>();
        fields.add("\"" + bulstat + "\"");
        fields.add(String.valueOf(data.month()));
        fields.add(String.valueOf(data.year()));
        fields.add("1"); // вид плащане = заплати
        fields.add(fmt(data.totalPensionEmployer()));
        fields.add(fmt(data.totalPensionEmployee()));
        fields.add(fmt(data.totalSicknessEmployer()));
        fields.add(fmt(data.totalSicknessEmployee()));
        fields.add(fmt(data.totalUnemploymentEmployer()));
        fields.add(fmt(data.totalUnemploymentEmployee()));
        fields.add(fmt(data.totalSupplementaryEmployer()));
        fields.add(fmt(data.totalSupplementaryEmployee()));
        fields.add(fmt(data.totalHealthEmployer()));
        fields.add(fmt(data.totalHealthEmployee()));
        fields.add(fmt(data.totalWorkAccident()));
        fields.add(fmt(data.grandTotalInsurance()));
        fields.add(String.valueOf(data.employeeCount()));

        return String.join(",", fields) + "\r\n";
    }

    private String buildTaxFile(D6Data data, String bulstat) {
        // Формат: ЕИК;месец;година;вид плащане;общ ДОД;брой лица
        List<String> fields = new ArrayList<>();
        fields.add("\"" + bulstat + "\"");
        fields.add(String.valueOf(data.month()));
        fields.add(String.valueOf(data.year()));
        fields.add("1");
        fields.add(fmt(data.totalIncomeTax()));
        fields.add(String.valueOf(data.employeeCount()));

        return String.join(",", fields) + "\r\n";
    }

    private String fmt(BigDecimal val) {
        if (val == null) return "0.00";
        return val.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private List<PayrollSnapshot> getSnapshots(String tenantId, int year, int month) {
        List<PayrollSnapshot> closed = StreamSupport.stream(
                snapshotRepo.findByTenantIdAndYearAndMonthAndStatus(tenantId, year, month, "CLOSED").spliterator(), false
        ).toList();
        if (!closed.isEmpty()) return closed;

        return StreamSupport.stream(
                snapshotRepo.findByTenantIdAndYearAndMonthAndStatus(tenantId, year, month, "CALCULATED").spliterator(), false
        ).toList();
    }
}
