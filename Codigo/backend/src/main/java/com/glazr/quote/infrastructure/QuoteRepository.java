package com.glazr.quote.infrastructure;

import com.glazr.quote.domain.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface QuoteRepository extends JpaRepository<QuoteEntity, UUID> {

    @Query("""
        SELECT q FROM QuoteEntity q
        WHERE (:customerName = '' OR lower(q.customerName) LIKE concat('%', lower(cast(:customerName as string)), '%'))
          AND (:status IS NULL OR q.status = :status)
        ORDER BY q.createdAt DESC
    """)
    Page<QuoteEntity> search(
            @Param("customerName") String customerName,
            @Param("status") QuoteStatus status,
            Pageable pageable
    );
}

