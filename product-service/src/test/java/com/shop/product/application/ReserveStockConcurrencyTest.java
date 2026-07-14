package com.shop.product.application;

import com.shop.product.domain.model.InsufficientStockException;
import com.shop.product.domain.model.Money;
import com.shop.product.domain.model.Product;
import com.shop.product.domain.port.in.ReserveStockCommand;
import com.shop.product.domain.port.in.ReserveStockUseCase;
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

/**
 * The test that justifies the whole reserve refactor.
 *
 * 20 threads race for 5 items. The old design (order-service reads stock over HTTP,
 * checks it in Java, then saves) cannot pass this: every thread reads stock=5, every
 * thread concludes "enough", and 20 orders get created for 5 items.
 *
 * Here the check and the write happen in one transaction, on the row that owns the
 * stock, behind SELECT ... FOR UPDATE. Exactly 5 threads win. Not 4, not 6.
 *
 * The 15 losers get InsufficientStockException — an honest business answer ("it is
 * gone") — and not an optimistic-locking error, which would just mean "try again".
 */
@SpringBootTest
@ActiveProfiles("test")
class ReserveStockConcurrencyTest {

    private static final int STOCK = 5;
    private static final int THREADS = 20;

    @Autowired
    private ReserveStockUseCase reserveStockUseCase;

    @Autowired
    private SaveProductPort saveProductPort;

    @Autowired
    private LoadProductPort loadProductPort;

    @Test
    @DisplayName("20 threads reserve 1 each from stock of 5 -> exactly 5 succeed, stock lands on 0")
    void neverOversells() throws Exception {
        Product product = saveProductPort.save(
                Product.create("Contended item", Money.of(new BigDecimal("1000")), STOCK));
        Long productId = product.id();

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        AtomicInteger lostTheRace = new AtomicInteger();

        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    startGun.await();
                    reserveStockUseCase.reserve(new ReserveStockCommand(productId, 1));
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException soldOutForReal) {
                    soldOut.incrementAndGet();
                } catch (OptimisticLockingFailureException lost) {
                    // Someone else updated the row first. Not oversold — just a retry.
                    lostTheRace.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGun.countDown();
        assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        int remaining = loadProductPort.findById(productId).orElseThrow().stock();

        assertThat(succeeded.get()).isEqualTo(STOCK);
        assertThat(soldOut.get()).isEqualTo(THREADS - STOCK);
        assertThat(remaining).isZero();

        // The write lock serialises them, so nobody should ever lose an optimistic race.
        assertThat(lostTheRace.get()).isZero();
    }
}
