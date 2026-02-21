package com.valstrz.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.valstrz.entity.User;

import java.util.Optional;

public interface UserRepository extends ArangoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Iterable<User> findByTenantId(String tenantId);
    Iterable<User> findByTenantIdAndActive(String tenantId, boolean active);
}
