package com.glazr.kit.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class KitComponentEntityTest {

    @Test
    void trimsNameAndNotesAndDropsBlankNotes() {
        KitComponentEntity component = KitComponentEntity.create("  Vidro 8mm  ", 2, "   ");

        assertThat(component.getName()).isEqualTo("Vidro 8mm");
        assertThat(component.getQuantity()).isEqualTo(2);
        assertThat(component.getNotes()).isNull();
    }

    @Test
    void rejectsBlankNameAndNonPositiveQuantity() {
        assertThatThrownBy(() -> KitComponentEntity.create("   ", 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nome");
        assertThatThrownBy(() -> KitComponentEntity.create("Vidro", 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantidade");
        assertThatThrownBy(() -> KitComponentEntity.create("Vidro", -1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}