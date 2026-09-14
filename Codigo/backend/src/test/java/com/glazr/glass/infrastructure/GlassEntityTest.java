package com.glazr.glass.infrastructure;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlassEntityTest {

    private static final Instant NOW = Instant.EPOCH;

    @Test
    void preservesFormattingWhileTrimmingWhitespace() {
        GlassEntity glass = GlassEntity.create(
                "  Vidro Temperado  ",
                new BigDecimal("8.00"),
                "  Lapidado  ",
                "  Incolor  ",
                new BigDecimal("180.50"),
                NOW
        );

        assertThat(glass.getName()).isEqualTo("Vidro Temperado");
        assertThat(glass.getThicknessMm()).isEqualByComparingTo("8.00");
        assertThat(glass.getFinish()).isEqualTo("Lapidado");
        assertThat(glass.getColor()).isEqualTo("Incolor");
        assertThat(glass.getPricePerSquareMeter()).isEqualByComparingTo("180.50");
        assertThat(glass.isActive()).isTrue();
        assertThat(glass.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsInvalidFields() {
        assertThatThrownBy(() -> GlassEntity.create("  ", new BigDecimal("8.00"), "Lapidado", "Incolor", new BigDecimal("100.00"), NOW))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> GlassEntity.create("Vidro", new BigDecimal("0.00"), "Lapidado", "Incolor", new BigDecimal("100.00"), NOW))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> GlassEntity.create("Vidro", new BigDecimal("8.00"), "  ", "Incolor", new BigDecimal("100.00"), NOW))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> GlassEntity.create("Vidro", new BigDecimal("8.00"), "Lapidado", "  ", new BigDecimal("100.00"), NOW))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> GlassEntity.create("Vidro", new BigDecimal("8.00"), "Lapidado", "Incolor", new BigDecimal("-1.00"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateAndStatusToggleModifyFieldsCorrectly() {
        GlassEntity glass = GlassEntity.create("Vidro Comum", new BigDecimal("4.00"), "Sem acabamento", "Verde", new BigDecimal("80.00"), NOW);

        glass.update("Vidro Laminado", new BigDecimal("6.00"), "Bisotê", "Fumê", new BigDecimal("120.00"), NOW.plusSeconds(30));
        assertThat(glass.getName()).isEqualTo("Vidro Laminado");
        assertThat(glass.getThicknessMm()).isEqualByComparingTo("6.00");
        assertThat(glass.getFinish()).isEqualTo("Bisotê");
        assertThat(glass.getColor()).isEqualTo("Fumê");
        assertThat(glass.getPricePerSquareMeter()).isEqualByComparingTo("120.00");
        assertThat(glass.getUpdatedAt()).isEqualTo(NOW.plusSeconds(30));

        glass.updateStatus(false, NOW.plusSeconds(60));
        assertThat(glass.isActive()).isFalse();
        assertThat(glass.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
    }
}
