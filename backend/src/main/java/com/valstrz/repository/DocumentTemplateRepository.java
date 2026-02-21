package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.document.DocumentTemplate;

public interface DocumentTemplateRepository extends ArangoRepository<DocumentTemplate, String> {

    Iterable<DocumentTemplate> findByTenantId(String tenantId);

    Iterable<DocumentTemplate> findByTenantIdAndCategory(String tenantId, String category);

    Iterable<DocumentTemplate> findByTenantIdAndActive(String tenantId, boolean active);

    Iterable<DocumentTemplate> findByTenantIdAndCategoryAndSystemTrue(String tenantId, String category);
}
