package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.declaration.NapSubmission;
import com.valstrz.entity.personnel.Amendment;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.entity.personnel.Termination;
import com.valstrz.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Генериране на уведомления по чл. 62 от КТ за НАП.
 * Събития: нов ТД (01), допълнително споразумение (02), прекратяване (03).
 */
@Service
public class Article62Service {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final CompanyRepository companyRepo;
    private final EmployeeRepository employeeRepo;
    private final EmploymentRepository employmentRepo;
    private final AmendmentRepository amendmentRepo;
    private final TerminationRepository terminationRepo;
    private final NapSubmissionRepository submissionRepo;

    public Article62Service(CompanyRepository companyRepo,
                            EmployeeRepository employeeRepo,
                            EmploymentRepository employmentRepo,
                            AmendmentRepository amendmentRepo,
                            TerminationRepository terminationRepo,
                            NapSubmissionRepository submissionRepo) {
        this.companyRepo = companyRepo;
        this.employeeRepo = employeeRepo;
        this.employmentRepo = employmentRepo;
        this.amendmentRepo = amendmentRepo;
        this.terminationRepo = terminationRepo;
        this.submissionRepo = submissionRepo;
    }

    // ── DTO ──

    public record Art62Record(
            String employeeId,
            String employeeName,
            String egn,
            String eventType,       // 01, 02, 03
            String eventTypeName,   // Нов ТД, ДС, Прекратяване
            List<String> fields     // 14 fields for the file
    ) {
        public String toFileLine() {
            return String.join(",", fields);
        }
    }

    // ── Публични методи ──

    public List<Art62Record> preview(String tenantId, LocalDate fromDate, LocalDate toDate) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) return List.of();

        List<Art62Record> records = new ArrayList<>();

        // 01: Нови трудови договори (contractDate в диапазона)
        List<Employment> employments = StreamSupport.stream(
                employmentRepo.findByTenantId(tenantId).spliterator(), false).toList();
        for (Employment emp : employments) {
            if (emp.getContractDate() != null && isInRange(emp.getContractDate(), fromDate, toDate)) {
                Employee employee = employeeRepo.findById(emp.getEmployeeId()).orElse(null);
                records.add(buildRecord(company, employee, emp, "01", "Нов ТД"));
            }
        }

        // 02: Допълнителни споразумения (date в диапазона)
        List<Amendment> amendments = StreamSupport.stream(
                amendmentRepo.findByTenantId(tenantId).spliterator(), false).toList();
        for (Amendment amend : amendments) {
            if (amend.getDate() != null && isInRange(amend.getDate(), fromDate, toDate)) {
                Employee employee = employeeRepo.findById(amend.getEmployeeId()).orElse(null);
                Employment emp = findEmployment(amend.getEmploymentId());
                records.add(buildAmendmentRecord(company, employee, emp, amend));
            }
        }

        // 03: Прекратявания (orderDate в диапазона)
        List<Termination> terminations = StreamSupport.stream(
                terminationRepo.findByTenantId(tenantId).spliterator(), false).toList();
        for (Termination term : terminations) {
            if (term.getOrderDate() != null && isInRange(term.getOrderDate(), fromDate, toDate)) {
                Employee employee = employeeRepo.findById(term.getEmployeeId()).orElse(null);
                Employment emp = findEmployment(term.getEmploymentId());
                records.add(buildTerminationRecord(company, employee, emp, term));
            }
        }

        return records;
    }

    public NapSubmission generate(String tenantId, LocalDate fromDate, LocalDate toDate) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) throw new RuntimeException("Фирмата не е намерена: " + tenantId);

        List<Art62Record> records = preview(tenantId, fromDate, toDate);
        if (records.isEmpty()) throw new RuntimeException("Няма събития за уведомление в периода");

        StringBuilder sb = new StringBuilder();
        List<String> employeeIds = new ArrayList<>();
        for (Art62Record r : records) {
            sb.append(r.toFileLine()).append("\r\n");
            if (!employeeIds.contains(r.employeeId())) {
                employeeIds.add(r.employeeId());
            }
        }

        String bulstat = company.getBulstat() != null ? company.getBulstat() : "";
        String fileName = String.format("UVD62_%s_%s_%s.TXT",
                bulstat, fromDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                toDate.format(DateTimeFormatter.BASIC_ISO_DATE));

        NapSubmission submission = new NapSubmission();
        submission.setTenantId(tenantId);
        submission.setType("ART62");
        submission.setYear(fromDate.getYear());
        submission.setMonth(fromDate.getMonthValue());
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

    private Art62Record buildRecord(Company company, Employee employee, Employment emp,
                                     String eventType, String eventTypeName) {
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

        String basis = emp != null ? safe(emp.getContractBasis()) : "";
        String contractDate = emp != null && emp.getContractDate() != null ? emp.getContractDate().format(DATE_FMT) : "";
        String contractNum = emp != null ? safe(emp.getContractNumber()) : "";
        String nkpdCode = emp != null ? safe(emp.getNkpdCode()) : "";
        String kidCode = emp != null ? safe(emp.getKidCode()) : "";
        String ekatte = safe(company.getEkatte());
        String endDate = emp != null && emp.getContractEndDate() != null ? emp.getContractEndDate().format(DATE_FMT) : "";
        String workTime = "0800"; // Default to 8 hours. Format: HHMM

        List<String> fields = new ArrayList<>();
        fields.add(bulstat);          // 1. ЕИК
        fields.add(egn);              // 2. ЕГН/ЛНЧ
        fields.add(idType);           // 3. Тип идентификатор
        fields.add(basis);            // 4. Код на основание
        fields.add(contractDate);     // 5. Дата на сключване
        fields.add(contractNum);      // 6. Номер на ТД
        fields.add(nkpdCode);         // 7. НКПД
        fields.add(kidCode);          // 8. КИД
        fields.add(ekatte);           // 9. ЕКАТТЕ
        fields.add(endDate);          // 10. Срок на договора
        fields.add("");               // 11. Основание за прекратяване
        fields.add("");               // 12. Дата на прекратяване
        fields.add(workTime);         // 13. Работно време
        fields.add(eventType);        // 14. Тип събитие

        String employeeId = employee != null ? employee.getId() : "";
        return new Art62Record(employeeId, fullName, egn, eventType, eventTypeName, fields);
    }

    private Art62Record buildAmendmentRecord(Company company, Employee employee,
                                              Employment emp, Amendment amend) {
        Art62Record rec = buildRecord(company, employee, emp, "02", "ДС");
        // Update specific fields for amendment
        List<String> f = rec.fields();
        f.set(3, safe(amend.getBasis())); // Basis from amendment
        f.set(4, amend.getDate() != null ? amend.getDate().format(DATE_FMT) : ""); // Date of amendment
        f.set(5, safe(amend.getNumber())); // Number of amendment
        f.set(9, amend.getEndDate() != null ? amend.getEndDate().format(DATE_FMT) : "");
        return rec;
    }

    private Art62Record buildTerminationRecord(Company company, Employee employee,
                                                Employment emp, Termination term) {
        Art62Record rec = buildRecord(company, employee, emp, "03", "Прекратяване");
        List<String> f = rec.fields();
        f.set(10, safe(term.getBasis())); // 11. Основание за прекратяване
        f.set(11, term.getLastWorkDay() != null ? term.getLastWorkDay().format(DATE_FMT) : ""); // 12. Дата на прекратяване
        return rec;
    }

    // ── Помощни методи ──

    private Employment findEmployment(String employmentId) {
        if (employmentId == null || employmentId.isEmpty()) return null;
        return employmentRepo.findById(employmentId).orElse(null);
    }

    private boolean isInRange(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
