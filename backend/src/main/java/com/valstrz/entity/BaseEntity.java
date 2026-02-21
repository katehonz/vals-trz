package com.valstrz.entity;

import com.arangodb.springframework.annotation.ArangoId;
import org.springframework.data.annotation.Id;

/**
 * Base entity with common fields for all ArangoDB documents.
 * All entities include tenantId for multi-tenant support (per company).
 */
public abstract class BaseEntity {

    @Id
    private String id;

    @ArangoId
    private String arangoId;

    private String tenantId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getArangoId() { return arangoId; }
    public void setArangoId(String arangoId) { this.arangoId = arangoId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
