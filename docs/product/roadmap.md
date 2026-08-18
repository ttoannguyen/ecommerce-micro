# Roadmap

Mỗi milestone phải mô tả đủ bốn phần: concept, failure scenario, implementation
và evidence. Chi tiết các kiến thức và failure mode cần bao phủ nằm tại
[bản đồ kiến thức microservice](../learning/microservice-knowledge-map.md).

Thứ tự thực hiện dựa trên dependency:

~~~text
build hygiene
    -> reservation correctness
    -> integration evidence
    -> realistic order lifecycle
    -> messaging
    -> resilience/observability
    -> security/deployment
~~~

## Milestone 0 — Build hygiene (completed)

Implementation plan: [Root build và CI](../implementation-plans/milestone-0-build-ci.md).

- Xóa dependency Flyway bị khai báo trùng.
- Thêm root command chạy test cả hai service.
- Thêm GitHub Actions cho compile, test, verify và Docker build.
- Đồng bộ README, branch và tài liệu.

Đã hoàn thành: root Maven reactor và wrapper dùng `./mvnw -B verify`; workflow
quality kiểm tra push/PR vào `main`, Compose config và build hai image. Workflow
đã được tạo nhưng trạng thái chạy trên GitHub phải được xác nhận ở repository.

## Milestone 1 — Reservation lifecycle (completed)

Implementation plan: [Reservation lifecycle](../implementation-plans/milestone-1-reservation-lifecycle.md).

- Tạo Reservation có UUID, status, idempotency key và expiresAt.
- Hỗ trợ hold, confirm, release và expire.
- Cùng key/cùng payload trả kết quả cũ; payload khác bị từ chối.
- Job expiry khóa an toàn khi chạy nhiều instance.
- Order lưu reservation ID.
- Inject Clock để test TTL.
- Tách on-hand, reserved và available.

Hoàn thành khi retry không tạo side effect trùng, hold bị bỏ quên tự phục hồi và
reconciliation phát hiện được sai lệch.

Đã hoàn thành: hold có UUID/TTL/idempotency scope theo caller, terminal command
khóa reservation và idempotent, expiry worker dùng server Clock, Product tách
on-hand/reserved/available, Order lưu reservation ID và create order cũng
idempotent. Reconciliation đối chiếu cả ledger/on-hand và HELD/reserved.

## Milestone 2 — Integration evidence (completed)

Implementation plan: [Integration readiness](../implementation-plans/milestone-2-integration-readiness.md).

- Testcontainers PostgreSQL cho hai database độc lập.
- Hai Spring application chạy đồng thời trong integration test.
- OpenFeign order → product qua HTTP thật.
- Idempotency replay, conflict và insufficient stock xuyên service.
- Flyway migration được tách location theo service để tránh collision khi test chung classpath.
- CI chạy integration path cùng root verify.

Timeout trước/sau commit và compensation failure vẫn là scenario mở rộng của
Milestone 5; Milestone 2 đã chứng minh database, HTTP boundary và các path cơ bản.

Hoàn thành: CI chạy được integration path thật mà không cần test tay.

## Milestone 3 — Order lifecycle (completed)

Implementation plan: [Order lifecycle](../implementation-plans/milestone-3-order-lifecycle.md).

- Nhiều OrderItem và snapshot từng line.
- State machine cho order.
- Reserve nhiều SKU nguyên tử.
- Lock SKU theo thứ tự ổn định để giảm deadlock.
- Optimistic version, pagination và transition history.

Đã có `POST /orders/batch`, batch hold xuyên OpenFeign, rollback transaction,
`pay/cancel` transition và transition history. Pagination và concurrency stress
test là phần mở rộng sau milestone.

Phạm vi core đã hoàn thành với batch reservation atomic, state transition và
release reservation khi cancel. Pagination và concurrency stress test còn lại
là phần hardening tiếp theo.

## Milestone 4 — Transactional messaging (completed)

Implementation plan: [Transactional messaging](../implementation-plans/milestone-4-transactional-messaging.md).

- Kafka, outbox event và publisher có retry/backoff.
- Inbox/deduplication cho consumer.
- Event envelope có ID, type, aggregate ID, time và schema version.
- Dead-letter topic và replay procedure.
- Notification consumer nhỏ để chứng minh fan-out.

Đã có outbox cùng transaction, publisher retry/backoff, event envelope versioned,
inbox deduplication, notification side effect và DLT config. Fault injection
crash/replay DLT là phần hardening tiếp theo.

## Milestone 5 — Resilience và observability

- Connect/read timeout rõ ràng.
- Retry chỉ cho operation idempotent.
- Circuit breaker và bulkhead.
- Structured logs, correlation ID, metrics và tracing.
- Liveness/readiness probe riêng.

Hoàn thành khi truy vết được một order xuyên REST/database/event và dashboard
hiển thị throughput, error rate, latency cùng backlog.

## Milestone 6 — Security, deployment và performance

- OAuth2/JWT và authorization theo actor.
- Secret ngoài source code.
- Container non-root, graceful shutdown và application health check.
- k6/Gatling test cho hot SKU và order flow.
- Backup/restore và migration rehearsal.

Hoàn thành khi quyền được kiểm chứng server-side, load test tái tạo được và quy
trình deploy/restore đã được diễn tập.

## Những việc cố ý không làm sớm

- Eureka khi Docker/Kubernetes DNS đã đủ.
- XA/distributed transaction.
- Event sourcing toàn hệ thống.
- Tách Catalog/Inventory trước khi reservation boundary ổn định.
- Thêm service không có ownership hoặc lifecycle độc lập.
