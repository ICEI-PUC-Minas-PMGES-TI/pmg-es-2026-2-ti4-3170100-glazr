package com.glazr.kit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glazr.kit.api.KitComponentRequest;
import com.glazr.kit.api.KitRequest;
import com.glazr.kit.api.KitResponse;
import com.glazr.kit.infrastructure.KitComponentEntity;
import com.glazr.kit.infrastructure.KitEntity;
import com.glazr.kit.infrastructure.KitRepository;
import com.glazr.shared.exception.ConflictException;
import com.glazr.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KitServiceTest {

    private static KitRequest request(String name) {
        return new KitRequest(name, "Incolor", new BigDecimal("1250.00"),
                List.of(new KitComponentRequest(name, 2, null)));
    }

    @Test
    void rejectsDuplicateKitNameWithStableConflictCode() {
        KitRepository kits = mock(KitRepository.class);
        when(kits.findByNameIgnoreCase("Kit Box")).thenReturn(Optional.of(KitEntity.create(
                "Kit Box", "Incolor", BigDecimal.ONE,
                List.of(KitComponentEntity.create("Vidro", 1, null)), Clock.systemUTC().instant())));
        KitService service = new KitService(kits, Clock.systemUTC());

        assertThatThrownBy(() -> service.create(request("Kit Box")))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("DUPLICATE_KIT");
    }

    @Test
    void createBuildsKitWithComponentsAndPersistsIt() {
        KitRepository kits = mock(KitRepository.class);
        when(kits.findByNameIgnoreCase("Kit Box")).thenReturn(Optional.empty());
        when(kits.saveAndFlush(org.mockito.ArgumentMatchers.any(KitEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        KitService service = new KitService(kits, Clock.systemUTC());

        KitResponse response = service.create(request("Kit Box"));

        assertThat(response.name()).isEqualTo("Kit Box");
        assertThat(response.components()).extracting(KitResponse.Component::name).containsExactly("Kit Box");
        ArgumentCaptor<KitEntity> captor = ArgumentCaptor.forClass(KitEntity.class);
        verify(kits).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getComponents()).hasSize(1);
    }

    @Test
    void replaceFailsWhenKitDoesNotExist() {
        KitRepository kits = mock(KitRepository.class);
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000009");
        when(kits.findById(unknownId)).thenReturn(Optional.empty());
        KitService service = new KitService(kits, Clock.systemUTC());

        assertThatThrownBy(() -> service.replace(unknownId, request("Kit")))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting("code")
                .isEqualTo("RESOURCE_NOT_FOUND");
    }
}