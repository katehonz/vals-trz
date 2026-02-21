package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.payroll.DeductionItem;

public interface DeductionItemRepository extends ArangoRepository<DeductionItem, String> {
    Iterable<DeductionItem> findByTenantId(String tenantId);
}
