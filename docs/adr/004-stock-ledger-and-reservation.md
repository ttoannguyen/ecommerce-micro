# ADR-004: Tách stock ledger khỏi reservation

- Status: accepted
- Date: 2026-08-18

## Context

Code hiện tại giảm product.stock và tạo ISSUE ngay khi reserve. Cách này chống
oversell nhưng mô hình một hold logic như thể hàng đã rời kho vật lý. Nó làm khó
việc trả lời riêng on-hand, reserved và available.

## Decision

- Dùng ba khái niệm: on-hand, reserved và available.
- Available bằng on-hand trừ reserved.
- Hold/release/expire chỉ thay đổi reservation và reserved projection.
- Receipt, fulfillment, return, damage và adjustment mới tạo StockMovement.
- Fulfillment reservation giảm reserved và on-hand trong cùng transaction.
- StockMovement append-only; sửa sai bằng reversal.

## Consequences

- Báo cáo phân biệt hàng vật lý và hàng có thể bán.
- Reservation expiry không tạo movement vật lý giả.
- Cần migration từ projection product.stock hiện tại.
- Cần reconciliation riêng cho on-hand/ledger và reserved/reservation.
- Flow confirm/fulfillment phức tạp hơn reserve-trừ-ngay nhưng chính xác hơn.
