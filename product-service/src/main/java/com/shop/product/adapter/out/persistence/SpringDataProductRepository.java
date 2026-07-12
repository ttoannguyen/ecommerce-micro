package com.shop.product.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository Spring Data cho bản ghi JPA. */
interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, Long> {
}
