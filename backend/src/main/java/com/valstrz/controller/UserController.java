package com.valstrz.controller;

import com.valstrz.entity.User;
import com.valstrz.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/companies/{tenantId}/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public Iterable<User> getAll(@PathVariable String tenantId) {
        return repository.findByTenantId(tenantId);
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String tenantId, @RequestBody User user) {
        if (repository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Потребителското име е заето.");
        }
        user.setTenantId(tenantId);
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(Set.of("VIEWER"));
        }
        user.setActive(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable String tenantId,
                                       @PathVariable String id,
                                       @RequestBody User userUpdate) {
        Optional<User> existingOpt = repository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User existing = existingOpt.get();
        existing.setFullName(userUpdate.getFullName());
        existing.setEmail(userUpdate.getEmail());
        existing.setRoles(userUpdate.getRoles());
        existing.setActive(userUpdate.isActive());

        // Update password only if provided
        if (userUpdate.getPasswordHash() != null && !userUpdate.getPasswordHash().isEmpty()) {
            existing.setPasswordHash(passwordEncoder.encode(userUpdate.getPasswordHash()));
        }

        return ResponseEntity.ok(repository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        Optional<User> userOpt = repository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = userOpt.get();
        user.setActive(false);
        repository.save(user);
        return ResponseEntity.noContent().build();
    }
}
