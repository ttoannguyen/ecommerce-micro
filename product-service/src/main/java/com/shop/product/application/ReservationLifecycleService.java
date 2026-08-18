package com.shop.product.application;

import com.shop.product.domain.model.IdempotencyConflictException;
import com.shop.product.domain.model.Product;
import com.shop.product.domain.model.ProductNotFoundException;
import com.shop.product.domain.model.Reservation;
import com.shop.product.domain.model.ReservationDetails;
import com.shop.product.domain.model.ReservationNotFoundException;
import com.shop.product.domain.model.ReservationStatus;
import com.shop.product.domain.model.StockChange;
import com.shop.product.domain.port.in.HoldReservationCommand;
import com.shop.product.domain.port.in.ReservationUseCase;
import com.shop.product.domain.port.out.LoadProductPort;
import com.shop.product.domain.port.out.ReservationStorePort;
import com.shop.product.domain.port.out.SaveProductPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationLifecycleService implements ReservationUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;
    private final ReservationStorePort reservationStore;
    private final Clock clock;
    private final Duration ttl;

    public ReservationLifecycleService(
            LoadProductPort loadProductPort,
            SaveProductPort saveProductPort,
            ReservationStorePort reservationStore,
            Clock clock,
            @Value("${inventory.reservation.ttl:PT15M}") Duration ttl) {
        this.loadProductPort = loadProductPort;
        this.saveProductPort = saveProductPort;
        this.reservationStore = reservationStore;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    @Transactional
    public ReservationDetails hold(HoldReservationCommand command) {
        validate(command);
        Optional<Reservation> replay = reservationStore.findByCallerAndKey(
                command.caller().trim(), command.idempotencyKey().trim());
        if (replay.isPresent()) {
            return replay(command, replay.orElseThrow());
        }

        Product product = loadForUpdate(command.productId());

        // Requests with the same key and SKU serialize on the balance row. Recheck
        // after the lock so the second request observes the first committed result.
        replay = reservationStore.findByCallerAndKey(
                command.caller().trim(), command.idempotencyKey().trim());
        if (replay.isPresent()) {
            return replay(command, replay.orElseThrow());
        }

        Product held = product.hold(command.quantity());
        Instant now = clock.instant();
        Reservation reservation = Reservation.hold(
                UUID.randomUUID(), command.caller(), command.idempotencyKey(),
                command.productId(), command.quantity(), now, ttl);

        saveProductPort.updateBalance(held);
        Reservation saved = reservationStore.save(reservation);
        return new ReservationDetails(saved, held);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationDetails> findById(UUID id) {
        return reservationStore.findById(id).map(this::details);
    }

    @Override
    @Transactional
    public ReservationDetails confirm(UUID id) {
        Reservation current = loadReservationForUpdate(id);
        if (current.status() == ReservationStatus.CONFIRMED) {
            return details(current);
        }

        Reservation confirmed = current.confirm(clock.instant());
        Product product = loadForUpdate(current.productId());
        StockChange fulfilled = product.fulfill(current.quantity());
        Product savedProduct = saveProductPort.apply(fulfilled);
        Reservation saved = reservationStore.save(confirmed);
        return new ReservationDetails(saved, savedProduct);
    }

    @Override
    @Transactional
    public ReservationDetails release(UUID id) {
        Reservation current = loadReservationForUpdate(id);
        if (current.status() == ReservationStatus.RELEASED
                || current.status() == ReservationStatus.EXPIRED) {
            return details(current);
        }

        Reservation released = current.release(clock.instant());
        Product product = loadForUpdate(current.productId());
        Product available = product.releaseHold(current.quantity());
        Product savedProduct = saveProductPort.updateBalance(available);
        Reservation saved = reservationStore.save(released);
        return new ReservationDetails(saved, savedProduct);
    }

    @Override
    @Transactional
    public void expireIfDue(UUID id) {
        Reservation current = loadReservationForUpdate(id);
        Instant now = clock.instant();
        if (!current.isDue(now)) {
            return;
        }

        Reservation expired = current.expire(now);
        Product product = loadForUpdate(current.productId());
        saveProductPort.updateBalance(product.releaseHold(current.quantity()));
        reservationStore.save(expired);
    }

    private ReservationDetails replay(HoldReservationCommand command,
                                      Reservation existing) {
        if (!existing.matches(command.caller(), command.productId(), command.quantity())) {
            throw new IdempotencyConflictException(
                    "Idempotency-Key đã được dùng với payload khác");
        }
        return details(existing);
    }

    private ReservationDetails details(Reservation reservation) {
        Product product = loadProductPort.findById(reservation.productId())
                .orElseThrow(() -> new ProductNotFoundException(reservation.productId()));
        return new ReservationDetails(reservation, product);
    }

    private Product loadForUpdate(Long productId) {
        return loadProductPort.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private Reservation loadReservationForUpdate(UUID id) {
        return reservationStore.findByIdForUpdate(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }

    private static void validate(HoldReservationCommand command) {
        if (command.productId() == null) {
            throw new IllegalArgumentException("productId là bắt buộc");
        }
        if (command.quantity() <= 0) {
            throw new IllegalArgumentException("quantity phải > 0");
        }
        if (command.caller() == null || command.caller().isBlank()
                || command.caller().length() > 64) {
            throw new IllegalArgumentException("caller phải có 1..64 ký tự");
        }
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()
                || command.idempotencyKey().length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key phải có 1..128 ký tự");
        }
    }
}
