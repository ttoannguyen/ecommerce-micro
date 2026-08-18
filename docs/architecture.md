# Kiến trúc hệ thống

## Mục tiêu

Kiến trúc bảo vệ ba điều khó nhất:

- không bán vượt tồn kho khả dụng;
- không mất hoặc nhân đôi side effect khi request/event được retry;
- truy vết được quan hệ giữa order, reservation và stock movement.

## Kiến trúc hiện tại

~~~text
Client
  |
  v
Order Service ------ REST/OpenFeign ------> Product Service
  |                                             |
orderdb                                      productdb
~~~

Mỗi service dùng Hexagonal Architecture:

~~~text
adapter/in -> application -> domain
adapter/out implements ports owned by domain/application
~~~

Domain không phụ thuộc Spring, JPA, HTTP hoặc Kafka.

## Bounded context

| Context | Sở hữu | Không sở hữu |
|---|---|---|
| Order | order, item, status, snapshot, reservation reference | stock hiện tại |
| Inventory | balance, reservation, stock movement, allocation | order lifecycle, giá hiển thị |
| Catalog | product description, price, sellability | stock và reservation |
| Payment | payment attempt, provider reference, payment status | order/inventory state |

Product service hiện gộp Catalog và Inventory. Giữ cách này trong ngắn hạn.
Chỉ tách khi giá/catalog và stock/reservation đã có lifecycle độc lập rõ ràng.

## Mô hình tồn kho mục tiêu

~~~text
available = on_hand - reserved
~~~

- on-hand: lượng vật lý đang ở kho.
- reserved: lượng đang được giữ bởi reservation HELD.
- available: lượng còn có thể hứa bán.

Hold/release/expire thay đổi reserved, không tạo physical stock movement.
Fulfillment/issue mới giảm on-hand và tạo movement. Receipt, return, damage và
adjustment cũng tạo movement.

## Transaction boundary

Các thao tác sau phải atomic trong Inventory:

- kiểm tra available và tạo reservation;
- confirm/fulfill reservation và ghi movement;
- release/expire reservation và cập nhật reserved projection;
- receipt/return/adjustment và cập nhật on-hand projection;
- ghi aggregate và outbox event.

Order transaction không thể rollback inventory transaction. Cross-service flow
dùng saga, idempotency, TTL và reconciliation thay cho distributed transaction.

## Giao tiếp

### Đồng bộ

REST dùng khi caller cần quyết định ngay:

- hold inventory;
- query reservation;
- command confirm/release có idempotency.

Mọi call phải có timeout. Retry chỉ bật sau khi endpoint idempotent.

### Bất đồng bộ

Kafka dùng cho fact đã xảy ra và side effect không cần nằm trên critical path:

- OrderCreated;
- ReservationExpired;
- OrderConfirmed;
- InventoryIssued;
- notification và reporting.

Event được phát qua transactional outbox. Consumer chấp nhận at-least-once và
phải deduplicate bằng event ID.

## Dữ liệu

- Database per service; không query hoặc foreign key xuyên database.
- Flyway là nguồn sự thật của schema.
- Order lưu snapshot cần thiết cho lịch sử.
- StockMovement là append-only fact của thay đổi vật lý.
- Reservation là nguồn sự thật của phần đang giữ.
- Balance là projection đọc nhanh và phải reconciliation được.

## Locking

- Hot SKU dùng pessimistic/conditional locking trong transaction ngắn.
- Khi giữ nhiều SKU, lock theo thứ tự ID ổn định.
- Order transition dùng optimistic version.
- Expiry worker dùng batch lock; PostgreSQL SKIP LOCKED được cân nhắc khi chạy
  nhiều instance.

## Observability

Request và event mang correlation/trace ID. Metric tối thiểu:

- order created/rejected;
- reservation held/released/expired;
- hold latency và conflict rate;
- outbox pending/publish failure;
- HTTP latency/error theo dependency.

## Service discovery

Docker Compose và Kubernetes đã có DNS theo service name. Eureka không phải yêu
cầu mặc định; chỉ thêm khi deployment model thực sự cần registry riêng.

