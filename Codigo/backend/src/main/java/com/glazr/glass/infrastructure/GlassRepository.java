package com.glazr.glass.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface GlassRepository extends JpaRepository<GlassEntity, UUID> {

    Optional<GlassEntity> findByNameIgnoreCaseAndThicknessMmAndFinishIgnoreCaseAndColorIgnoreCase(
            String name, BigDecimal thicknessMm, String finish, String color);

    @Query("""
        SELECT g FROM GlassEntity g
        WHERE (:query IS NULL OR :query = '' OR LOWER(g.name) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:thicknessMm IS NULL OR g.thicknessMm = :thicknessMm)
          AND (:finish IS NULL OR :finish = '' OR LOWER(g.finish) = LOWER(:finish))
          AND (:active IS NULL OR g.active = :active)
    """)
    Page<GlassEntity> search(@Param("query") String query,
                             @Param("thicknessMm") BigDecimal thicknessMm,
                             @Param("finish") String finish,
                             @Param("active") Boolean active,
                             Pageable pageable);
}
