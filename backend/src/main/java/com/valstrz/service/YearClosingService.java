package com.valstrz.service;

import com.valstrz.entity.personnel.Employee;
import com.valstrz.entity.personnel.LeaveEntitlement;
import com.valstrz.repository.EmployeeRepository;
import com.valstrz.repository.LeaveEntitlementRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class YearClosingService {

    private final EmployeeRepository employeeRepository;
    private final LeaveEntitlementRepository leaveEntitlementRepository;
    private final MonthlyCalendarService calendarService;
    private final AuditService auditService;

    public YearClosingService(EmployeeRepository employeeRepository,
                               LeaveEntitlementRepository leaveEntitlementRepository,
                               MonthlyCalendarService calendarService,
                               AuditService auditService) {
        this.employeeRepository = employeeRepository;
        this.leaveEntitlementRepository = leaveEntitlementRepository;
        this.calendarService = calendarService;
        this.auditService = auditService;
    }

    /**
     * Prepares new year: transfers unused leave, generates January calendar.
     */
    public YearClosingResult closeYear(String tenantId, int closingYear) {
        int newYear = closingYear + 1;
        List<String> actions = new ArrayList<>();

        // 1. Transfer unused leave days
        Iterable<Employee> employees = employeeRepository.findByTenantIdAndActive(tenantId, true);
        int transferredCount = 0;
        for (Employee emp : employees) {
            boolean transferred = transferLeave(tenantId, emp.getId(), closingYear, newYear);
            if (transferred) transferredCount++;
        }
        actions.add("Прехвърлени отпуски за " + transferredCount + " служител(и).");

        // 2. Generate January calendar for new year
        calendarService.generateCalendar(tenantId, newYear, 1);
        actions.add("Генериран календар за Януари " + newYear + ".");

        // 3. Log
        auditService.log(tenantId, "YEAR_CLOSE", "Year", String.valueOf(closingYear),
                "Годишно приключване " + closingYear, java.util.Map.of(
                        "closingYear", closingYear,
                        "newYear", newYear,
                        "transferredLeave", transferredCount
                ));

        return new YearClosingResult(closingYear, newYear, actions);
    }

    private boolean transferLeave(String tenantId, String employeeId, int fromYear, int toYear) {
        Iterable<LeaveEntitlement> entitlements = leaveEntitlementRepository
                .findByTenantIdAndEmployeeIdAndYear(tenantId, employeeId, fromYear);

        boolean hadTransfer = false;
        for (LeaveEntitlement le : entitlements) {
            int remaining = le.getTotalEntitled() - le.getTotalUsed();
            if (remaining > 0) {
                // Create new year entitlement with carried-over days
                LeaveEntitlement newLe = new LeaveEntitlement();
                newLe.setTenantId(tenantId);
                newLe.setEmployeeId(employeeId);
                newLe.setYear(toYear);
                newLe.setBasicLeaveDays(le.getBasicLeaveDays());
                newLe.setAdditionalLeaveDays(le.getAdditionalLeaveDays());
                newLe.setIrregularLeaveDays(le.getIrregularLeaveDays());
                newLe.setAgreedLeaveDays(le.getAgreedLeaveDays());
                newLe.setCarriedOverDays(remaining);
                leaveEntitlementRepository.save(newLe);
                hadTransfer = true;
            }
        }
        return hadTransfer;
    }

    public static class YearClosingResult {
        private final int closedYear;
        private final int newYear;
        private final List<String> actions;

        public YearClosingResult(int closedYear, int newYear, List<String> actions) {
            this.closedYear = closedYear;
            this.newYear = newYear;
            this.actions = actions;
        }

        public int getClosedYear() { return closedYear; }
        public int getNewYear() { return newYear; }
        public List<String> getActions() { return actions; }
    }
}
