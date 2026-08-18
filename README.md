# ecommerce-micro — Order & Inventory Platform

Microservice học tập. 2 service, DB riêng mỗi service, gọi nhau qua REST (OpenFeign).

```
Client ──POST /orders/batch──► Order Service ──POST /reservations/batch──► Product Service
   (8082)                    │            (Feign)                              │  (8081)
                          orderdb (Postgres 5433)                     productdb (Postgres 5432)
```

## Yêu cầu

- JDK 21 cho build, test và chạy service local.
- Docker với Docker Compose v2 cho database và application containers.
- Không cần cài Maven global; Maven Wrapper ở root sẽ tự tải Maven 3.9.16.

## Build và test từ root

Lệnh chuẩn cho cả hai service là:

```bash
./mvnw -B verify
```

Lệnh này chạy Maven reactor ở root, compile/package và toàn bộ test của
`order-service` và `product-service`. CI dùng cùng lệnh này.

Kiểm tra cấu hình Compose và build hai application image:

```bash
docker compose config
docker compose build product-service order-service
```

## Luật quan trọng nhất: bất biến nằm ở nơi sở hữu dữ liệu

Order **không** hỏi "còn bao nhiêu hàng?" rồi tự quyết. Nó **ra lệnh** "giữ cho tôi 2 cái",
và Product tự quyết trong transaction của mình.

```
SAI (race condition):                    ĐÚNG (hold):
  Order: GET /products/1  -> stock=1       Order: POST /products/1/reservations {2}
  Order: GET /products/1  -> stock=1                Product: BEGIN
  Order: check ok, save                              SELECT ... FOR UPDATE
  Order: check ok, save   -> BÁN QUÁ HÀNG            available >= 2 ? reserved += 2
                                                    COMMIT
                                           -> 201 {reservationId, expiresAt}
```

Giữa lúc Order **đọc** stock và lúc nó **lưu** đơn luôn có khe hở cho request khác chen vào
(TOCTOU). Order không thể tự bịt: nó chỉ cầm một bản sao đã cũ. Chỉ Product — chủ sở hữu
dòng dữ liệu đó — mới bịt được.

`ReservationConcurrencyTest` chứng minh: 20 thread tranh 5 available → đúng 5 hold
thành công, on-hand vẫn là 5 và reserved là 5.

Vì `POST /reservations` đã commit ở productdb, `@Transactional` của Order **không** rollback
được nó. Nên `PlaceOrderService` phải tự **bồi hoàn** bằng reservation UUID khi lưu đơn
thất bại. Release/confirm/expire đều idempotent; hold bỏ quên tự hết hạn sau TTL.

## Kiến trúc (DDD / Hexagonal)

Mỗi service = 1 bounded context, chia 3 tầng theo ports & adapters:

```
com.shop.order
├── domain/                 # POJO thuần, KHÔNG annotation framework
│   ├── model/              # Order, Money/Quantity, ReservedProduct
│   ├── port/in/            # PlaceOrderUseCase, FindOrdersUseCase, PlaceOrderCommand
│   └── port/out/           # ReserveStockPort, SaveOrderPort, LoadOrderPort
├── application/            # điều phối use case (PlaceOrderService, OrderQueryService)
└── adapter/
    ├── in/web/             # OrderController + DTO + GlobalExceptionHandler
    ├── out/persistence/    # OrderJpaEntity, OrderMapper, OrderPersistenceAdapter
    └── out/client/         # ProductClient (Feign) + ProductClientAdapter
```

Luật: phụ thuộc chỉ hướng **vào trong** (adapter -> application -> domain). Domain
không biết Spring/JPA/Feign. Luật miền (đủ tồn kho, tính tiền) nằm trong aggregate.
`product-service` cùng khuôn (đọc catalog + seed dữ liệu qua port).

`HttpStatus` chỉ được xuất hiện ở `adapter/in/web` (xem `ErrorCode`). Domain ném
exception, `GlobalExceptionHandler` dịch sang mã HTTP — application không biết 400/409 là gì.

## Chạy bằng Docker Compose

```bash
cd ecommerce-micro
docker compose up --build
```

Chờ 4 container lên: `postgres-product`, `postgres-order`, `product-service`, `order-service`.

## Thử

```bash
# Xem sản phẩm (Product seed sẵn 3 cái)
curl http://localhost:8081/products
curl http://localhost:8081/products/1

# Đặt hàng một SKU (API tương thích)
curl -X POST http://localhost:8082/orders \
  -H "Idempotency-Key: postman-order-001" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# Đặt order nhiều sản phẩm: toàn bộ reservation thành công hoặc rollback
curl -X POST http://localhost:8082/orders/batch \
  -H "Idempotency-Key: postman-order-batch-001" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}'

# Xem đơn
curl http://localhost:8082/orders

# Response sản phẩm phân biệt onHand, reserved và available
curl http://localhost:8081/products/1
```

Test trực tiếp lifecycle Inventory (thay UUID trong hai lệnh cuối bằng
`reservationId` nhận từ lệnh hold):

```bash
curl -X POST http://localhost:8081/products/1/reservations \
  -H "Idempotency-Key: inventory-hold-001" \
  -H "X-Caller-Id: postman" \
  -H "Content-Type: application/json" \
  -d '{"quantity":2}'

curl http://localhost:8081/reservations/00000000-0000-0000-0000-000000000000
curl -X POST http://localhost:8081/reservations/00000000-0000-0000-0000-000000000000/confirm
# Hoặc release một reservation HELD:
curl -X DELETE http://localhost:8081/reservations/00000000-0000-0000-0000-000000000000
```

TTL mặc định là 15 phút. Milestone hiện tại chưa tự chuyển trạng thái Order khi
reservation hết hạn; phần order state machine thuộc Milestone 3.

## Học được gì
- Service gọi service **đồng bộ** qua OpenFeign
- **DB per service** — Order không đụng DB Product, phải hỏi qua API
- **Reserve, đừng read-then-check** — bất biến phải ép ở nơi sở hữu dữ liệu
- **Idempotency** — cùng key/payload trả cùng order/reservation; payload khác nhận 409
- **Reservation lifecycle** — HELD -> CONFIRMED/RELEASED/EXPIRED, có TTL
- **Tồn kho rõ nghĩa** — available = onHand - reserved; hold không phải ISSUE
- **Saga + bồi hoàn** — không có distributed transaction, phải tự trả hàng về kho
- **Pessimistic lock** (`SELECT ... FOR UPDATE`) cho 1 dòng nóng như counter tồn kho.
  Optimistic (`@Version`) sẽ khiến 19/20 request thua cuộc phải retry — không oversell,
  nhưng đầy 409 vô cớ.
- Khi Product chết -> Order gọi lỗi (Milestone 5 sẽ thêm Circuit Breaker)

## Còn thiếu (cố ý, để dành milestone sau)

- Testcontainers PostgreSQL và contract test cho OpenFeign.
- Order hiện chỉ có một product; multi-SKU atomic hold thuộc Milestone 3.
- Order lifecycle chưa có endpoint confirm/cancel; Inventory đã có command confirm.

## Schema: Flyway, không phải `ddl-auto`

Các file `db/migration/V*.sql` là nguồn sự thật. Hibernate chạy `validate` — đối chiếu, không viết DDL.

Migration chạy **cả trên H2** (profile `dev` và `test`), không riêng Postgres, nên `V*.sql` sai
sẽ làm đỏ `./mvnw test` chứ không đỏ lúc deploy. Đổi lại, SQL phải portable
(`GENERATED BY DEFAULT AS IDENTITY`, không `BIGSERIAL`).

Lưu ý `validate` không so `length`/`precision`/`NOT NULL` — chỉ so tên và kiểu cơ sở. Vì vậy
entity khai báo `@Column(nullable, length, precision, scale)` tường minh, khớp từng chữ với
`V1__init.sql`. Bỏ annotation đi là schema lệch âm thầm.

Nếu đã từng `docker compose up` với `ddl-auto: update`, DB cũ có bảng nhưng không có bảng lịch
sử của Flyway → Flyway sẽ chết. Xoá volume rồi chạy lại (mất data, seeder tạo lại):

```bash
docker compose down -v
```

## Chạy local, KHÔNG cần Docker (profile `dev`)

Chỉ cần JDK 21. Profile `dev` dùng H2 in-memory thay Postgres.
Không cần cài Maven — `mvnw` tự tải.

```bash
# Terminal 1
cd product-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # -> 8081

# Terminal 2
cd order-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev      # -> 8082
```
Windows: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`

Data biến mất khi tắt process — đúng ý đồ của `dev`.

## Chạy local với Postgres thật

Cần 2 Postgres (5432 `productdb`, 5433 `orderdb`) — dễ nhất là `docker compose up -d postgres-product postgres-order`. Rồi:

```bash
cd product-service && ./mvnw spring-boot:run    # -> 8081
cd order-service   && ./mvnw spring-boot:run    # -> 8082
```

Lệnh hay dùng khác:

| Lệnh | Làm gì |
|---|---|
| `./mvnw test` | Chạy test (domain test không cần DB) |
| `./mvnw clean package` | Build ra `target/*.jar` |
| `./mvnw spring-boot:run` | Chạy service |
| `java -jar target/*.jar` | Chạy jar đã build |

## API docs (Swagger)

Mỗi service tự sinh spec khi chạy:

- http://localhost:8081/swagger-ui.html — product-service
- http://localhost:8082/swagger-ui.html — order-service
- `/v3/api-docs` — spec JSON thô

## Test

```bash
./mvnw test
```
- `domain/model/*Test` — POJO thuần, không Spring, không DB. ~0.1s.
- `adapter/in/web/*Test` — nạp context thật trên H2. ~10-20s.

Chênh lệch 100 lần đó là lý do đẩy luật miền vào `domain/`.

## Tiếp theo

Correctness trước infra — Eureka/Gateway chỉ là config, còn nợ reservation đã làm mất hàng thật.

Xem [bản đồ tài liệu](docs/README.md), gồm định hướng sản phẩm, hiện trạng,
roadmap, domain rules, kiến trúc và ADR.

1. ~~Root build + CI~~ — xong
2. ~~Reservation UUID + TTL + idempotency + balance tách nghĩa~~ — xong
3. Testcontainers PostgreSQL + WireMock/contract test
4. Multi-item order lifecycle
5. Kafka + outbox
6. Resilience4j + observability
