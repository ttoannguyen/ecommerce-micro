package com.shop.product.adapter.in.bootstrap;

import com.shop.product.domain.model.Money;
import com.shop.product.domain.model.Product;
import com.shop.product.domain.port.out.LoadProductPort;
import com.shop.product.domain.port.out.SaveProductPort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Seeds a few products when the DB is empty. Goes through the port, never the repository. */
@Component
public class ProductDataSeeder implements CommandLineRunner {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    public ProductDataSeeder(LoadProductPort loadProductPort, SaveProductPort saveProductPort) {
        this.loadProductPort = loadProductPort;
        this.saveProductPort = saveProductPort;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (loadProductPort.count() > 0) {
            return;
        }
        seed("Bàn phím cơ", new BigDecimal("1200000"), 10);
        seed("Chuột không dây", new BigDecimal("450000"), 25);
        seed("Màn hình 27inch", new BigDecimal("5500000"), 5);
    }

    /**
     * Two steps, not one: the product is created empty, then the opening stock is
     * RECEIVED. Seeded rows go through the same ledger as everything else, so they are
     * not a permanent hole in `SUM(movements) == stock`.
     */
    private void seed(String name, BigDecimal price, int openingStock) {
        Product saved = saveProductPort.save(Product.create(name, Money.of(price)));
        saveProductPort.apply(saved.receive(openingStock));
    }
}
