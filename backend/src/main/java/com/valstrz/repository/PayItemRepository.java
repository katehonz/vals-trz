package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.payroll.PayItem;

public interface PayItemRepository extends ArangoRepository<PayItem, String> {
    Iterable<PayItem> findByTenantId(String tenantId);
    Iterable<PayItem> findByTenantIdAndActive(String tenantId, boolean active);
}
