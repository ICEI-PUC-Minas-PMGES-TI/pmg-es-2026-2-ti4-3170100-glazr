package com.glazr.kit.application;

import com.glazr.kit.api.KitRequest;
import com.glazr.kit.api.KitResponse;
import com.glazr.kit.infrastructure.KitComponentEntity;
import com.glazr.kit.infrastructure.KitEntity;
import com.glazr.kit.infrastructure.KitRepository;
import com.glazr.shared.exception.ConflictException;
import com.glazr.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class KitService {

    private final KitRepository kits;
    private final Clock clock;

    public KitService(KitRepository kits, Clock clock) {
        this.kits = kits;
        this.clock = clock;
    }

    @PreAuthorize("hasAuthority('KIT_CREATE')")
    @Transactional
    public KitResponse create(KitRequest request) {
        rejectDuplicateName(request.name(), null);
        List<KitComponentEntity> components = request.components().stream()
                .map(component -> KitComponentEntity.create(component.name(), component.quantity(), component.notes()))
                .toList();
        KitEntity kit = KitEntity.create(request.name(), request.color(), request.price(), components, clock.instant());
        return KitResponse.from(kits.saveAndFlush(kit));
    }

    @PreAuthorize("hasAuthority('KIT_READ')")
    @Transactional(readOnly = true)
    public Page<KitResponse> list(String query, Pageable pageable) {
        Page<KitEntity> result = query == null || query.isBlank()
                ? kits.findAll(pageable)
                : kits.findByNameContainingIgnoreCase(query.strip(), pageable);
        return result.map(KitResponse::from);
    }

    @PreAuthorize("hasAuthority('KIT_READ')")
    @Transactional(readOnly = true)
    public KitResponse get(UUID id) { return KitResponse.from(requireKit(id)); }

    @PreAuthorize("hasAuthority('KIT_UPDATE')")
    @Transactional
    public KitResponse replace(UUID id, KitRequest request) {
        KitEntity kit = requireKit(id);
        rejectDuplicateName(request.name(), id);
        List<KitComponentEntity> components = request.components().stream()
                .map(component -> KitComponentEntity.create(component.name(), component.quantity(), component.notes()))
                .toList();
        kit.replace(request.name(), request.color(), request.price(), components, clock.instant());
        return KitResponse.from(kits.saveAndFlush(kit));
    }

    @PreAuthorize("hasAuthority('KIT_DELETE')")
    @Transactional
    public void delete(UUID id) {
        KitEntity kit = requireKit(id);
        kits.delete(kit);
        kits.flush();
    }

    private KitEntity requireKit(UUID id) {
        return kits.findById(id).orElseThrow(() -> new ResourceNotFoundException("Kit"));
    }

    private void rejectDuplicateName(String name, UUID currentId) {
        kits.findByNameIgnoreCase(name.strip()).filter(kit -> !kit.getId().equals(currentId))
                .ifPresent(kit -> { throw new ConflictException("DUPLICATE_KIT", "Já existe um kit com este nome"); });
    }
}