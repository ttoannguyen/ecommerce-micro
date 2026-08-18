# Milestone 3 — Order lifecycle

Status: in progress  
Updated: 2026-08-18

## Mục tiêu

Mở rộng order từ một SKU thành nhiều line, giữ snapshot giá/tên/reservation
trong `orderdb`, và bảo đảm lúc tạo order hoặc giữ được toàn bộ SKU hoặc không
SKU nào bị giữ.

## Đã triển khai

- `POST /orders/batch` nhận nhiều `{productId, quantity}`.
- Order gửi một batch command tới `product-service` bằng OpenFeign.
- Product khóa các product row theo thứ tự `productId`, tạo các reservation trong
  cùng transaction và rollback toàn bộ nếu một line thất bại.
- `orders` có migration `V3__order_items.sql`; mỗi line lưu product snapshot,
  unit price, quantity và reservation UUID.
- Order response có `items` và status `RESERVED` cho batch order.
- State machine domain có các trạng thái `PENDING_RESERVATION`, `RESERVED`,
  `PAYMENT_PENDING`, `PAID`, `CANCELLED` và `FAILED`.
- Reservation idempotency chuyển sang khóa `(caller, idempotency_key,
  product_id)` để một batch dùng chung idempotency key cho nhiều SKU.

## Verification

```bash
MAVEN_USER_HOME=/tmp/ecommerce-maven-m2 \
  ./mvnw -B -pl integration-tests -am \
  -Dtest=Milestone2IntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Integration test dùng hai PostgreSQL container thật và chứng minh batch order
giữ được hai SKU; khi SKU thứ hai thiếu hàng, SKU thứ nhất không bị giữ.

## Phần còn lại

- Thêm command/API `pay`, `cancel` và transition history.
- Hoàn thiện optimistic version/pagination cho order query.
- Tách test class thành suite Milestone 3 riêng và thêm concurrency test cho
  hai batch có thứ tự SKU ngược nhau.

## Definition of done

- [x] Nhiều OrderItem và snapshot từng line.
- [x] Batch reservation atomic xuyên HTTP boundary.
- [x] Lock SKU theo thứ tự ổn định.
- [x] State machine domain cơ bản.
- [x] Integration regression với PostgreSQL thật.
- [ ] Payment/cancel transition API.
- [ ] Transition history và pagination.
