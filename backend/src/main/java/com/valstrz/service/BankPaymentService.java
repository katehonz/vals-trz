package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.repository.CompanyRepository;
import com.valstrz.repository.EmployeeRepository;
import com.valstrz.repository.PayrollSnapshotRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Генериране на файл за масови банкови преводи на заплати.
 */
@Service
public class BankPaymentService {

    private final PayrollSnapshotRepository snapshotRepo;
    private final CompanyRepository companyRepo;
    private final EmployeeRepository employeeRepo;

    public BankPaymentService(PayrollSnapshotRepository snapshotRepo,
                               CompanyRepository companyRepo,
                               EmployeeRepository employeeRepo) {
        this.snapshotRepo = snapshotRepo;
        this.companyRepo = companyRepo;
        this.employeeRepo = employeeRepo;
    }

    public record PaymentRecord(
            String employeeId,
            String employeeName,
            String iban,
            String bic,
            BigDecimal amount,
            String description,
            List<String> warnings
    ) {}

    public record PaymentFileResult(
            String fileName,
            String fileContent,
            int recordCount,
            BigDecimal totalAmount,
            List<PaymentRecord> records
    ) {}

    public List<PaymentRecord> preview(String tenantId, int year, int month) {
        List<PayrollSnapshot> snapshots = getSnapshots(tenantId, year, month);
        List<PaymentRecord> records = new ArrayList<>();

        for (PayrollSnapshot s : snapshots) {
            Employee emp = employeeRepo.findById(s.getEmployeeId()).orElse(null);
            if (emp == null) continue;

            BigDecimal netSalary = s.getNetSalary() != null ? s.getNetSalary() : BigDecimal.ZERO;
            if (netSalary.compareTo(BigDecimal.ZERO) <= 0) continue;

            List<String> warnings = new ArrayList<>();
            String iban = emp.getIban() != null ? emp.getIban() : "";
            String bic = emp.getBic() != null ? emp.getBic() : "";
            if (iban.isEmpty()) warnings.add("Липсва IBAN");
            if (bic.isEmpty()) warnings.add("Липсва BIC");

            String description = String.format("Заплата %02d/%d", month, year);

            records.add(new PaymentRecord(
                    emp.getId(), emp.getFullName(), iban, bic,
                    netSalary.setScale(2, RoundingMode.HALF_UP), description, warnings));
        }
        return records;
    }

    public PaymentFileResult generateFile(String tenantId, int year, int month) {
        Company company = companyRepo.findById(tenantId).orElse(null);
        if (company == null) throw new RuntimeException("Фирмата не е намерена");

        List<PaymentRecord> records = preview(tenantId, year, month);
        if (records.isEmpty()) throw new RuntimeException("Няма плащания за генериране");

        StringBuilder sb = new StringBuilder();
        // Header
        sb.append("IBAN;BIC;Сума;Получател;Основание\r\n");

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (PaymentRecord r : records) {
            if (r.iban().isEmpty()) continue; // пропусни без IBAN
            sb.append(r.iban()).append(";");
            sb.append(r.bic()).append(";");
            sb.append(r.amount().toPlainString()).append(";");
            sb.append(r.employeeName()).append(";");
            sb.append(r.description()).append("\r\n");
            total = total.add(r.amount());
            count++;
        }

        String bulstat = company.getBulstat() != null ? company.getBulstat() : "UNKNOWN";
        String fileName = String.format("SALARY_%s_%d_%02d.CSV", bulstat, year, month);

        return new PaymentFileResult(fileName, sb.toString(), count, total, records);
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
