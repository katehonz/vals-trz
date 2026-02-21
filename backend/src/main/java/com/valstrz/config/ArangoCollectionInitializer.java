package com.valstrz.config;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ArangoCollectionInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ArangoCollectionInitializer.class);

    private static final List<String> COLLECTIONS = List.of(
            "companies", "users", "auditLogs",
            "workSchedules", "shiftSchedules", "monthlyCalendars", "annualCalendars",
            "nomenclatures", "economicActivities",
            "seniorityBonusConfigs", "personnelTypes",
            "departments",
            "employees", "employments", "amendments", "terminations",
            "absences", "leaveEntitlements", "monthlyTimesheets",
            "employeePayItems", "employeeDeductions",
            "insuranceRates", "insuranceContributions", "insuranceThresholds",
            "payItems", "deductionItems", "payrolls", "payrollSnapshots",
            "monthClosingSnapshots", "accountingEntries",
            "documentTemplates", "napSubmissions"
    );

    @Value("${arangodb.spring.data.hosts:localhost:8529}")
    private String hosts;

    @Value("${arangodb.spring.data.user:root}")
    private String user;

    @Value("${arangodb.spring.data.password:}")
    private String password;

    @Value("${arangodb.spring.data.database:valstrz}")
    private String database;

    @Override
    public void run(String... args) {
        String[] hostParts = hosts.split(":");
        ArangoDB arangoDB = new ArangoDB.Builder()
                .host(hostParts[0], Integer.parseInt(hostParts[1]))
                .user(user)
                .password(password)
                .build();

        try {
            ArangoDatabase db = arangoDB.db(database);
            Set<String> existing = db.getCollections().stream()
                    .filter(c -> !c.getIsSystem())
                    .map(c -> c.getName())
                    .collect(Collectors.toSet());

            int created = 0;
            for (String col : COLLECTIONS) {
                if (!existing.contains(col)) {
                    db.createCollection(col);
                    log.info("Created ArangoDB collection: {}", col);
                    created++;
                }
            }
            if (created > 0) {
                log.info("Created {} new ArangoDB collections", created);
            }
        } finally {
            arangoDB.shutdown();
        }
    }
}
