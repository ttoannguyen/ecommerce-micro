# Milestone 1: Reservation lifecycle

- Status: completed
- Updated: 2026-08-18
- Scope: reservation identity/lifecycle, idempotency, expiry, inventory balance,
  order reference và reconciliation

## Outcome

Inventory giữ hàng bằng reservation có UUID thay vì giảm stock mù theo
`productId + quantity`. Hold có TTL và idempotency key; confirm, release và
expire chạy theo reservation ID và không lặp side effect. Order lưu
`reservationId` và create order cũng dùng cùng idempotency key.

## API contract

### Hold

~~~http
POST /products/{productId}/reservations
Idempotency-Key: <1..128 characters>
X-Caller-Id: <caller scope>
Content-Type: application/json

{"quantity": 2}
~~~

Cùng caller/key/product/quantity trả reservation cũ. Cùng caller/key nhưng
product hoặc quantity khác trả `409 IDEMPOTENCY_CONFLICT`.

### Query và terminal commands

~~~text
GET    /reservations/{reservationId}
POST   /reservations/{reservationId}/confirm
DELETE /reservations/{reservationId}
GET    /products/{productId}/reconciliation
~~~

State machine:

~~~text
             confirm
HELD --------------------> CONFIRMED
  | release                   terminal
  +----------------------> RELEASED
  |
  | expiresAt reached
  +----------------------> EXPIRED
~~~

## Data model và migration

Product migration V3:

- rename `stock` thành `on_hand`;
- thêm `reserved` và database check `0 <= reserved <= on_hand`;
- tạo bảng `reservation` với UUID, caller/key unique, product, quantity,
  status, timestamps, expiry index và optimistic version.

Order migration V2 thêm `reservation_id` và unique `idempotency_key`. Hai cột
cho phép null để các order cũ trước Milestone 1 vẫn migrate được; mọi order mới
đều ghi cả hai.

## Transaction và concurrency

- Hold khóa Product, kiểm tra `available = onHand - reserved`, tăng reserved và
  insert reservation trong cùng transaction.
- Confirm khóa Reservation rồi Product, giảm reserved/on-hand và ghi ISSUE
  movement trong cùng transaction.
- Release/expire khóa Reservation rồi Product, giảm reserved đúng một lần và
  không tạo physical movement.
- Expiry worker chọn batch ID rồi command khóa lại từng reservation. Nhiều
  instance có thể nhìn cùng ID nhưng chỉ instance giữ lock mới chuyển HELD;
  instance sau đọc terminal state và không release lần hai.
- `Clock` là dependency được inject; TTL không gọi system clock trực tiếp trong
  application service.

## Idempotency và saga

- Inventory unique theo `(caller, idempotency_key)`.
- Order unique theo `idempotency_key` và replay cùng payload trả order cũ trước
  khi gọi Inventory.
- Nếu save order thất bại, compensation gọi DELETE bằng reservation UUID.
- Retry hold không tăng reserved lần hai; retry release/confirm/expire không lặp
  balance hoặc ledger effect.

## Verification evidence

Đã xác minh local trên nhánh `main` với Temurin JDK 21:

- Order: 18 test pass, gồm validation, domain arithmetic, order idempotency,
  reservation reference và compensation theo UUID.
- Product: 23 test pass, gồm domain lifecycle, HTTP contract, idempotency
  replay/conflict, Clock-based expiry, reconciliation và concurrent hold.
- 20 thread tranh 5 available: đúng 5 reservation thành công, on-hand giữ 5,
  reserved thành 5, available về 0 và reconciliation khớp.
- Flyway chạy schema H2 từ trống: Order V1..V2 và Product V1..V3.
- `./mvnw -B verify` thành công cho cả reactor; Order 18 test và Product 23 test.
- `docker compose config` thành công.
- `docker compose build product-service order-service` thành công và tạo lại hai
  image `ecommerce-micro-product-service:latest` và
  `ecommerce-micro-order-service:latest`.

## Known limitations

- Mỗi reservation hiện giữ đúng một SKU; multi-SKU atomic hold thuộc Milestone 3.
- Order chưa có confirm/cancel lifecycle; Inventory confirm API đã sẵn sàng cho
  order state machine ở Milestone 3.
- PostgreSQL locking, OpenFeign contract, timeout/ambiguous outcome và
  multi-instance expiry load chưa có integration evidence; đây là Milestone 2.
- Worker dùng row lock để đúng dữ liệu nhưng chưa tối ưu phân phối batch bằng
  PostgreSQL `SKIP LOCKED`.

## Definition of Done

- [x] Reservation có UUID, status, idempotency key và expiresAt.
- [x] Hold/confirm/release/expire tuân theo state machine.
- [x] Replay cùng payload không tạo side effect trùng; payload khác bị từ chối.
- [x] Expiry dùng injected Clock và giải phóng forgotten hold.
- [x] Terminal command khóa row và idempotent.
- [x] Product tách on-hand, reserved và available.
- [x] Hold/release/expire không tạo physical stock movement.
- [x] Confirm ghi ISSUE và giảm on-hand/reserved atomic.
- [x] Order lưu reservation ID và dùng idempotency key.
- [x] Reconciliation phát hiện được lệch ledger/balance/reservation projection.
- [x] Migration chạy được trên test profile từ schema trống.
- [x] Root quality gate pass.
