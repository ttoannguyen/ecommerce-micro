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

## Chạy (cần Docker + JDK 17)

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
Cần 2 Postgres chạy sẵn (5432 productdb, 5433 orderdb). Rồi:
```bash
cd product-service && ./mvnw spring-boot:run   # port 8081
cd order-service   && ./mvnw spring-boot:run   # port 8082, DB_PORT=5433
```

## Tuần sau
- Tuần 2: + Eureka + API Gateway (bỏ hard-code URL)
- Tuần 3: + Kafka (event order.created -> Notification Service)
- Tuần 4: + Resilience4j + Zipkin tracing
