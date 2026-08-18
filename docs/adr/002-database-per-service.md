# ADR-002: Database per service và local transaction

- Status: accepted
- Date: 2026-08-18

## Context

Order và Inventory có ownership và transaction boundary khác nhau. Cho phép
order-service đọc productdb sẽ làm rò boundary và khuyến khích quyết định tồn kho
từ dữ liệu không thuộc quyền sở hữu.

Distributed transaction/XA làm coupling deployment và không loại bỏ nhu cầu xử
lý timeout hoặc message trùng ở boundary bên ngoài.

## Decision

- Mỗi service sở hữu database riêng.
- Service khác chỉ truy cập qua contract REST/event.
- Không có query, foreign key hoặc shared repository xuyên database.
- Cross-service workflow dùng local transaction, saga, idempotency, TTL, outbox
  và reconciliation.
- Flyway migration được quản lý riêng cho từng service.

## Consequences

- Inventory là nơi duy nhất được quyết định available.
- Order lưu snapshot/reference cần thiết thay vì join runtime.
- Rollback cục bộ không hoàn tác HTTP call đã commit.
- Contract, failure handling và observability trở thành phần bắt buộc.

