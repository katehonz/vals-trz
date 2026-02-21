package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.personnel.Garnishment;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GarnishmentRepository extends ArangoRepository<Garnishment, String> {
    List<Garnishment> findByEmployeeId(String employeeId);
}
