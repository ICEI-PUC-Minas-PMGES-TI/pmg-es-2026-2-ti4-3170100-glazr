package com.glazr;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GlazrApplicationTests {

    @Test
    void exposesSpringBootApplicationEntryPoint() {
        assertThat(GlazrApplication.class).isNotNull();
    }
}
