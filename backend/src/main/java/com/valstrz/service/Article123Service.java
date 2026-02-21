package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.declaration.NapSubmission;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Генериране на уведомления по чл. 123 от КТ за НАП.
 * При промяна на работодател: сливане, вливане, разделяне, отделяне,
 * преотстъпване/прехвърляне на дейност, промяна на правноорг. форма.
 */
@Service
public class Article123Service {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final CompanyRepository companyRepo;
    private final EmployeeRepository employeeRepo;
    private final EmploymentRepository employmentRepo;
    private final NapSubmissionRepository submissionRepo;

    public Article123Service(CompanyRepository companyRepo,
                              EmployeeRepository employeeRepo,
                              EmploymentRepository employmentRepo,
                              NapSubmissionRepository submissionRepo) {
        this.companyRepo = companyRepo;
        this.employeeRepo = employeeRepo;
        this.employmentRepo = employmentRepo;
        this.submissionRepo = submissionRepo;
    }

    // ── DTO ──

    /**
     * @param changeType Код на промяна: 1=сливане, 2=вливане, 3=разделяне,
     *                   4=отделяне, 5=преотстъпване/прехвърляне, 6=промяна правна форма
     */
    public record Art123Record(
            String employeeId,
            String employeeName,
            String egn,
            int changeType,
            String changeTypeName,
            List<String> fields
    ) {
        public String toFileLine() {
            return String.join(",", fields);
        }
    }

    public record Art123Request(
            int changeType,
            String newEmployerBulstat,
            String newEmployerName,
            LocalDate changeDate,
            List<String> employeeIds  // null = всички активни
    ) {}

    private static final String[] CHANGE_TYPE_NAMES = {
            "", "Сливане", "Вливане", "Разделяне", "Отделяне",
            "Преотстъпване/прехвърляне на дейност", "Промяна на правноорг. форма"
    };

    // ── Публични методи ──

    public List<Art123Record> preview(String tenantId, Art123Request request) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) return List.of();

        List<Employee> employees = getAffectedEmployees(tenantId, request.employeeIds());
        List<Art123Record> records = new ArrayList<>();

        for (Employee emp : employees) {
            Employment employment = findCurrentEmployment(tenantId, emp.getId());
            records.add(buildRecord(company, emp, employment, request));
        }
        return records;
    }

    public NapSubmission generate(String tenantId, Art123Request request) {
        Company company = companyRepo.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Фирмата не е намерена: " + tenantId));

        List<Art123Record> records = preview(tenantId, request);
        if (records.isEmpty()) throw new RuntimeException("Няма служители за уведомление");

        StringBuilder sb = new StringBuilder();
        List<String> employeeIds = new ArrayList<>();
        for (Art123Record r : records) {
            sb.append(r.toFileLine()).append("\r\n");
            if (!employeeIds.contains(r.employeeId())) {
                employeeIds.add(r.employeeId());
            }
        }

        String bulstat = company.getBulstat() != null ? company.getBulstat() : "";
        String fileName = String.format("UVD123_%s_%s.TXT",
                bulstat, request.changeDate().format(DateTimeFormatter.BASIC_ISO_DATE));

        NapSubmission submission = new NapSubmission();
        submission.setTenantId(tenantId);
        submission.setType("ART123");
        submission.setYear(request.changeDate().getYear());
        submission.setMonth(request.changeDate().getMonthValue());
        submission.setFileName(fileName);
        submission.setFileContent(sb.toString());
        submission.setRecordCount(records.size());
        submission.setStatus("DRAFT");
        submission.setCorrectionCode(0);
        submission.setGeneratedAt(LocalDateTime.now());
        submission.setValidationErrors(List.of());
        submission.setEmployeeIds(employeeIds);

        return submissionRepo.save(submission);
    }

    // ── Построяване на записи ──

    private Art123Record buildRecord(Company company, Employee employee,
                                      Employment employment, Art123Request request) {
        String bulstat = safe(company.getBulstat());
        String egn = "";
        String idType = "0";
        String fullName = "";

        if (employee != null) {
            fullName = employee.getFullName();
            if (employee.getEgn() != null && !employee.getEgn().isEmpty()) {
                egn = employee.getEgn();
                idType = "0";
            } else if (employee.getLnch() != null && !employee.getLnch().isEmpty()) {
                egn = employee.getLnch();
                idType = "1";
            }
        }

        String changeTypeName = request.changeType() >= 1 && request.changeType() <= 6
                ? CHANGE_TYPE_NAMES[request.changeType()] : "Неизвестна";

        String contractNum = employment != null ? safe(employment.getContractNumber()) : "";
        String contractDate = employment != null && employment.getContractDate() != null
                ? employment.getContractDate().format(DATE_FMT) : "";
        String nkpdCode = employment != null ? safe(employment.getNkpdCode()) : "";
        String startDate = employment != null && employment.getStartDate() != null
                ? employment.getStartDate().format(DATE_FMT) : "";

        List<String> fields = new ArrayList<>();
        fields.add(bulstat);                                          // 1. ЕИК стар работодател
        fields.add(egn);                                              // 2. ЕГН/ЛНЧ
        fields.add(idType);                                           // 3. Тип идентификатор
        fields.add(safe(request.newEmployerBulstat()));               // 4. ЕИК нов работодател
        fields.add(String.valueOf(request.changeType()));             // 5. Код на промяна
        fields.add(request.changeDate().format(DATE_FMT));            // 6. Дата на промяна
        fields.add(contractNum);                                      // 7. Номер на ТД
        fields.add(contractDate);                                     // 8. Дата на ТД
        fields.add(nkpdCode);                                        // 9. НКПД
        fields.add(startDate);                                       // 10. Дата на постъпване

        String employeeId = employee != null ? employee.getId() : "";
        return new Art123Record(employeeId, fullName, egn,
                request.changeType(), changeTypeName, fields);
    }

    // ── Помощни методи ──

    private List<Employee> getAffectedEmployees(String tenantId, List<String> employeeIds) {
        if (employeeIds != null && !employeeIds.isEmpty()) {
            List<Employee> result = new ArrayList<>();
            for (String id : employeeIds) {
                employeeRepo.findById(id).ifPresent(result::add);
            }
            return result;
        }
        // Всички активни служители
        return StreamSupport.stream(
                employeeRepo.findByTenantId(tenantId).spliterator(), false
        ).toList();
    }

    private Employment findCurrentEmployment(String tenantId, String employeeId) {
        List<Employment> employments = StreamSupport.stream(
                employmentRepo.findByTenantIdAndEmployeeId(tenantId, employeeId).spliterator(), false
        ).toList();
        return employments.stream()
                .filter(Employment::isCurrent)
                .findFirst()
                .orElse(employments.isEmpty() ? null : employments.get(0));
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
