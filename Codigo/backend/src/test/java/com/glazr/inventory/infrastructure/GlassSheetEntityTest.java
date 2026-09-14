package com.glazr.inventory.infrastructure;

import com.glazr.inventory.domain.SheetStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlassSheetEntityTest {

    @Test
    void calculatesAreaFromMillimetersAndTracksDepletion() {
        GlassSheetEntity sheet = GlassSheetEntity.create(
                UUID.randomUUID(), new BigDecimal("2000.00"), new BigDecimal("3000.00"), Instant.EPOCH);

        assertThat(sheet.getOriginalAreaM2()).isEqualByComparingTo("6.000000");
        assertThat(sheet.getStatus()).isEqualTo(SheetStatus.AVAILABLE);

        sheet.updateRemainingArea(BigDecimal.ZERO, Instant.EPOCH.plusSeconds(1));

        assertThat(sheet.getRemainingAreaM2()).isEqualByComparingTo("0.000000");
        assertThat(sheet.getStatus()).isEqualTo(SheetStatus.DEPLETED);
    }

    @Test
    void rejectsRemainingAreaAboveTheOriginalSheetArea() {
        GlassSheetEntity sheet = GlassSheetEntity.create(
                UUID.randomUUID(), new BigDecimal("1000.00"), new BigDecimal("1000.00"), Instant.EPOCH);

        assertThatThrownBy(() -> sheet.updateRemainingArea(new BigDecimal("1.000001"), Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Saldo disponível não pode superar a área original da chapa");
    }
}
