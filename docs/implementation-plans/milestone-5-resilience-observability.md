# Milestone 5 — Resilience và observability

Status: completed
Updated: 2026-08-18

## Mục tiêu

Làm cho luồng `order-service -> product-service` chịu được lỗi mạng ngắn hạn,
không retry mù các operation không an toàn, và để operator truy vết được một
request qua REST, database và Kafka.

## Đã triển khai

- OpenFeign có connect timeout 1 giây và read timeout 3 giây.
- Reserve batch, reserve một SKU và release được bọc bằng Resilience4j:
  circuit breaker, semaphore bulkhead và retry tối đa hai lần.
- Retry chỉ bắt `RetryableException`/`IOException`; các request reserve có
  `Idempotency-Key`, còn release là operation idempotent. Lỗi 409 và lỗi
  nghiệp vụ không bị retry.
- `X-Correlation-Id` được tạo nếu client không gửi, trả lại trong response,
  truyền qua Feign và lưu ở cột `outbox_events.correlation_id`.
- Kafka consumer phục hồi correlation ID vào MDC khi xử lý event. Log console
  có key-value fields cho correlation ID, method, path, status và duration.
- Micrometer Prometheus và tracing bridge được bật. HTTP metrics có throughput,
  error và latency; gauge `messaging.outbox.events{status=...}` hiển thị backlog.
- Actuator mở `metrics`/`prometheus` và tách probe:
  `/actuator/health/liveness`, `/actuator/health/readiness`.

## Failure scenarios đã xử lý

- Product service không phản hồi trong timeout: Feign kết thúc bounded, retry
  chỉ các lỗi kết nối và circuit breaker chặn storm tiếp theo.
- Nhiều request đồng thời gọi product: bulkhead giới hạn 20 cuộc gọi và chờ tối
  đa 100 ms.
- Request đi qua order rồi product: cùng correlation ID xuất hiện ở response,
  log và event envelope.
- Kafka replay: correlation ID vẫn được khôi phục, inbox deduplication của M4
  tiếp tục ngăn side effect trùng.

## Verification

```bash
./mvnw -B verify
docker compose config
docker compose build product-service order-service
```

Integration test kiểm tra response correlation ID, outbox lưu correlation ID,
Kafka notification/replay, hai probe health và Prometheus có HTTP/outbox metric.

## Giới hạn có chủ ý

- Chưa thêm exporter/collector bên ngoài; tracing bridge tạo trace context tại
  application boundary, còn việc lưu/hiển thị span phụ thuộc collector của môi
  trường triển khai.
- DLT replay/audit và fault injection giữa DB commit/Kafka publish vẫn là
  hardening của Milestone 4.

## Definition of done

- [x] Timeout và retry policy rõ ràng, retry chỉ operation idempotent.
- [x] Circuit breaker và bulkhead cho outbound product call.
- [x] Structured logs và correlation ID xuyên REST/Feign/outbox/consumer.
- [x] Metrics throughput/error/latency từ HTTP và backlog từ outbox.
- [x] Tracing bridge và probe liveness/readiness riêng.
- [x] Integration test chứng minh các tín hiệu vận hành chính.
