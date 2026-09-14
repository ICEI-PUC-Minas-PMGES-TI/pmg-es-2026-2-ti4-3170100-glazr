package com.glazr.kit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glazr.kit.application.KitService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class KitControllerTest {

    private static final UUID KIT_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Test
    void delegatesCreationToTheServiceAndReturnsItsResponse() {
        KitService service = mock(KitService.class);
        KitRequest request = new KitRequest("Kit Box", "Incolor", new BigDecimal("1250.00"),
                java.util.List.of(new KitComponentRequest("Vidro 8mm", 2, null)));
        KitResponse response = new KitResponse(KIT_ID, "Kit Box", "Incolor", new BigDecimal("1250.00"),
                java.util.List.of(new KitResponse.Component("Vidro 8mm", 2, null)),
                java.time.Instant.EPOCH, java.time.Instant.EPOCH);
        when(service.create(request)).thenReturn(response);

        KitResponse result = new KitController(service).create(request);

        assertThat(result).isSameAs(response);
        verify(service).create(request);
    }

    @Test
    void delegatesListingToTheService() {
        KitService service = mock(KitService.class);
        Pageable pageable = Pageable.unpaged();
        KitResponse response = new KitResponse(KIT_ID, "Kit Box", "Incolor", BigDecimal.ONE,
                java.util.List.of(), java.time.Instant.EPOCH, java.time.Instant.EPOCH);
        when(service.list(null, pageable)).thenReturn(new PageImpl<>(java.util.List.of(response)));

        new KitController(service).list(null, pageable);

        verify(service).list(null, pageable);
    }
}