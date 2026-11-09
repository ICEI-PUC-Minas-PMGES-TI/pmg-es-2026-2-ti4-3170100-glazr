package com.glazr.kit.infrastructure;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KitEntityTest {

    private static final Instant NOW = Instant.EPOCH;

    private static List<KitComponentEntity> components(String name) {
        return List.of(KitComponentEntity.create(name, 2, null));
    }

    @Test
    void preservesTheUsersCapitalizationWhileTrimmingWhitespace() {
        KitEntity kit = KitEntity.create(
                "  Kit Box Incolor  ", "  Incolor  ", new BigDecimal("1250.00"), components("Vidro 8mm"), NOW);

        assertThat(kit.getName()).isEqualTo("Kit Box Incolor");
        assertThat(kit.getColor()).isEqualTo("Incolor");
        assertThat(kit.getPrice()).isEqualByComparingTo("1250.00");
    }

    @Test
    void rejectsBlankNameColorAndNegativePrice() {
        assertThatThrownBy(() -> KitEntity.create("   ", "Incolor", BigDecimal.ONE, components("Vidro"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KitEntity.create("Kit", "   ", BigDecimal.ONE, components("Vidro"), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KitEntity.create("Kit", "Incolor", new BigDecimal("-0.01"), components("Vidro"), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Preço");
    }

    @Test
    void requiresAtLeastOneComponent() {
        assertThatThrownBy(() -> KitEntity.create("Kit", "Incolor", BigDecimal.ONE, List.of(), NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("componente");
        assertThatThrownBy(() -> KitEntity.create("Kit", "Incolor", BigDecimal.ONE, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replaceSwapsNameColorPriceAndComponents() {
        KitEntity kit = KitEntity.create("Kit antigo", "Bronze", BigDecimal.ONE, components("Vidro 6mm"), NOW);

        kit.replace("Kit novo", "Fumê", new BigDecimal("99.90"), components("Vidro 8mm"), NOW.plusSeconds(60));

        assertThat(kit.getName()).isEqualTo("Kit novo");
        assertThat(kit.getColor()).isEqualTo("Fumê");
        assertThat(kit.getPrice()).isEqualByComparingTo("99.90");
        assertThat(kit.getComponents()).extracting(KitComponentEntity::getName).containsExactly("Vidro 8mm");
        assertThat(kit.getUpdatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void exposesComponentsAsDefensiveCopy() {
        KitEntity kit = KitEntity.create("Kit", "Incolor", BigDecimal.ONE, components("Vidro"), NOW);

        assertThatThrownBy(() -> kit.getComponents().add(KitComponentEntity.create("Extra", 1, null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}