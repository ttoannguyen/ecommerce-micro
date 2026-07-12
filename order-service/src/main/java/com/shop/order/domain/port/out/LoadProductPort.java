package com.shop.order.domain.port.out;

import com.shop.order.domain.model.ProductSnapshot;

/** Outbound port: lấy dữ liệu product từ bounded context khác. */
public interface LoadProductPort {

    ProductSnapshot loadProduct(Long productId);
}
