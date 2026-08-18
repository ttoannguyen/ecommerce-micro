# ADR-001: Tập trung vào Order và Inventory

- Status: accepted
- Date: 2026-08-18

## Context

Repository có tên ecommerce nhưng code mạnh nhất nằm ở reserve stock, concurrency,
saga và inventory ledger. Một nền tảng ecommerce đầy đủ còn cần storefront,
search, promotion, customer, payment và nhiều phần không phục vụ trực tiếp bài
toán consistency hiện tại.

## Decision

Định vị dự án là **Order & Inventory Platform** cho checkout đa kênh, flash sale
và fulfillment. Inventory Management & Order Fulfillment là phạm vi cốt lõi.

Catalog, Payment và Notification chỉ được mở rộng khi hỗ trợ trực tiếp order hoặc
inventory lifecycle.

## Consequences

- Roadmap ưu tiên reservation, ledger, reconciliation và concurrency.
- Không cần xây frontend ecommerce đầy đủ.
- Có thể ứng dụng cho web, POS, marketplace và WMS-lite.
- Service mới phải có ownership/lifecycle rõ ràng.

