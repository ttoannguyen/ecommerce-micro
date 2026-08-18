# Milestone 2 — Integration readiness

Status: completed  
Updated: 2026-08-18

## Mục tiêu

Chứng minh reservation và order hoạt động trên PostgreSQL thật, qua HTTP thật
giữa hai application, thay vì chỉ dựa trên H2 và unit/application tests.

## Phạm vi đã triển khai

- Thêm module `integration-tests` vào Maven reactor.
- Dùng Testcontainers 2.0.5 với hai container PostgreSQL 16 độc lập:
  `productdb` và `orderdb`.
- Khởi động `ProductServiceApplication` và `OrderServiceApplication` với port
  động trong cùng bài test.
- Gọi product-service trực tiếp bằng HTTP để kiểm tra hold, replay và
  reconciliation.
- Gọi order-service bằng HTTP; order-service gọi product-service qua OpenFeign
  thật.
- Kiểm tra order replay, idempotency conflict và insufficient stock.
- Tách Flyway location thành `db/migration/product` và `db/migration/order` để
  hai service có thể cùng xuất hiện trên một test classpath mà không collision
  migration version.
- Giữ plain jar làm artifact Maven và tạo executable `*-exec.jar` riêng cho
  Docker image.

## Evidence

Lệnh chạy integration path:

```bash
MAVEN_USER_HOME=/tmp/ecommerce-maven-m2 \
  ./mvnw -B -pl integration-tests -am \
  -Dtest=Milestone2IntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Kết quả xác minh ngày 2026-08-18:

- PostgreSQL 16.15 container khởi động thành công.
- Product migration: 3 migration pass, đến version V3.
- Order migration: 2 migration pass, đến version V2.
- `Milestone2IntegrationTest`: 3 tests pass.
- OpenFeign order → product chạy qua HTTP với port động.
- Root Maven reactor và CI workflow bao gồm integration module.

## Giới hạn còn lại

- Chưa có fault injection cho timeout trước/sau commit hoặc compensation failure.
- Chưa có multi-SKU order/reservation.
- Docker Compose smoke test đầy đủ giữa containerized services sẽ là evidence bổ
  sung; integration test hiện khởi động application trực tiếp để feedback nhanh.
- Testcontainers cần Docker daemon chạy; CI runner phải cung cấp Docker socket.

## Definition of Done

- [x] PostgreSQL schema được Flyway migrate trên database thật.
- [x] Hai service chạy cùng lúc và dùng database riêng.
- [x] OpenFeign call được kiểm thử qua HTTP thật.
- [x] Replay/conflict/insufficient-stock path có assertion.
- [x] Integration test nằm trong Maven reactor và CI quality workflow.
