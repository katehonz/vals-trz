package com.valstrz.service;

import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.entity.personnel.Termination;
import com.valstrz.repository.EmployeeRepository;
import com.valstrz.repository.EmploymentRepository;
import com.valstrz.repository.TerminationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * Анализ на персонала - демография, разпределение, текучество.
 */
@Service
public class PersonnelAnalyticsService {

    private final EmployeeRepository employeeRepository;
    private final EmploymentRepository employmentRepository;
    private final TerminationRepository terminationRepository;

    public PersonnelAnalyticsService(EmployeeRepository employeeRepository,
                                      EmploymentRepository employmentRepository,
                                      TerminationRepository terminationRepository) {
        this.employeeRepository = employeeRepository;
        this.employmentRepository = employmentRepository;
        this.terminationRepository = terminationRepository;
    }

    public PersonnelReport getReport(String tenantId) {
        List<Employee> all = StreamSupport.stream(
                employeeRepository.findByTenantId(tenantId).spliterator(), false).toList();
        List<Employee> active = all.stream().filter(Employee::isActive).toList();
        List<Employee> inactive = all.stream().filter(e -> !e.isActive()).toList();

        List<Employment> employments = StreamSupport.stream(
                employmentRepository.findByTenantId(tenantId).spliterator(), false).toList();

        // Разпределение по пол
        Map<String, Integer> genderDistribution = new LinkedHashMap<>();
        for (Employee e : active) {
            String g = e.getGender() != null ? e.getGender() : "Неуказан";
            genderDistribution.merge(g, 1, Integer::sum);
        }

        // Разпределение по възраст
        Map<String, Integer> ageDistribution = new LinkedHashMap<>();
        ageDistribution.put("до 25", 0);
        ageDistribution.put("25-34", 0);
        ageDistribution.put("35-44", 0);
        ageDistribution.put("45-54", 0);
        ageDistribution.put("55-64", 0);
        ageDistribution.put("65+", 0);

        for (Employee e : active) {
            int age = calculateAge(e);
            if (age < 0) continue;
            String group;
            if (age < 25) group = "до 25";
            else if (age < 35) group = "25-34";
            else if (age < 45) group = "35-44";
            else if (age < 55) group = "45-54";
            else if (age < 65) group = "55-64";
            else group = "65+";
            ageDistribution.merge(group, 1, Integer::sum);
        }

        // Разпределение по образование
        Map<String, Integer> educationDistribution = new LinkedHashMap<>();
        Map<Integer, String> eduLabels = Map.of(
                1, "Начално", 2, "Основно", 3, "Средно",
                4, "Средно специално", 5, "Полувисше",
                6, "Висше - бакалавър", 7, "Висше - магистър/доктор");
        for (Employee e : active) {
            String label = eduLabels.getOrDefault(e.getEducationCode(), "Неуказано");
            educationDistribution.merge(label, 1, Integer::sum);
        }

        // Текучество - наети/напуснали за текущата година
        int currentYear = LocalDate.now().getYear();
        int hiredThisYear = 0;
        int terminatedThisYear = 0;
        for (Employment empl : employments) {
            if (empl.getStartDate() != null && empl.getStartDate().getYear() == currentYear) {
                hiredThisYear++;
            }
        }
        List<Termination> terminations = StreamSupport.stream(
                terminationRepository.findByTenantId(tenantId).spliterator(), false).toList();
        for (Termination t : terminations) {
            if (t.getLastWorkDay() != null && t.getLastWorkDay().getYear() == currentYear) {
                terminatedThisYear++;
            }
        }

        // Средна възраст
        double avgAge = active.stream()
                .mapToInt(this::calculateAge)
                .filter(a -> a >= 0)
                .average().orElse(0);

        // Среден стаж (години от startDate)
        double avgSeniority = employments.stream()
                .filter(e -> e.getStartDate() != null && e.isCurrent())
                .mapToDouble(e -> Period.between(e.getStartDate(), LocalDate.now()).getYears())
                .average().orElse(0);

        return new PersonnelReport(
                all.size(), active.size(), inactive.size(),
                genderDistribution, ageDistribution, educationDistribution,
                Math.round(avgAge * 10) / 10.0,
                Math.round(avgSeniority * 10) / 10.0,
                hiredThisYear, terminatedThisYear
        );
    }

    private int calculateAge(Employee e) {
        LocalDate birth = e.getBirthDate();
        if (birth == null && e.getEgn() != null && e.getEgn().length() >= 6) {
            birth = parseBirthDateFromEgn(e.getEgn());
        }
        if (birth == null) return -1;
        return Period.between(birth, LocalDate.now()).getYears();
    }

    private LocalDate parseBirthDateFromEgn(String egn) {
        try {
            int yy = Integer.parseInt(egn.substring(0, 2));
            int mm = Integer.parseInt(egn.substring(2, 4));
            int dd = Integer.parseInt(egn.substring(4, 6));
            int year;
            if (mm > 40) {
                year = 2000 + yy;
                mm -= 40;
            } else if (mm > 20) {
                year = 1800 + yy;
                mm -= 20;
            } else {
                year = 1900 + yy;
            }
            return LocalDate.of(year, mm, dd);
        } catch (Exception ex) {
            return null;
        }
    }

    public record PersonnelReport(
        int totalEmployees,
        int activeEmployees,
        int inactiveEmployees,
        Map<String, Integer> genderDistribution,
        Map<String, Integer> ageDistribution,
        Map<String, Integer> educationDistribution,
        double averageAge,
        double averageSeniority,
        int hiredThisYear,
        int terminatedThisYear
    ) {}
}
