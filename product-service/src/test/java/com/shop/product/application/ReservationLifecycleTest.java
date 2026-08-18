package com.shop.product.application;

import com.shop.product.domain.model.IdempotencyConflictException;
import com.shop.product.domain.model.InventoryReconciliation;
import com.shop.product.domain.model.Money;
import com.shop.product.domain.model.Product;
import com.shop.product.domain.model.ReservationDetails;
import com.shop.product.domain.model.ReservationStatus;
import com.shop.product.domain.port.in.HoldReservationCommand;
import com.shop.product.domain.port.in.ReconcileInventoryUseCase;
import com.shop.product.domain.port.in.ReservationUseCase;
import com.shop.product.domain.port.out.SaveProductPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ReservationLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

    @Autowired ReservationUseCase reservations;
    @Autowired SaveProductPort products;
    @Autowired ReconcileInventoryUseCase reconciliation;
    @Autowired MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(NOW);
    }

    @Test
    void sameKeyAndPayloadReturnsSameReservationWithoutDoubleHold() {
        Product product = stocked(10);
        HoldReservationCommand command = new HoldReservationCommand(
                product.id(), 3, "order-service", "same-key");

        ReservationDetails first = reservations.hold(command);
        ReservationDetails replay = reservations.hold(command);

        assertThat(replay.reservation().id()).isEqualTo(first.reservation().id());
        assertThat(replay.product().onHand()).isEqualTo(10);
        assertThat(replay.product().reserved()).isEqualTo(3);
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejected() {
        Product product = stocked(10);
        reservations.hold(new HoldReservationCommand(
                product.id(), 2, "order-service", "conflict-key"));

        assertThatThrownBy(() -> reservations.hold(new HoldReservationCommand(
                product.id(), 3, "order-service", "conflict-key")))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void releaseIsIdempotentAndDoesNotCreatePhysicalStock() {
        Product product = stocked(10);
        ReservationDetails held = reservations.hold(new HoldReservationCommand(
                product.id(), 4, "order-service", "release-key"));

        ReservationDetails released = reservations.release(held.reservation().id());
        ReservationDetails replay = reservations.release(held.reservation().id());

        assertThat(released.reservation().status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(replay.product().onHand()).isEqualTo(10);
        assertThat(replay.product().reserved()).isZero();
        assertThat(reconciliation.reconcile(product.id()).consistent()).isTrue();
    }

    @Test
    void confirmIsIdempotentAndIssuesPhysicalStockExactlyOnce() {
        Product product = stocked(10);
        ReservationDetails held = reservations.hold(new HoldReservationCommand(
                product.id(), 4, "order-service", "confirm-key"));

        ReservationDetails confirmed = reservations.confirm(held.reservation().id());
        ReservationDetails replay = reservations.confirm(held.reservation().id());

        assertThat(confirmed.reservation().status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(replay.product().onHand()).isEqualTo(6);
        assertThat(replay.product().reserved()).isZero();
        InventoryReconciliation result = reconciliation.reconcile(product.id());
        assertThat(result.consistent()).isTrue();
        assertThat(result.expectedOnHand()).isEqualTo(6);
    }

    @Test
    void forgottenHoldExpiresUsingInjectedClockAndReleasesOnce() {
        Product product = stocked(10);
        ReservationDetails held = reservations.hold(new HoldReservationCommand(
                product.id(), 4, "order-service", "expiry-key"));
        clock.advance(Duration.ofMinutes(16));

        reservations.expireIfDue(held.reservation().id());
        reservations.expireIfDue(held.reservation().id());

        ReservationDetails expired = reservations.findById(held.reservation().id())
                .orElseThrow();
        assertThat(expired.reservation().status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(expired.product().onHand()).isEqualTo(10);
        assertThat(expired.product().reserved()).isZero();
        assertThat(reconciliation.reconcile(product.id()).consistent()).isTrue();
    }

    @Test
    void concurrentExpiryWorkersReleaseTheHoldOnlyOnce() throws Exception {
        Product product = stocked(10);
        ReservationDetails held = reservations.hold(new HoldReservationCommand(
                product.id(), 4, "order-service", "concurrent-expiry-key"));
        clock.advance(Duration.ofMinutes(16));

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(2);
        AtomicInteger failures = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    reservations.expireIfDue(held.reservation().id());
                } catch (RuntimeException failure) {
                    failures.incrementAndGet();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        start.countDown();
        assertThat(finished.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        ReservationDetails result = reservations.findById(held.reservation().id())
                .orElseThrow();
        assertThat(failures.get()).isZero();
        assertThat(result.reservation().status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(result.product().onHand()).isEqualTo(10);
        assertThat(result.product().reserved()).isZero();
        assertThat(reconciliation.reconcile(product.id()).consistent()).isTrue();
    }

    private Product stocked(int quantity) {
        Product created = products.save(Product.create(
                "Lifecycle " + System.nanoTime(), Money.of(new BigDecimal("1000"))));
        return products.apply(created.receive(quantity));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(NOW);
        }
    }

    static final class MutableClock extends Clock {
        private volatile Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void set(Instant value) {
            instant = value;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
