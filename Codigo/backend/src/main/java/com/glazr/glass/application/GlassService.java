package com.glazr.glass.application;

import com.glazr.glass.api.CreateGlassRequest;
import com.glazr.glass.api.GlassResponse;
import com.glazr.glass.api.UpdateGlassRequest;
import com.glazr.glass.infrastructure.GlassEntity;
import com.glazr.glass.infrastructure.GlassRepository;
import com.glazr.shared.exception.ConflictException;
import com.glazr.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

@Service
public class GlassService {

    private final GlassRepository glasses;
    private final Clock clock;

    public GlassService(GlassRepository glasses, Clock clock) {
        this.glasses = glasses;
        this.clock = clock;
    }

    @PreAuthorize("hasAuthority('GLASS_CREATE')")
    @Transactional
    public GlassResponse create(CreateGlassRequest request) {
        rejectDuplicate(request.name(), request.thicknessMm(), request.finish(), request.color(), null);
        GlassEntity glass = GlassEntity.create(
                request.name(),
                request.thicknessMm(),
                request.finish(),
                request.color(),
                request.pricePerSquareMeter(),
                clock.instant()
        );
        return GlassResponse.from(glasses.saveAndFlush(glass));
    }

    @PreAuthorize("hasAuthority('GLASS_READ')")
    @Transactional(readOnly = true)
    public Page<GlassResponse> list(String query, BigDecimal thicknessMm, String finish, Boolean active, Pageable pageable) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.strip() : null;
        String cleanFinish = (finish != null && !finish.isBlank()) ? finish.strip() : null;
        Page<GlassEntity> result = glasses.search(cleanQuery, thicknessMm, cleanFinish, active, pageable);
        return result.map(GlassResponse::from);
    }

    @PreAuthorize("hasAuthority('GLASS_READ')")
    @Transactional(readOnly = true)
    public GlassResponse get(UUID id) {
        return GlassResponse.from(requireGlass(id));
    }

    @PreAuthorize("hasAuthority('GLASS_UPDATE')")
    @Transactional
    public GlassResponse update(UUID id, UpdateGlassRequest request) {
        GlassEntity glass = requireGlass(id);
        rejectDuplicate(request.name(), request.thicknessMm(), request.finish(), request.color(), id);
        glass.update(
                request.name(),
                request.thicknessMm(),
                request.finish(),
                request.color(),
                request.pricePerSquareMeter(),
                clock.instant()
        );
        return GlassResponse.from(glasses.saveAndFlush(glass));
    }

    @PreAuthorize("hasAuthority('GLASS_UPDATE')")
    @Transactional
    public GlassResponse updateStatus(UUID id, boolean active) {
        GlassEntity glass = requireGlass(id);
        glass.updateStatus(active, clock.instant());
        return GlassResponse.from(glasses.saveAndFlush(glass));
    }

    @PreAuthorize("hasAuthority('GLASS_DELETE')")
    @Transactional
    public void delete(UUID id) {
        GlassEntity glass = requireGlass(id);
        glasses.delete(glass);
        glasses.flush();
    }

    private GlassEntity requireGlass(UUID id) {
        return glasses.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vidro"));
    }

    private void rejectDuplicate(String name, BigDecimal thicknessMm, String finish, String color, UUID currentId) {
        glasses.findByNameIgnoreCaseAndThicknessMmAndFinishIgnoreCaseAndColorIgnoreCase(
                name.strip(), thicknessMm, finish.strip(), color.strip()
        ).filter(g -> !g.getId().equals(currentId)).ifPresent(g -> {
            throw new ConflictException("DUPLICATE_GLASS", "Já existe um vidro cadastrado com este nome, espessura, acabamento e cor");
        });
    }
}
