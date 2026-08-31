package com.glazr.password.application;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetDeliveryListenerTest {
    @Test
    void deliveryRunsOutsideThePublicRequestThread() throws Exception {
        Async async = PasswordResetDeliveryListener.class
                .getDeclaredMethod("deliver", PasswordResetDeliveryRequested.class)
                .getAnnotation(Async.class);

        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("mailTaskExecutor");
    }
}
