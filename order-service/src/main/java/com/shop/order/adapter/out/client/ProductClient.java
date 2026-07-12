package com.shop.order.adapter.out.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Gọi Product Service qua HTTP. Order KHÔNG đụng DB Product —
 * phải hỏi qua API. Đây là DB-per-service.
 * Tuần 2 bỏ url cứng, dùng tên service qua Eureka.
 */
@FeignClient(name = "product-service", url = "${product-service.url}")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductResponse getProduct(@PathVariable("id") Long id);
}
