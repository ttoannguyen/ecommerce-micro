package com.shop.product.adapter.in.bootstrap;

import com.shop.product.domain.model.Money;
import com.shop.product.domain.model.Product;
import com.shop.product.domain.port.out.LoadProductPort;
import com.shop.product.domain.port.out.SaveProductPort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Seed vài sản phẩm khi DB trống. Chạy qua port, không đụng repository trực tiếp. */
@Component
public class ProductDataSeeder implements CommandLineRunner {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    public ProductDataSeeder(LoadProductPort loadProductPort, SaveProductPort saveProductPort) {
        this.loadProductPort = loadProductPort;
        this.saveProductPort = saveProductPort;
    }

    @Override
    public void run(String... args) {
        if (loadProductPort.count() > 0) {
            return;
        }
        saveProductPort.save(Product.create("Bàn phím cơ", Money.of(new BigDecimal("1200000")), 10));
        saveProductPort.save(Product.create("Chuột không dây", Money.of(new BigDecimal("450000")), 25));
        saveProductPort.save(Product.create("Màn hình 27inch", Money.of(new BigDecimal("5500000")), 5));
    }
}
