package com.shop.product.application;

import com.shop.product.domain.model.IdempotencyConflictException;
import com.shop.product.domain.model.Product;
import com.shop.product.domain.model.ProductNotFoundException;
import com.shop.product.domain.model.Reservation;
import com.shop.product.domain.model.ReservationBatchDetails;
import com.shop.product.domain.model.ReservationDetails;
import com.shop.product.domain.model.ReservationNotFoundException;
import com.shop.product.domain.model.ReservationStatus;
import com.shop.product.domain.model.StockChange;
import com.shop.product.domain.port.in.HoldReservationCommand;
import com.shop.product.domain.port.in.HoldReservationBatchCommand;
import com.shop.product.domain.port.in.HoldReservationLine;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
        return holdWithinTransaction(command.productId(), command.quantity(),
                command.caller(), command.idempotencyKey());
    }

    @Override
    @Transactional
    public ReservationBatchDetails holdBatch(HoldReservationBatchCommand command) {
        validateBatch(command);
        List<HoldReservationLine> ordered = command.lines().stream()
                .sorted(Comparator.comparing(HoldReservationLine::productId))
                .toList();
        List<ReservationDetails> reservations = new ArrayList<>();
        for (HoldReservationLine line : ordered) {
            reservations.add(holdWithinTransaction(line.productId(), line.quantity(),
                    command.caller(), command.idempotencyKey()));
        }
        return new ReservationBatchDetails(reservations);
    }

    private ReservationDetails holdWithinTransaction(Long productId, int quantity,
                                                      String caller, String idempotencyKey) {
        Optional<Reservation> replay = reservationStore.findByCallerKeyAndProduct(
                caller.trim(), idempotencyKey.trim(), productId);
        if (replay.isPresent()) {
            return replay(replay.orElseThrow(), caller, productId, quantity);
        }

        Product product = loadForUpdate(productId);
        replay = reservationStore.findByCallerKeyAndProduct(
                caller.trim(), idempotencyKey.trim(), productId);
        if (replay.isPresent()) {
            return replay(replay.orElseThrow(), caller, productId, quantity);
        }

        Product held = product.hold(quantity);
        Instant now = clock.instant();
        Reservation reservation = Reservation.hold(
                UUID.randomUUID(), caller, idempotencyKey, productId, quantity, now, ttl);
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

    private ReservationDetails replay(Reservation existing, String caller, Long productId,
                                      int quantity) {
        if (!existing.matches(caller, productId, quantity)) {
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

    private static void validateBatch(HoldReservationBatchCommand command) {
        if (command == null || command.lines() == null || command.lines().isEmpty()) {
            throw new IllegalArgumentException("lines phải có ít nhất một sản phẩm");
        }
        if (command.lines().stream().anyMatch(line -> line == null
                || line.productId() == null || line.quantity() <= 0)) {
            throw new IllegalArgumentException("mỗi line phải có productId và quantity > 0");
        }
        if (new HashSet<>(command.lines().stream().map(HoldReservationLine::productId).toList())
                .size() != command.lines().size()) {
            throw new IllegalArgumentException("một order không được lặp productId");
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
