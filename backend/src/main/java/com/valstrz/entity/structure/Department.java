package com.valstrz.entity.structure;

import com.arangodb.springframework.annotation.Document;
import com.valstrz.entity.BaseEntity;

/**
 * Организационна единица (отдел, звено, сектор).
 * Дървовидна йерархия чрез parentId.
 */
@Document("departments")
public class Department extends BaseEntity {

    private String code;
    private String name;
    private String parentId;       // null = корен (самата фирма)
    private String managerId;      // id на служител-ръководител
    private int sortOrder;

    public Department() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
