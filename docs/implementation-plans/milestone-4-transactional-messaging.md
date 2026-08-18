# Milestone 4 — Transactional messaging

Status: completed
Updated: 2026-08-18

## Mục tiêu

Phát event order mà không phụ thuộc vào việc Kafka có sẵn sàng ngay tại thời
điểm ghi order. Event được ghi cùng local transaction với aggregate, publisher
có retry, và consumer không tạo side effect trùng khi Kafka giao lại event.

## Đã triển khai

- Order service ghi `ORDER_CREATED`, `ORDER_PAID` và `ORDER_CANCELLED` vào
  `outbox_events` trong transaction ghi order.
- Event envelope có `eventId`, `eventType`, `aggregateType`, `aggregateId`,
  `occurredAt`, `schemaVersion` và payload snapshot.
- Publisher claim batch bằng pessimistic lock, publish qua Kafka, retry với
  exponential backoff và reclaim event `PROCESSING` bị kẹt sau timeout.
- Notification consumer ghi `inbox_events` cùng `notification_log`; event ID
  đã xử lý được bỏ qua khi replay.
- Kafka listener retry hữu hạn rồi chuyển poison event sang
  `order-events.DLT`; Compose chạy Kafka KRaft.
- Integration test dùng PostgreSQL và Kafka thật.

## Failure scenarios đã xử lý

- Order commit trước khi publisher chạy: event vẫn còn `PENDING` trong outbox.
- Kafka tạm thời lỗi: event trở về `PENDING` với `next_attempt_at` tăng dần.
- Process chết khi đang publish: event `PROCESSING` quá hạn được claim lại.
- Consumer nhận cùng event nhiều lần: inbox unique theo `eventId`, notification
  chỉ tạo một lần.
- Payload không parse được: listener retry hữu hạn rồi đưa sang DLT.

## Verification

```bash
MAVEN_USER_HOME=/tmp/ecommerce-maven-m2 ./mvnw -B verify
```

Integration test tạo order thật, chờ outbox chuyển sang `PUBLISHED`, kiểm tra
notification được tạo và gửi lại nguyên envelope; số notification/inbox không
tăng sau replay.

## Phần mở rộng tiếp theo

- Thêm endpoint/metric vận hành để inspect và replay DLT có audit.
- Fault injection kill process giữa DB commit và Kafka publish.
- Đưa event contract dùng chung sang module/schema registry khi có consumer
  độc lập thứ hai.

## Definition of done

- [x] Aggregate update và outbox insert cùng local transaction.
- [x] Event envelope versioned, có ID ổn định.
- [x] Publisher retry/backoff và reclaim event đang xử lý.
- [x] Inbox deduplication cùng notification side effect.
- [x] Kafka integration test với PostgreSQL thật.
- [x] Dead-letter topic được cấu hình cho poison event.
