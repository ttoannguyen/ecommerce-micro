package com.shop.product.application;

import com.shop.product.domain.model.InsufficientStockException;
import com.shop.product.domain.model.InventoryReconciliation;
import com.shop.product.domain.model.Money;
import com.shop.product.domain.model.Product;
import com.shop.product.domain.port.in.HoldReservationCommand;
import com.shop.product.domain.port.in.ReconcileInventoryUseCase;
import com.shop.product.domain.port.in.ReservationUseCase;
import com.shop.product.domain.port.out.LoadProductPort;
import com.shop.product.domain.port.out.SaveProductPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReservationConcurrencyTest {

    private static final int STOCK = 5;
    private static final int THREADS = 20;

    @Autowired ReservationUseCase reservations;
    @Autowired SaveProductPort saveProductPort;
    @Autowired LoadProductPort loadProductPort;
    @Autowired ReconcileInventoryUseCase reconciliation;

    @Test
    @DisplayName("20 hold tranh 5 available -> đúng 5 HELD, on-hand không đổi")
    void neverOverReserves() throws Exception {
        Product created = saveProductPort.save(
                Product.create("Contended item", Money.of(new BigDecimal("1000"))));
        Product product = saveProductPort.apply(created.receive(STOCK));
        Long productId = product.id();

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        AtomicInteger lostTheRace = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            String key = "concurrent-" + productId + "-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    reservations.hold(new HoldReservationCommand(
                            productId, 1, "test", key));
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException soldOutForReal) {
                    soldOut.incrementAndGet();
                } catch (OptimisticLockingFailureException lost) {
                    lostTheRace.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        start.countDown();
        assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        Product finalBalance = loadProductPort.findById(productId).orElseThrow();
        assertThat(succeeded.get()).isEqualTo(STOCK);
        assertThat(soldOut.get()).isEqualTo(THREADS - STOCK);
        assertThat(lostTheRace.get()).isZero();
        assertThat(finalBalance.onHand()).isEqualTo(STOCK);
        assertThat(finalBalance.reserved()).isEqualTo(STOCK);
        assertThat(finalBalance.available()).isZero();

        InventoryReconciliation result = reconciliation.reconcile(productId);
        assertThat(result.consistent()).isTrue();
        assertThat(result.expectedOnHand()).isEqualTo(STOCK);
        assertThat(result.expectedReserved()).isEqualTo(STOCK);
    }
}
