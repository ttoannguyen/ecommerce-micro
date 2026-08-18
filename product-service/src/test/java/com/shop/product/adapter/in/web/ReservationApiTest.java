package com.shop.product.adapter.in.web;

import com.shop.product.domain.model.Money;
import com.shop.product.domain.model.Product;
import com.shop.product.domain.port.out.SaveProductPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired SaveProductPort products;

    @Test
    void holdRequiresKeyAndReturnsReservationIdentityAndBalances() throws Exception {
        Product created = products.save(Product.create(
                "API product " + System.nanoTime(), Money.of(new BigDecimal("1000"))));
        Product stocked = products.apply(created.receive(10));

        mockMvc.perform(post("/products/{id}/reservations", stocked.id())
                        .header("Idempotency-Key", "api-hold-" + stocked.id())
                        .header("X-Caller-Id", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.onHand").value(10))
                .andExpect(jsonPath("$.reserved").value(3))
                .andExpect(jsonPath("$.available").value(7));
    }

    @Test
    void holdWithoutIdempotencyKeyIsRejected() throws Exception {
        mockMvc.perform(post("/products/1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest());
    }
}
