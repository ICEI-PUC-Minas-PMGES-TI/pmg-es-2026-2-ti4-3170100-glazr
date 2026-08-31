package com.glazr.invitation.infrastructure;

import com.glazr.invitation.domain.DeliveryStatus;
import com.glazr.invitation.domain.InvitationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeeInvitationEntityTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-31T10:00:00Z");

    @Test
    void derivesFunctionalStatusFromDatesIndependentlyOfDelivery() {
        EmployeeInvitationEntity invitation = invitation();
        assertThat(invitation.statusAt(CREATED_AT)).isEqualTo(InvitationStatus.PENDING);

        invitation.markSent(CREATED_AT.plusSeconds(10));
        assertThat(invitation.getDeliveryStatus()).isEqualTo(DeliveryStatus.SENT);
        assertThat(invitation.statusAt(CREATED_AT.plusSeconds(60))).isEqualTo(InvitationStatus.PENDING);

        invitation.consume(CREATED_AT.plusSeconds(120));
        assertThat(invitation.statusAt(CREATED_AT.plusSeconds(121))).isEqualTo(InvitationStatus.CONSUMED);
    }

    @Test
    void expiredOrRevokedInvitationCannotBeConsumed() {
        EmployeeInvitationEntity expired = invitation();
        assertThatThrownBy(() -> expired.consume(CREATED_AT.plusSeconds(49 * 60 * 60)))
                .isInstanceOf(IllegalStateException.class);

        EmployeeInvitationEntity revoked = invitation();
        revoked.revoke(CREATED_AT.plusSeconds(10));
        assertThat(revoked.statusAt(CREATED_AT.plusSeconds(11))).isEqualTo(InvitationStatus.REVOKED);
        assertThatThrownBy(() -> revoked.consume(CREATED_AT.plusSeconds(12)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deliveryFailureUsesOnlyASafeCode() {
        EmployeeInvitationEntity invitation = invitation();

        invitation.markDeliveryFailed("SMTP_UNAVAILABLE");

        assertThat(invitation.getDeliveryStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(invitation.getDeliveryErrorCode()).isEqualTo("SMTP_UNAVAILABLE");
    }

    private EmployeeInvitationEntity invitation() {
        return EmployeeInvitationEntity.create(
                UUID.fromString("4a0b6076-9b3c-436f-89bd-fc729a6a27a1"),
                UUID.fromString("9833922e-7288-44cb-96a3-155fe21e3fd6"),
                "a".repeat(64), CREATED_AT.plusSeconds(48 * 60 * 60), CREATED_AT);
    }
}
