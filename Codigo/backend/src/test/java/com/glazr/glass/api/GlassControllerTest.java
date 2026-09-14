package com.glazr.glass.api;

import com.glazr.glass.application.GlassService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlassControllerTest {

    private final GlassService service = mock(GlassService.class);
    private final GlassController controller = new GlassController(service);

    @Test
    void delegatesCreateToService() {
        CreateGlassRequest request = new CreateGlassRequest("Vidro Laminado", new BigDecimal("10.00"), "Lapidado", "Incolor", new BigDecimal("220.00"));
        GlassResponse response = new GlassResponse(UUID.randomUUID(), "Vidro Laminado", new BigDecimal("10.00"), "Lapidado", "Incolor", new BigDecimal("220.00"), true, Instant.EPOCH, Instant.EPOCH);
        when(service.create(request)).thenReturn(response);

        GlassResponse result = controller.create(request);
        assertThat(result).isSameAs(response);
        verify(service).create(request);
    }

    @Test
    void delegatesListToService() {
        Pageable pageable = Pageable.unpaged();
        when(service.list("Laminado", new BigDecimal("10.00"), "Lapidado", true, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        controller.list("Laminado", new BigDecimal("10.00"), "Lapidado", true, pageable);
        verify(service).list("Laminado", new BigDecimal("10.00"), "Lapidado", true, pageable);
    }
}
