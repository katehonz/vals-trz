package com.valstrz.service;

import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.repository.EmployeeRepository;
import com.valstrz.repository.EmploymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class EmployeeImportService {

    private final EmployeeRepository employeeRepository;
    private final EmploymentRepository employmentRepository;

    public EmployeeImportService(EmployeeRepository employeeRepository, EmploymentRepository employmentRepository) {
        this.employeeRepository = employeeRepository;
        this.employmentRepository = employmentRepository;
    }

    public void importEmployees(String tenantId, MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (isFirstLine) {
                    if (line.toLowerCase().contains("egn")) {
                        isFirstLine = false;
                        continue;
                    }
                    isFirstLine = false;
                }

                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (parts.length >= 9) {
                    processLine(tenantId, parts);
                }
            }
        }
    }

    private void processLine(String tenantId, String[] parts) {
        String egn = parts[0].trim();
        String firstName = parts[1].trim();
        String middleName = parts[2].trim();
        String lastName = parts[3].trim();
        LocalDate birthDate = parseDate(parts[4].trim());
        String jobTitle = parts[5].trim();
        String nkpd = parts[6].trim();
        String kid = parts[7].trim();
        BigDecimal salary = parseDecimal(parts[8].trim());
        LocalDate contractDate = parseDate(parts.length > 9 ? parts[9].trim() : LocalDate.now().toString());
        LocalDate startDate = parseDate(parts.length > 10 ? parts[10].trim() : contractDate.toString());

        // Find or create Employee
        Iterable<Employee> existing = employeeRepository.findByTenantIdAndEgn(tenantId, egn);
        Employee employee;
        if (existing.iterator().hasNext()) {
            employee = existing.iterator().next();
        } else {
            employee = new Employee();
            employee.setTenantId(tenantId);
            employee.setEgn(egn);
            employee.setActive(true);
        }
        
        employee.setFirstName(firstName);
        employee.setMiddleName(middleName);
        employee.setLastName(lastName);
        employee.setBirthDate(birthDate);
        
        employee = employeeRepository.save(employee);

        // Create/Update Employment
        Iterable<Employment> employments = employmentRepository.findByTenantIdAndEmployeeIdAndCurrent(tenantId, employee.getId(), true);
        Employment employment;
        if (employments.iterator().hasNext()) {
            employment = employments.iterator().next();
        } else {
            employment = new Employment();
            employment.setTenantId(tenantId);
            employment.setEmployeeId(employee.getId());
            employment.setCurrent(true);
        }

        employment.setJobTitle(jobTitle);
        employment.setNkpdCode(nkpd);
        employment.setKidCode(kid);
        employment.setBaseSalary(salary);
        employment.setContractDate(contractDate);
        employment.setStartDate(startDate);
        if (employment.getContractNumber() == null) {
             employment.setContractNumber("IMP-" + egn);
        }

        employmentRepository.save(employment);
    }

    private LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr); // Expects YYYY-MM-DD
        } catch (DateTimeParseException | NullPointerException e) {
            return LocalDate.now();
        }
    }

    private BigDecimal parseDecimal(String str) {
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
