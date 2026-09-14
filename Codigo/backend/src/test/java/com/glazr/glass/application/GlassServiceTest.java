package com.glazr.glass.application;

import com.glazr.glass.api.CreateGlassRequest;
import com.glazr.glass.api.GlassResponse;
import com.glazr.glass.api.UpdateGlassRequest;
import com.glazr.glass.infrastructure.GlassEntity;
import com.glazr.glass.infrastructure.GlassRepository;
import com.glazr.shared.exception.ConflictException;
import com.glazr.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlassServiceTest {

    private final GlassRepository glasses = mock(GlassRepository.class);
    private final GlassService service = new GlassService(glasses, Clock.systemUTC());

    @Test
    void rejectsDuplicateCompositeAttributes() {
        GlassEntity existing = GlassEntity.create(
                "Vidro Temperado",
                new BigDecimal("8.00"),
                "Lapidado",
                "Incolor",
                new BigDecimal("150.00"),
                Clock.systemUTC().instant()
        );

        when(glasses.findByNameIgnoreCaseAndThicknessMmAndFinishIgnoreCaseAndColorIgnoreCase(
                "Vidro Temperado", new BigDecimal("8.00"), "Lapidado", "Incolor"
        )).thenReturn(Optional.of(existing));

        CreateGlassRequest request = new CreateGlassRequest(
                "Vidro Temperado",
                new BigDecimal("8.00"),
                "Lapidado",
                "Incolor",
                new BigDecimal("160.00")
        );

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .extracting("code")
                .isEqualTo("DUPLICATE_GLASS");
    }

    @Test
    void createsAndPersistsNewGlass() {
        when(glasses.findByNameIgnoreCaseAndThicknessMmAndFinishIgnoreCaseAndColorIgnoreCase(
                any(), any(), any(), any()
        )).thenReturn(Optional.empty());

        when(glasses.saveAndFlush(any(GlassEntity.class))).thenAnswer(i -> i.getArgument(0));

        CreateGlassRequest request = new CreateGlassRequest(
                "Vidro Temperado",
                new BigDecimal("8.00"),
                "Lapidado",
                "Incolor",
                new BigDecimal("150.00")
        );

        GlassResponse response = service.create(request);
        assertThat(response.name()).isEqualTo("Vidro Temperado");
        assertThat(response.thicknessMm()).isEqualByComparingTo("8.00");
        assertThat(response.active()).isTrue();

        ArgumentCaptor<GlassEntity> captor = ArgumentCaptor.forClass(GlassEntity.class);
        verify(glasses).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Vidro Temperado");
    }

    @Test
    void updateFailsWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(glasses.findById(unknownId)).thenReturn(Optional.empty());

        UpdateGlassRequest request = new UpdateGlassRequest(
                "Vidro",
                new BigDecimal("6.00"),
                "Lapidado",
                "Incolor",
                new BigDecimal("100.00")
        );

        assertThatThrownBy(() -> service.update(unknownId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
