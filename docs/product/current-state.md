# Hiện trạng dự án

- Updated: 2026-08-18
- Branch reviewed: main
- Scope: root build, reservation lifecycle, order-service, product-service,
  Docker Compose và integration test hiện có

## Tóm tắt

Dự án đã có reservation identity, TTL, idempotency và stock ledger. Hold không
còn bị mô hình như xuất kho vật lý: Product tách on-hand, reserved và available;
Order lưu reference tới reservation. Milestone 2 đã bổ sung evidence trên
PostgreSQL/OpenFeign thật. Milestone 3 hiện đã có multi-SKU batch reservation,
order line snapshot, payment/cancel transition và transition history.

## Kiến trúc đang chạy

~~~text
Client
  |
  v
Order Service -- REST/OpenFeign --> Product Service
  |                                    |
orderdb                             productdb
~~~

- Mỗi service có PostgreSQL riêng.
- Flyway sở hữu schema; Hibernate chạy validate.
- Profile dev và test dùng H2.
- Docker Compose khởi động hai database và hai application.

## Phần đã có

### Service boundary

- Order không truy cập productdb.
- Product là nơi quyết định đủ tồn kho.
- HTTP status được dịch ở web adapter, không đi vào domain.
- Domain không phụ thuộc Spring/JPA/Feign.

### Inventory correctness

- Hold kiểm tra available và tăng reserved trong một transaction.
- Repository dùng PESSIMISTIC_WRITE.
- Version là lớp bảo vệ cho update ngoài đường reserve.
- Hold/release/expire không tạo physical stock movement.
- Confirm giảm on-hand và reserved, đồng thời ghi ISSUE trong cùng transaction.
- Concurrency test kiểm tra 20 thread tranh 5 available mà không over-reserve.

### Reservation lifecycle

- Reservation có UUID, caller-scoped idempotency key, HELD/CONFIRMED/RELEASED/EXPIRED,
  createdAt, expiresAt và optimistic version.
- Cùng key/cùng payload trả lại aggregate cũ; payload khác trả conflict.
- Confirm, release và expire khóa reservation row và không lặp side effect.
- Expiry worker quét batch, mỗi command khóa lại row nên nhiều instance không
  release hai lần.
- `Clock` được inject; test điều khiển thời gian chứng minh hold bỏ quên tự phục hồi.
- Reconciliation đối chiếu ledger với on-hand và tổng HELD với reserved.

### Data lifecycle

- Flyway migration tạo schema và backfill opening stock movement.
- Order lưu price snapshot, reservation ID, idempotency key và thời điểm tạo.
- Order batch lưu từng line trong `order_items`, gồm tên/giá snapshot, quantity
  và reservation ID.
- Product seeder đi qua use case/port thay vì ghi repository trực tiếp.

### Developer experience

- Maven Wrapper và Dockerfile riêng cho từng service.
- Root Maven reactor chạy build và test cho cả hai service bằng `./mvnw -B verify`.
- GitHub Actions quality workflow được cấu hình để chạy root verify, kiểm tra diff/Compose và build hai image.
- H2 profile cho chạy local không cần PostgreSQL.
- Module `integration-tests` dùng Testcontainers 2.0.5, PostgreSQL 16 và chạy hai
  application context với port động.
- Order service có transactional outbox v6, Kafka publisher retry/backoff,
  inbox deduplication và notification log; Compose chạy Kafka KRaft.
- Integration test chứng minh Flyway PostgreSQL, reservation replay, order replay,
  idempotency conflict, insufficient stock và batch rollback qua OpenFeign.
- OpenAPI/Swagger, Actuator health/info/metrics/prometheus, liveness/readiness
  probe, Prometheus outbox backlog gauge và Micrometer tracing bridge.
- Feign có timeout; product call có retry bounded, circuit breaker và bulkhead.
- Correlation ID được trả về qua REST, truyền qua Feign và lưu trong outbox/event;
  structured request logs đưa correlation ID vào MDC.

## Khoảng trống mức P1

- Chưa có stress test PostgreSQL locking/isolation ở quy mô lớn.
- Saga orchestration có unit test, nhưng chưa có integration test cho timeout
  trước/sau commit, compensation failure và ambiguous response.
- Order batch đã hỗ trợ nhiều sản phẩm; endpoint legacy `/orders` vẫn giữ một SKU.
- Chưa có pagination và stress test concurrency cho nhiều batch.
- API danh sách chưa phân trang.
- Expiry worker dùng row lock an toàn nhưng chưa dùng PostgreSQL `SKIP LOCKED` để
  chia batch hiệu quả dưới nhiều instance.

## Khoảng trống mức P2

- Chưa có fault-injection test cho crash giữa DB commit và Kafka publish.
- Chưa có endpoint vận hành để inspect/replay DLT có audit.
- Chưa có fault-injection test để đo circuit breaker dưới outage dài.
- Chưa có collector/dashboard production cho trace span; hiện application đã tạo
  trace context và expose metrics để nối vào hạ tầng observability.
- Chưa có authentication/authorization.
- Docker image build với test bị skip và chưa chạy non-root.
- Production config vẫn bật SQL logging và có credential mặc định.

## Nợ tài liệu và build

- Dockerfile hiện vẫn skip test; root `verify` là quality gate trước Docker build.
- HANDOFF.md là snapshot bàn giao và có thể lệch với source hiện tại.

## Quyết định tiếp theo

Milestone tiếp theo là resilience và observability. Pagination, concurrency
stress và fault-injection messaging sẽ được xử lý như hardening song song.
Xem [roadmap](roadmap.md).
