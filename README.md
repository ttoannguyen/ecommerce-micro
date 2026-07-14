# ecommerce-micro — Tuần 1

Microservice học tập. 2 service, DB riêng mỗi service, gọi nhau qua REST (OpenFeign).

```
Client ──POST /orders──► Order Service ──GET /products/{id} (Feign)──► Product Service
   (8082)                    │                                            │  (8081)
                          orderdb (Postgres 5433)                     productdb (Postgres 5432)
```

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
- Khi Product chết -> Order gọi lỗi (tuần 4 sẽ thêm Circuit Breaker)

## Chạy local (không Docker)

Cần JDK 21 + 2 Postgres chạy sẵn (5432 `productdb`, 5433 `orderdb`).
Không cần cài Maven — `mvnw` tự tải.

```bash
# Terminal 1
cd product-service
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run   -> 8081

# Terminal 2
cd order-service
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run   -> 8082
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
