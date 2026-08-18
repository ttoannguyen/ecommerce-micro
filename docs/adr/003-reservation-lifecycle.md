# ADR-003: Reservation có identity, TTL và idempotency

- Status: accepted
- Date: 2026-08-18

## Context

API hiện reserve/release bằng productId + quantity. Sau timeout, caller không
biết Inventory đã commit hay chưa. Release mù có thể tạo stock; không release có
thể làm rò rỉ stock. Không có reservation ID cũng khiến retry, audit và
reconciliation không đáng tin cậy.

## Decision

- Reservation là aggregate được lưu với UUID.
- Trạng thái gồm HELD, CONFIRMED, RELEASED và EXPIRED.
- Hold bắt buộc có idempotency key và expiresAt.
- Confirm/release/expire command theo reservation ID và idempotent.
- Worker expire reservation HELD quá hạn.
- Order lưu reservation ID.
- Cùng key/cùng payload trả kết quả cũ; cùng key/payload khác bị từ chối.

## Consequences

- Caller có thể query/retry sau ambiguous outcome.
- Hold bị bỏ quên có đường tự phục hồi.
- Cần unique constraint, expiry worker và concurrency test.
- Cần lưu idempotency result và policy retention.
- API cũ release theo product/quantity phải bị thay thế.
