package com.valstrz.entity;

import com.arangodb.springframework.annotation.Document;

import java.util.Set;

@Document("users")
public class User extends BaseEntity {

    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private Set<String> roles;  // ADMIN, ACCOUNTANT, HR_MANAGER, VIEWER
    private boolean active = true;

    public User() {}

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
