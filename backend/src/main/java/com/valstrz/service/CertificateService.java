package com.valstrz.service;

import com.valstrz.entity.company.Company;
import com.valstrz.entity.payroll.PayrollSnapshot;
import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.Employment;
import com.valstrz.repository.CompanyRepository;
import com.valstrz.repository.EmployeeRepository;
import com.valstrz.repository.EmploymentRepository;
import com.valstrz.repository.PayrollSnapshotRepository;
import com.valstrz.util.MoneyUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * Генериране на удостоверение УП-2 за осигурителен доход.
 * Агрегира PayrollSnapshot данни за диапазон от месеци.
 */
@Service
public class CertificateService {

    private final PayrollSnapshotRepository snapshotRepo;
    private final EmployeeRepository employeeRepo;
    private final EmploymentRepository employmentRepo;
    private final CompanyRepository companyRepo;

    public CertificateService(PayrollSnapshotRepository snapshotRepo,
                               EmployeeRepository employeeRepo,
                               EmploymentRepository employmentRepo,
                               CompanyRepository companyRepo) {
        this.snapshotRepo = snapshotRepo;
        this.employeeRepo = employeeRepo;
        this.employmentRepo = employmentRepo;
        this.companyRepo = companyRepo;
    }

    public record MonthlyInsuranceData(
            int year, int month,
            BigDecimal insurableIncome,
            int workedDays, int totalInsuredDays
    ) {}

    public record UP2Data(
            String employeeId, String employeeName, String egn,
            String companyName, String bulstat,
            int fromYear, int fromMonth, int toYear, int toMonth,
            List<MonthlyInsuranceData> months,
            BigDecimal totalIncome, int totalDays,
            String html
    ) {}

    public List<MonthlyInsuranceData> getInsuranceIncomeData(String tenantId, String employeeId,
                                                               int fromYear, int fromMonth,
                                                               int toYear, int toMonth) {
        List<MonthlyInsuranceData> result = new ArrayList<>();

        int y = fromYear;
        int m = fromMonth;
        while (y < toYear || (y == toYear && m <= toMonth)) {
            List<PayrollSnapshot> snapshots = StreamSupport.stream(
                    snapshotRepo.findByTenantIdAndYearAndMonthAndStatus(tenantId, y, m, "CLOSED").spliterator(), false
            ).toList();

            if (snapshots.isEmpty()) {
                snapshots = StreamSupport.stream(
                        snapshotRepo.findByTenantIdAndYearAndMonthAndStatus(tenantId, y, m, "CALCULATED").spliterator(), false
                ).toList();
            }

            PayrollSnapshot match = snapshots.stream()
                    .filter(s -> employeeId.equals(s.getEmployeeId()))
                    .findFirst().orElse(null);

            if (match != null) {
                BigDecimal income = match.getInsurableIncome() != null ? match.getInsurableIncome() : BigDecimal.ZERO;
                Map<String, Object> tsData = match.getTimesheetData();
                int workedDays = 0;
                int totalDays = 0;
                if (tsData != null) {
                    workedDays = tsData.get("workedDays") instanceof Number n ? n.intValue() : 0;
                    totalDays = tsData.get("totalInsuredDays") instanceof Number n ? n.intValue() : 0;
                }
                result.add(new MonthlyInsuranceData(y, m, MoneyUtil.round(income), workedDays, totalDays));
            }

            m++;
            if (m > 12) { m = 1; y++; }
        }
        return result;
    }

    public UP2Data generateUP2(String tenantId, String employeeId,
                                int fromYear, int fromMonth,
                                int toYear, int toMonth) {
        Employee emp = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Служителят не е намерен"));
        Company company = companyRepo.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Фирмата не е намерена"));

        List<MonthlyInsuranceData> months = getInsuranceIncomeData(tenantId, employeeId,
                fromYear, fromMonth, toYear, toMonth);

        BigDecimal totalIncome = BigDecimal.ZERO;
        int totalDays = 0;
        for (MonthlyInsuranceData md : months) {
            totalIncome = MoneyUtil.add(totalIncome, md.insurableIncome());
            totalDays += md.totalInsuredDays();
        }

        String html = buildUP2Html(company, emp, months, totalIncome, totalDays,
                fromYear, fromMonth, toYear, toMonth);

        return new UP2Data(
                employeeId, emp.getFullName(), emp.getEgn(),
                company.getName(), company.getBulstat(),
                fromYear, fromMonth, toYear, toMonth,
                months, MoneyUtil.round(totalIncome), totalDays, html
        );
    }

    private String buildUP2Html(Company company, Employee emp,
                                 List<MonthlyInsuranceData> months,
                                 BigDecimal totalIncome, int totalDays,
                                 int fromYear, int fromMonth, int toYear, int toMonth) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body{font-family:Arial,sans-serif;font-size:12px;margin:20px;}");
        sb.append("h2{text-align:center;margin-bottom:5px;}");
        sb.append("h3{text-align:center;margin-top:0;}");
        sb.append("table{border-collapse:collapse;width:100%;margin:10px 0;}");
        sb.append("th,td{border:1px solid #000;padding:4px 8px;text-align:center;}");
        sb.append("th{background:#f0f0f0;}");
        sb.append(".info{margin:5px 0;}");
        sb.append(".signatures{margin-top:40px;display:flex;justify-content:space-between;}");
        sb.append(".sig-block{text-align:center;width:200px;}");
        sb.append("</style></head><body>");

        sb.append("<h2>УДОСТОВЕРЕНИЕ</h2>");
        sb.append("<h3>Обр. УП-2</h3>");
        sb.append("<h3>за осигурителен доход</h3>");

        sb.append("<div class='info'><strong>Предприятие:</strong> ")
                .append(esc(company.getName())).append("</div>");
        sb.append("<div class='info'><strong>БУЛСТАТ:</strong> ")
                .append(esc(company.getBulstat())).append("</div>");
        sb.append("<div class='info'><strong>Служител:</strong> ")
                .append(esc(emp.getFullName())).append("</div>");
        sb.append("<div class='info'><strong>ЕГН:</strong> ")
                .append(esc(emp.getEgn())).append("</div>");
        sb.append("<div class='info'><strong>Период:</strong> ")
                .append(String.format("%02d/%d - %02d/%d", fromMonth, fromYear, toMonth, toYear))
                .append("</div>");

        sb.append("<table>");
        sb.append("<tr><th>Година</th><th>Месец</th><th>Осиг. доход</th><th>Отработени дни</th><th>Осиг. дни</th></tr>");

        for (MonthlyInsuranceData md : months) {
            sb.append("<tr>");
            sb.append("<td>").append(md.year()).append("</td>");
            sb.append("<td>").append(String.format("%02d", md.month())).append("</td>");
            sb.append("<td>").append(md.insurableIncome().toPlainString()).append("</td>");
            sb.append("<td>").append(md.workedDays()).append("</td>");
            sb.append("<td>").append(md.totalInsuredDays()).append("</td>");
            sb.append("</tr>");
        }

        sb.append("<tr style='font-weight:bold;'>");
        sb.append("<td colspan='2'>ОБЩО</td>");
        sb.append("<td>").append(MoneyUtil.round(totalIncome).toPlainString()).append("</td>");
        sb.append("<td></td>");
        sb.append("<td>").append(totalDays).append("</td>");
        sb.append("</tr>");
        sb.append("</table>");

        sb.append("<div class='signatures'>");
        sb.append("<div class='sig-block'><div>Ръководител</div><div>_________________</div><div>")
                .append(esc(company.getHrManagerName() != null ? company.getHrManagerName() : "")).append("</div></div>");
        sb.append("<div class='sig-block'><div>Гл. счетоводител</div><div>_________________</div><div>")
                .append(esc(company.getChiefAccountantName() != null ? company.getChiefAccountantName() : "")).append("</div></div>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════
    // УП-3 — Удостоверение за осигурителен стаж
    // ═══════════════════════════════════════════════════════

    public record SeniorityPeriod(
            LocalDate fromDate, LocalDate toDate,
            String jobTitle, String nkpdCode,
            int insuranceCategory,  // 1, 2 или 3 категория труд
            int years, int months, int days
    ) {}

    public record UP3Data(
            String employeeId, String employeeName, String egn,
            String companyName, String bulstat,
            LocalDate fromDate, LocalDate toDate,
            List<SeniorityPeriod> periods,
            int totalYears, int totalMonths, int totalDays,
            String html
    ) {}

    public UP3Data generateUP3(String tenantId, String employeeId,
                                LocalDate fromDate, LocalDate toDate) {
        Employee emp = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Служителят не е намерен"));
        Company company = companyRepo.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Фирмата не е намерена"));

        List<Employment> employments = StreamSupport.stream(
                employmentRepo.findByTenantIdAndEmployeeId(tenantId, employeeId).spliterator(), false).toList();

        List<SeniorityPeriod> periods = new ArrayList<>();
        int totalYears = 0, totalMonths = 0, totalDays = 0;

        for (Employment empl : employments) {
            LocalDate start = empl.getStartDate();
            if (start == null) continue;

            LocalDate end = empl.getTerminationDate() != null ? empl.getTerminationDate() : toDate;

            // Пресичаме с искания период
            if (start.isBefore(fromDate)) start = fromDate;
            if (end.isAfter(toDate)) end = toDate;
            if (start.isAfter(end)) continue;

            // Изчисляване на стаж: години, месеци, дни
            long totalCalDays = ChronoUnit.DAYS.between(start, end) + 1;
            int y = 0, m = 0, d = 0;
            LocalDate temp = start;
            while (temp.plusYears(1).minusDays(1).isBefore(end) || temp.plusYears(1).minusDays(1).isEqual(end)) {
                y++;
                temp = temp.plusYears(1);
            }
            while (temp.plusMonths(1).minusDays(1).isBefore(end) || temp.plusMonths(1).minusDays(1).isEqual(end)) {
                m++;
                temp = temp.plusMonths(1);
            }
            d = (int) ChronoUnit.DAYS.between(temp, end) + 1;
            if (d >= 30) { m++; d = 0; }
            if (m >= 12) { y++; m -= 12; }

            int category = 3; // по подразбиране трета категория
            periods.add(new SeniorityPeriod(start, end,
                    empl.getJobTitle() != null ? empl.getJobTitle() : "",
                    empl.getNkpdCode() != null ? empl.getNkpdCode() : "",
                    category, y, m, d));

            totalDays += d;
            totalMonths += m;
            totalYears += y;
        }

        // Нормализиране
        totalMonths += totalDays / 30;
        totalDays = totalDays % 30;
        totalYears += totalMonths / 12;
        totalMonths = totalMonths % 12;

        String html = buildUP3Html(company, emp, periods,
                totalYears, totalMonths, totalDays, fromDate, toDate);

        return new UP3Data(employeeId, emp.getFullName(), emp.getEgn(),
                company.getName(), company.getBulstat(),
                fromDate, toDate, periods,
                totalYears, totalMonths, totalDays, html);
    }

    private String buildUP3Html(Company company, Employee emp,
                                 List<SeniorityPeriod> periods,
                                 int totalYears, int totalMonths, int totalDays,
                                 LocalDate fromDate, LocalDate toDate) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body{font-family:Arial,sans-serif;font-size:12px;margin:20px;}");
        sb.append("h2{text-align:center;margin-bottom:5px;}");
        sb.append("h3{text-align:center;margin-top:0;}");
        sb.append("table{border-collapse:collapse;width:100%;margin:10px 0;}");
        sb.append("th,td{border:1px solid #000;padding:4px 8px;text-align:center;}");
        sb.append("th{background:#f0f0f0;}");
        sb.append(".info{margin:5px 0;}");
        sb.append(".signatures{margin-top:40px;display:flex;justify-content:space-between;}");
        sb.append(".sig-block{text-align:center;width:200px;}");
        sb.append("</style></head><body>");

        sb.append("<h2>УДОСТОВЕРЕНИЕ</h2>");
        sb.append("<h3>Обр. УП-3</h3>");
        sb.append("<h3>за осигурителен (трудов) стаж</h3>");

        sb.append("<div class='info'><strong>Предприятие:</strong> ")
                .append(esc(company.getName())).append("</div>");
        sb.append("<div class='info'><strong>БУЛСТАТ:</strong> ")
                .append(esc(company.getBulstat())).append("</div>");
        sb.append("<div class='info'><strong>Служител:</strong> ")
                .append(esc(emp.getFullName())).append("</div>");
        sb.append("<div class='info'><strong>ЕГН:</strong> ")
                .append(esc(emp.getEgn())).append("</div>");
        sb.append("<div class='info'><strong>Период:</strong> ")
                .append(fromDate).append(" — ").append(toDate).append("</div>");

        sb.append("<table>");
        sb.append("<tr><th>От дата</th><th>До дата</th><th>Длъжност</th><th>НКПД</th><th>Категория</th><th>Години</th><th>Месеци</th><th>Дни</th></tr>");

        for (SeniorityPeriod p : periods) {
            sb.append("<tr>");
            sb.append("<td>").append(p.fromDate()).append("</td>");
            sb.append("<td>").append(p.toDate()).append("</td>");
            sb.append("<td style='text-align:left;'>").append(esc(p.jobTitle())).append("</td>");
            sb.append("<td>").append(esc(p.nkpdCode())).append("</td>");
            sb.append("<td>").append(p.insuranceCategory()).append("-та</td>");
            sb.append("<td>").append(p.years()).append("</td>");
            sb.append("<td>").append(p.months()).append("</td>");
            sb.append("<td>").append(p.days()).append("</td>");
            sb.append("</tr>");
        }

        sb.append("<tr style='font-weight:bold;'>");
        sb.append("<td colspan='5'>ОБЩО осигурителен стаж</td>");
        sb.append("<td>").append(totalYears).append("</td>");
        sb.append("<td>").append(totalMonths).append("</td>");
        sb.append("<td>").append(totalDays).append("</td>");
        sb.append("</tr>");
        sb.append("</table>");

        sb.append("<p>Удостоверението се издава на основание чл. 5, ал. 7 от КСО, ")
                .append("за да послужи пред ТП на НОИ.</p>");

        sb.append("<div class='signatures'>");
        sb.append("<div class='sig-block'><div>Ръководител</div><div>_________________</div><div>")
                .append(esc(company.getHrManagerName() != null ? company.getHrManagerName() : "")).append("</div></div>");
        sb.append("<div class='sig-block'><div>Гл. счетоводител</div><div>_________________</div><div>")
                .append(esc(company.getChiefAccountantName() != null ? company.getChiefAccountantName() : "")).append("</div></div>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
