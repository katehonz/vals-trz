package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.nomenclature.Nomenclature;

public interface NomenclatureRepository extends ArangoRepository<Nomenclature, String> {
    Iterable<Nomenclature> findByTenantId(String tenantId);
    Iterable<Nomenclature> findByTenantIdAndCode(String tenantId, String code);
}
