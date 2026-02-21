package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.payroll.AccountingEntry;

public interface AccountingEntryRepository extends ArangoRepository<AccountingEntry, String> {
    Iterable<AccountingEntry> findByTenantIdAndYearAndMonth(String tenantId, int year, int month);
}
