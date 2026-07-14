# ecommerce-micro — Tuần 1

Microservice học tập. 2 service, DB riêng mỗi service, gọi nhau qua REST (OpenFeign).

```
Client ──POST /orders──► Order Service ──POST /products/{id}/reservations──► Product Service
   (8082)                    │            (Feign)                              │  (8081)
                          orderdb (Postgres 5433)                     productdb (Postgres 5432)
```

## Luật quan trọng nhất: bất biến nằm ở nơi sở hữu dữ liệu

Order **không** hỏi "còn bao nhiêu hàng?" rồi tự quyết. Nó **ra lệnh** "giữ cho tôi 2 cái",
và Product tự quyết trong transaction của mình.

```
SAI (race condition):                    ĐÚNG (reserve):
  Order: GET /products/1  -> stock=1       Order: POST /products/1/reservations {2}
  Order: GET /products/1  -> stock=1                Product: BEGIN
  Order: check ok, save                              SELECT ... FOR UPDATE
  Order: check ok, save   -> BÁN QUÁ HÀNG            stock >= 2 ?  trừ kho
                                                    COMMIT
                                           -> 200 {price}  hoặc  409 INSUFFICIENT_STOCK
```

Giữa lúc Order **đọc** stock và lúc nó **lưu** đơn luôn có khe hở cho request khác chen vào
(TOCTOU). Order không thể tự bịt: nó chỉ cầm một bản sao đã cũ. Chỉ Product — chủ sở hữu
dòng dữ liệu đó — mới bịt được.

`ReserveStockConcurrencyTest` chứng minh: 20 thread cướp 5 món → đúng 5 thắng, 15 nhận 409.

Vì `POST /reservations` đã commit ở productdb, `@Transactional` của Order **không** rollback
được nó. Nên `PlaceOrderService` phải tự **bồi hoàn** (`DELETE /reservations`) khi lưu đơn
thất bại. Đó là saga.

## Kiến trúc (DDD / Hexagonal)

Mỗi service = 1 bounded context, chia 3 tầng theo ports & adapters:

```
com.shop.order
├── domain/                 # POJO thuần, KHÔNG annotation framework
│   ├── model/              # Order (aggregate), Money/Quantity/OrderStatus (VO), ProductSnapshot
│   ├── port/in/            # PlaceOrderUseCase, FindOrdersUseCase, PlaceOrderCommand
│   └── port/out/           # LoadProductPort, SaveOrderPort, LoadOrderPort
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

## Chạy (cần Docker + JDK 21)

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

# Đặt hàng -> Order gọi Product qua Feign, check tồn kho, tính tiền
curl -X POST http://localhost:8082/orders \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# Xem đơn
curl http://localhost:8082/orders
```

## Học được gì
- Service gọi service **đồng bộ** qua OpenFeign
- **DB per service** — Order không đụng DB Product, phải hỏi qua API
- **Reserve, đừng read-then-check** — bất biến phải ép ở nơi sở hữu dữ liệu
- **Saga + bồi hoàn** — không có distributed transaction, phải tự trả hàng về kho
- **Pessimistic lock** (`SELECT ... FOR UPDATE`) cho 1 dòng nóng như counter tồn kho.
  Optimistic (`@Version`) sẽ khiến 19/20 request thua cuộc phải retry — không oversell,
  nhưng đầy 409 vô cớ.
- Khi Product chết -> Order gọi lỗi (tuần 4 sẽ thêm Circuit Breaker)

## Còn thiếu (cố ý, để dành các tuần sau)
- Hold có **TTL** — hiện `reserve` giữ hàng vĩnh viễn. Nếu order-service chết đúng
  giữa saga, hàng bị khoá mãi. Cần job quét hold quá hạn.
- **Idempotency key** — client bấm 2 lần = 2 đơn.
- **Flyway** — `ddl-auto: update` vẫn đang tự đổi schema. Không dùng được ở production.

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

## Tuần sau
- Tuần 2: + Eureka + API Gateway (bỏ hard-code URL)
- Tuần 3: + Kafka (event order.created -> Notification Service)
- Tuần 4: + Resilience4j + Zipkin tracing
