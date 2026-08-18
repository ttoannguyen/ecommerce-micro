# Quy tắc nghiệp vụ

Các rule dưới đây là invariant mục tiêu. UI có thể diễn đạt khác, nhưng
application service và database không được vi phạm chúng. Trạng thái triển khai
được theo dõi riêng tại [current-state](../product/current-state.md).

## BR-ORD — Order

- **BR-ORD-001:** Create order phải idempotent theo sales channel và key.
- **BR-ORD-002:** Order không tự quyết định đủ tồn từ dữ liệu đã đọc trước đó.
- **BR-ORD-003:** Order item lưu snapshot SKU, tên, giá và quantity tại thời điểm đặt.
- **BR-ORD-004:** Total dùng decimal chính xác và bằng tổng các line snapshot.
- **BR-ORD-005:** State transition phải đi qua domain policy và kiểm tra version.
- **BR-ORD-006:** Confirm/cancel đồng thời chỉ có một transition thắng.
- **BR-ORD-007:** Order đã confirmed/cancelled không bị hard-delete.
- **BR-ORD-008:** Order lưu reference tới reservation dùng để đáp ứng nó.

## BR-RES — Reservation

- **BR-RES-001:** Mỗi reservation có ID ổn định và ít nhất một line.
- **BR-RES-002:** Mọi quantity phải lớn hơn 0.
- **BR-RES-003:** Chỉ tạo hold khi tất cả line còn đủ available.
- **BR-RES-004:** Giữ nhiều SKU là atomic; không để lại partial hold.
- **BR-RES-005:** Reservation mới bắt đầu ở HELD và có expiresAt.
- **BR-RES-006:** HELD chỉ chuyển sang CONFIRMED, RELEASED hoặc EXPIRED.
- **BR-RES-007:** Confirm, release và expire phải idempotent.
- **BR-RES-008:** Release/expire chỉ giảm reserved đúng một lần.
- **BR-RES-009:** Reservation terminal không được quay lại HELD.
- **BR-RES-010:** Expiry dùng server clock và được test bằng clock điều khiển được.

## BR-IDEM — Idempotency

- **BR-IDEM-001:** Idempotency key phải unique trong phạm vi operation và caller.
- **BR-IDEM-002:** Replay cùng key và cùng payload trả lại kết quả đã lưu.
- **BR-IDEM-003:** Cùng key nhưng payload khác bị từ chối.
- **BR-IDEM-004:** Key và kết quả sống đủ lâu hơn cửa sổ retry của caller.
- **BR-IDEM-005:** Retry chỉ được bật cho operation đáp ứng các rule idempotency.

## BR-INV — Inventory balance

- **BR-INV-001:** On-hand, reserved và available có nghĩa riêng, không dùng lẫn.
- **BR-INV-002:** Available bằng on-hand trừ reserved.
- **BR-INV-003:** On-hand và reserved không âm; reserved không vượt on-hand.
- **BR-INV-004:** Hold chỉ tăng reserved; không làm giảm on-hand.
- **BR-INV-005:** Release/expire chỉ giảm reserved; không tạo stock movement.
- **BR-INV-006:** Fulfillment giảm cả reserved và on-hand trong một transaction.
- **BR-INV-007:** Mọi quyết định available chạy trong transaction tại Inventory.
- **BR-INV-008:** Khi lock nhiều SKU, thứ tự lock phải deterministic.
- **BR-INV-009:** Không sửa projection mà bỏ qua aggregate/ledger liên quan.

## BR-STK — Stock movement

- **BR-STK-001:** Mọi thay đổi on-hand phải có StockMovement.
- **BR-STK-002:** Movement là append-only và không hard-delete.
- **BR-STK-003:** Quantity có dấu; zero không phải một movement hợp lệ.
- **BR-STK-004:** Movement có type, thời điểm và reference tới chứng từ nguồn.
- **BR-STK-005:** Adjustment cần reason và actor.
- **BR-STK-006:** Sai movement được sửa bằng reversal/compensation.
- **BR-STK-007:** On-hand projection phải tái tạo được từ opening balance và ledger.
- **BR-STK-008:** Reservation không phải physical movement.

## BR-SAGA — Cross-service consistency

- **BR-SAGA-001:** Không transaction nào rollback được database service khác.
- **BR-SAGA-002:** Compensation gọi command idempotent bằng aggregate ID.
- **BR-SAGA-003:** Ambiguous HTTP outcome được xử lý bằng query/retry hoặc TTL.
- **BR-SAGA-004:** Order và reservation có reference để reconciliation.
- **BR-SAGA-005:** Terminal failure cần trạng thái/audit, không chỉ log rồi bỏ qua.

## BR-EVT — Event

- **BR-EVT-001:** Aggregate update và outbox insert nằm trong cùng local transaction.
- **BR-EVT-002:** Event có ID, type, aggregate ID, occurredAt và schema version.
- **BR-EVT-003:** Consumer phải chịu được event trùng.
- **BR-EVT-004:** Không tuyên bố exactly-once end-to-end.
- **BR-EVT-005:** Event lỗi nhiều lần đi vào dead-letter path có thể replay.
- **BR-EVT-006:** Event contract thay đổi phải tương thích hoặc có version mới.

## BR-REC — Reconciliation

- **BR-REC-001:** Reserved projection bằng tổng reservation line đang HELD.
- **BR-REC-002:** On-hand projection bằng opening balance cộng ledger.
- **BR-REC-003:** Reconciliation không tự sửa dữ liệu mà không để lại audit.
- **BR-REC-004:** Sai lệch chỉ ra SKU, expected, actual và nguồn tính.

