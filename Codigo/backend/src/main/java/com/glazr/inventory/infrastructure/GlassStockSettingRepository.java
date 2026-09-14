package com.glazr.inventory.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GlassStockSettingRepository extends JpaRepository<GlassStockSettingEntity, UUID> {
}
