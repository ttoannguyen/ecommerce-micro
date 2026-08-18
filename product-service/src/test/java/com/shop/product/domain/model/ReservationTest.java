package com.shop.product.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationTest {

    private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

    private static Reservation held() {
        return Reservation.hold(UUID.randomUUID(), "order-service", "order-1",
                1L, 2, NOW, Duration.ofMinutes(15));
    }

    @Test
    void newReservationIsHeldAndExpires() {
        Reservation reservation = held();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.HELD);
        assertThat(reservation.expiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(reservation.matches("order-service", 1L, 2)).isTrue();
    }

    @Test
    void terminalCommandsAreIdempotent() {
        Reservation confirmed = held().confirm(NOW.plusSeconds(1));
        assertThat(confirmed.confirm(NOW.plusSeconds(2))).isSameAs(confirmed);

        Reservation released = held().release(NOW.plusSeconds(1));
        assertThat(released.release(NOW.plusSeconds(2))).isSameAs(released);

        Reservation expired = held().expire(NOW.plusSeconds(900));
        assertThat(expired.expire(NOW.plusSeconds(901))).isSameAs(expired);
    }

    @Test
    void terminalReservationNeverReturnsToAnotherTerminalState() {
        Reservation released = held().release(NOW.plusSeconds(1));

        assertThatThrownBy(() -> released.confirm(NOW.plusSeconds(2)))
                .isInstanceOf(InvalidReservationTransitionException.class);
    }

    @Test
    void cannotConfirmAfterExpiryTime() {
        assertThatThrownBy(() -> held().confirm(NOW.plusSeconds(900)))
                .isInstanceOf(InvalidReservationTransitionException.class);
    }
}
