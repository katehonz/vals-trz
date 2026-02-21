package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.company.PersonInfo;
import com.valstrz.entity.personnel.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless замествач на {{placeholder}} кодове в шаблони за документи.
 */
@Service
public class TemplateSubstitutionService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([a-zA-Z0-9_.]+)\\}\\}");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /**
     * Заменя всички {{key}} в content с values от map.
     * Незаместени placeholder-и остават непроменени (за видимост при debug).
     */
    public String substitute(String content, Map<String, String> variables) {
        if (content == null || content.isEmpty()) return content;

        Matcher m = PLACEHOLDER.matcher(content);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String value = variables.getOrDefault(key, m.group(0));
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Събира всички placeholder стойности от подадените обекти.
     */
    public Map<String, String> buildVariables(Company company, Employee employee,
                                               Employment employment, Amendment amendment,
                                               Termination termination, Absence absence) {
        Map<String, String> vars = new LinkedHashMap<>();

        // Дати
        LocalDate today = LocalDate.now();
        vars.put("today", fmt(today));
        vars.put("today.year", String.valueOf(today.getYear()));
        vars.put("today.month", String.valueOf(today.getMonthValue()));

        if (company != null) putCompany(vars, company);
        if (employee != null) putEmployee(vars, employee);
        if (employment != null) putEmployment(vars, employment);
        if (amendment != null) putAmendment(vars, amendment);
        if (termination != null) putTermination(vars, termination);
        if (absence != null) putAbsence(vars, absence);

        return vars;
    }

    private void putCompany(Map<String, String> v, Company c) {
        v.put("company.name", safe(c.getName()));
        v.put("company.bulstat", safe(c.getBulstat()));
        v.put("company.city", safe(c.getCity()));
        v.put("company.postalCode", safe(c.getPostalCode()));
        v.put("company.address", safe(c.getAddress()));
        v.put("company.correspondenceAddress", safe(c.getCorrespondenceAddress()));
        v.put("company.phone", safe(c.getPhone()));
        v.put("company.email", safe(c.getEmail()));
        v.put("company.nkidCode", safe(c.getNkidCode()));

        PersonInfo dir = c.getDirector();
        if (dir != null) {
            v.put("company.director", safe(dir.getName()));
            v.put("company.directorTitle", safe(dir.getTitle()));
            v.put("company.directorEgn", safe(dir.getEgn()));
        } else {
            v.put("company.director", "");
            v.put("company.directorTitle", "");
            v.put("company.directorEgn", "");
        }
        v.put("company.hrManager", safe(c.getHrManagerName()));
        v.put("company.chiefAccountant", safe(c.getChiefAccountantName()));
    }

    private void putEmployee(Map<String, String> v, Employee e) {
        v.put("employee.fullName", safe(e.getFullName()));
        v.put("employee.firstName", safe(e.getFirstName()));
        v.put("employee.middleName", safe(e.getMiddleName()));
        v.put("employee.lastName", safe(e.getLastName()));
        v.put("employee.egn", safe(e.getEgn()));
        v.put("employee.lnch", safe(e.getLnch()));
        v.put("employee.birthDate", fmt(e.getBirthDate()));
        v.put("employee.gender", safe(e.getGender()));
        v.put("employee.citizenship", safe(e.getCitizenship()));
        v.put("employee.address", safe(e.getPermanentAddress()));
        v.put("employee.city", safe(e.getPermanentCity()));
        v.put("employee.municipality", safe(e.getPermanentMunicipality()));
        v.put("employee.region", safe(e.getPermanentRegion()));
        v.put("employee.currentAddress", safe(e.getCurrentAddress()));
        v.put("employee.phone", safe(e.getPhone()));
        v.put("employee.email", safe(e.getEmail()));
        v.put("employee.idCardNumber", safe(e.getIdCardNumber()));
        v.put("employee.idCardDate", fmt(e.getIdCardDate()));
        v.put("employee.idCardIssuedBy", safe(e.getIdCardIssuedBy()));
        v.put("employee.idCard",
                safe(e.getIdCardNumber()) + " / " + fmt(e.getIdCardDate()) + " / " + safe(e.getIdCardIssuedBy()));
        v.put("employee.education", String.valueOf(e.getEducationCode()));
        v.put("employee.specialty", safe(e.getSpecialty()));
        v.put("employee.iban", safe(e.getIban()));
        v.put("employee.bic", safe(e.getBic()));
    }

    private void putEmployment(Map<String, String> v, Employment emp) {
        v.put("employment.contractNumber", safe(emp.getContractNumber()));
        v.put("employment.contractDate", fmt(emp.getContractDate()));
        v.put("employment.startDate", fmt(emp.getStartDate()));
        v.put("employment.contractBasis", safe(emp.getContractBasis()));
        v.put("employment.contractType", safe(emp.getContractType()));
        v.put("employment.contractEndDate", fmt(emp.getContractEndDate()));
        v.put("employment.jobTitle", safe(emp.getJobTitle()));
        v.put("employment.nkpdCode", safe(emp.getNkpdCode()));
        v.put("employment.workplace", safe(emp.getWorkplace()));
        v.put("employment.workTimeType", safe(emp.getWorkTimeType()));
        v.put("employment.workScheduleCode", safe(emp.getWorkScheduleCode()));
        v.put("employment.baseSalary", emp.getBaseSalary() != null ? emp.getBaseSalary().toPlainString() : "");
        v.put("employment.paymentType", safe(emp.getPaymentType()));
        v.put("employment.personnelType", safe(emp.getPersonnelType()));
        v.put("employment.seniorityBonusPercent",
                emp.getSeniorityBonusPercent() != null ? emp.getSeniorityBonusPercent().toPlainString() : "");
        v.put("employment.noticePeriodDays", "");
    }

    private void putAmendment(Map<String, String> v, Amendment a) {
        v.put("amendment.number", safe(a.getNumber()));
        v.put("amendment.date", fmt(a.getDate()));
        v.put("amendment.effectiveDate", fmt(a.getEffectiveDate()));
        v.put("amendment.basis", safe(a.getBasis()));
        v.put("amendment.endDate", fmt(a.getEndDate()));
        v.put("amendment.specificText", safe(a.getSpecificText()));
    }

    private void putTermination(Map<String, String> v, Termination t) {
        v.put("termination.orderNumber", safe(t.getOrderNumber()));
        v.put("termination.orderDate", fmt(t.getOrderDate()));
        v.put("termination.lastWorkDay", fmt(t.getLastWorkDay()));
        v.put("termination.basis", safe(t.getBasis()));
        v.put("termination.specificText", safe(t.getSpecificText()));
    }

    private void putAbsence(Map<String, String> v, Absence a) {
        v.put("absence.type", safe(a.getType()));
        v.put("absence.typeName", safe(a.getTypeName()));
        v.put("absence.fromDate", fmt(a.getFromDate()));
        v.put("absence.toDate", fmt(a.getToDate()));
        v.put("absence.workingDays", String.valueOf(a.getWorkingDays()));
        v.put("absence.calendarDays", String.valueOf(a.getCalendarDays()));
        v.put("absence.orderNumber", safe(a.getOrderNumber()));
        v.put("absence.orderDate", fmt(a.getOrderDate()));
        v.put("absence.notes", safe(a.getNotes()));
    }

    /**
     * Списък на наличните placeholder-и по категория.
     */
    public Map<String, List<String>> getAvailablePlaceholders(String category) {
        Map<String, List<String>> result = new LinkedHashMap<>();

        result.put("Дати", List.of("today", "today.year", "today.month"));

        result.put("Фирма", List.of(
                "company.name", "company.bulstat", "company.city", "company.postalCode",
                "company.address", "company.phone", "company.email",
                "company.director", "company.directorTitle", "company.directorEgn",
                "company.hrManager", "company.chiefAccountant", "company.nkidCode"));

        result.put("Служител", List.of(
                "employee.fullName", "employee.firstName", "employee.middleName", "employee.lastName",
                "employee.egn", "employee.birthDate", "employee.gender", "employee.citizenship",
                "employee.address", "employee.city", "employee.phone", "employee.email",
                "employee.idCard", "employee.idCardNumber", "employee.education", "employee.specialty",
                "employee.iban", "employee.bic"));

        result.put("Правоотношение", List.of(
                "employment.contractNumber", "employment.contractDate", "employment.startDate",
                "employment.contractBasis", "employment.contractType", "employment.contractEndDate",
                "employment.jobTitle", "employment.nkpdCode", "employment.workplace",
                "employment.workTimeType", "employment.baseSalary", "employment.paymentType",
                "employment.personnelType", "employment.seniorityBonusPercent"));

        if ("AMENDMENT".equals(category)) {
            result.put("Допълнително споразумение", List.of(
                    "amendment.number", "amendment.date", "amendment.effectiveDate",
                    "amendment.basis", "amendment.specificText"));
        }
        if ("TERMINATION".equals(category)) {
            result.put("Прекратяване", List.of(
                    "termination.orderNumber", "termination.orderDate",
                    "termination.lastWorkDay", "termination.basis", "termination.specificText"));
        }
        if ("LEAVE_ORDER".equals(category)) {
            result.put("Отпуск", List.of(
                    "absence.typeName", "absence.fromDate", "absence.toDate",
                    "absence.workingDays", "absence.calendarDays",
                    "absence.orderNumber", "absence.orderDate", "absence.notes"));
        }

        return result;
    }

    private static String safe(String s) { return s != null ? s : ""; }

    private static String fmt(LocalDate d) { return d != null ? d.format(DATE_FMT) : ""; }
}
