package com.glazr.permission.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permissions")
public class PermissionEntity {

    @Id
    @Column(name = "key", length = 80, nullable = false)
    private String key;

    @Column(name = "module", length = 60, nullable = false)
    private String module;

    @Column(name = "description", length = 300, nullable = false)
    private String description;

    protected PermissionEntity() {
    }

    public PermissionEntity(String key, String module, String description) {
        this.key = key;
        this.module = module;
        this.description = description;
    }

    public String getKey() { return key; }
    public String getModule() { return module; }
    public String getDescription() { return description; }
}
