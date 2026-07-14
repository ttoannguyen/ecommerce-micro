# Handoff — ecommerce-micro

Cập nhật: 2026-07-14. Nhánh `main`, remote `git@github.com:ttoannguyen/ecommerce-micro.git`.

## Đang ở đâu

Dự án học microservice + DDD/Hexagonal. **Tuần 1 xong**, và đã đi xa hơn README gốc:
race condition trong luồng đặt hàng đã được sửa bằng reserve pattern.

- Spring Boot **4.1.0**, Java **21**, Spring Cloud **2025.1.2** (train của Boot 4.x).
- 2 service, DB riêng: `product-service` (8081, productdb) và `order-service` (8082, orderdb).
- 19 test, tất cả xanh. Chạy `./mvnw test` trong từng service.

## Chạy trong 30 giây

```bash
cd product-service && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # 8081
cd order-service   && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # 8082
```
Profile `dev` dùng H2 in-memory → **không cần Docker, không cần Postgres**.
Swagger: `localhost:8081/swagger-ui.html`, `localhost:8082/swagger-ui.html`.

Muốn Postgres thật: cài Docker Desktop → `docker compose up`.

## Quyết định kiến trúc (và vì sao)

### 1. Hexagonal 3 tầng, không phải Clean 4 tầng
`domain/` → `application/` → `adapter/{in,out}`. Phụ thuộc chỉ hướng vào trong.

Chia theo **hướng** (ai gọi mình / mình gọi ai), không theo **công nghệ**. Lý do: microservice
đầy cổng ra — Feign, Kafka producer, cache. Sơ đồ `infrastructure/` + `presentation/` không
có chỗ cho Feign client, và Kafka thì nằm cả hai bên (`in/messaging` lẫn `out/messaging`).

### 2. Bất biến nằm ở nơi sở hữu dữ liệu — đây là quyết định lớn nhất
Code cũ: `order-service` GET stock → check bằng `if` trong Java → save. Đó là **TOCTOU**:
giữa lúc đọc và lúc lưu, request khác đã bán mất hàng.

Code mới: `order-service` gọi `POST /products/{id}/reservations`. `product-service` đọc +
kiểm tra + trừ kho **trong một transaction**, dưới `SELECT ... FOR UPDATE`.

Hệ quả trên code:
- `Order.place()` **không còn** check tồn kho. Nó chỉ tính tiền.
- `LoadProductPort` (query) → `ReserveStockPort` (**command**). Đó là toàn bộ khác biệt.
- `ProductSnapshot` (có field `stock`) → `ReservedProduct` (**không** có `stock`) — để Order
  không bị cám dỗ check lại thứ nó không sở hữu.

`ReserveStockConcurrencyTest` là bằng chứng: 20 thread, 5 hàng → đúng 5 thắng.

### 3. Pessimistic lock, không phải optimistic
Tồn kho là **một dòng nóng**. `@Version` (optimistic) sẽ để 1 thread thắng và 19 thread
ném `OptimisticLockingFailure` → không oversell, nhưng *undersell* + đầy 409 vô cớ.
`SELECT ... FOR UPDATE` bắt chúng xếp hàng thay vì đâm nhau.

`@Version` vẫn giữ trên `ProductJpaEntity` làm lưới an toàn cho các đường update khác.

Cạm bẫy đã dính và đã sửa: `ProductPersistenceAdapter.save()` ban đầu dựng entity **detached**
mới rồi `merge()` — làm vậy là vứt `@Version` đi, lock mất tác dụng im lặng. Phải sửa trên
entity **đang được quản lý**.

### 4. Saga thủ công — và chỗ nó KHÔNG cứu được
`POST /reservations` đã commit ở productdb. `@Transactional` của order **không** rollback nó
được. Nên `PlaceOrderService` bắt exception khi lưu đơn thất bại → gọi `DELETE /reservations`
để trả hàng.

**Nhưng bồi hoàn chỉ cứu được lỗi ở bước `save`.** Nếu chính lời gọi `reserve` fail — timeout,
đứt mạng, lỗi deserialize — thì ta **không biết** product-service đã commit hay chưa:

- Chưa commit → release sẽ **tạo hàng từ không khí**.
- Đã commit → không release thì **mất hàng vĩnh viễn**.

Không `try/catch` nào phân biệt được. Đây là **ambiguous outcome**, bản chất của distributed
system. Hiện tại code chỉ `log.error("Reservation outcome UNKNOWN, stock may be leaked")`
và để người vào dọn.

Đã dính thật: xem mục "Bẫy môi trường" bên dưới — `IllegalAccessError` của Feign proxy làm
`reserve` ném exception **sau khi** kho đã bị trừ. 2 món của product 3 bay mất vĩnh viễn trong
productdb. Đó là lý do phải làm **hold có TTL** trước khi làm bất cứ thứ gì khác.

### 5. `HttpStatus` chỉ sống ở `adapter/in/web`
`ErrorCode` enum giữ `HttpStatus`, và nó nằm ở tầng web. Domain ném exception; `GlobalExceptionHandler`
dịch sang mã HTTP. **Application không được biết 400/409 là gì.**

(So sánh: repo `handmadeshop` để `BaseResponse` mang status code 400/500 vào tận
`application/usecase` — đó là vỡ luật phụ thuộc. Đừng copy.)

### 6. Không Lombok, không MapStruct
- Lombok `@Data`/`@Builder` trên aggregate = mở toang setter = giết bất biến. Java 21 `record`
  đã lo phần VO/DTO.
- MapStruct cần setter hoặc constructor all-args để map **vào** target. Aggregate đúng chuẩn
  thì không có cái nào. Mapper viết tay ~10 dòng, rõ hơn.

## Nợ kỹ thuật đã biết (cố ý để lại)

| Nợ | Vì sao nguy hiểm | Sửa thế nào |
|---|---|---|
| **Hold không có TTL** | order-service chết giữa saga → hàng khoá vĩnh viễn | Bảng `reservation` + `expires_at` + job quét |
| **Không idempotency** | Client bấm 2 lần = 2 đơn | `Idempotency-Key` header + unique index |
| **`ddl-auto: update`** | Hibernate tự đổi schema, không version, không rollback | Flyway `V1__*.sql`, đổi sang `validate` |
| **Không retry / circuit breaker** | Product chết → Order 500 | Resilience4j (tuần 4) |
| **URL product-service hard-code** | | Eureka + Gateway (tuần 2) |
| **Message lỗi tiếng Việt, comment tiếng Anh** | Lệch ngôn ngữ | Chọn một |

## Lộ trình

- **Tuần 2**: Eureka + API Gateway (bỏ hard-code URL)
- **Tuần 3**: Kafka — `OrderPlaced` → Notification. Thêm **outbox pattern** (nếu không, event
  và DB commit không nguyên tử).
- **Tuần 4**: Resilience4j + Zipkin

## Bẫy môi trường

- **Không có Maven global** — dùng `mvnw` (script-only, tự tải Maven 3.9.16).
- **Windows nuốt exec bit**: `mvnw` từng bị commit thành `100644` → `Permission denied` trên
  Linux/mac. Đã fix bằng `git update-index --chmod=+x`. Kiểm tra lại nếu thêm script mới.
- **PowerShell 5.1 đọc `.ps1` theo ANSI** → nát tiếng Việt trong script. Để chuỗi UTF-8 ra
  file JSON riêng, script chỉ dùng ASCII.
- **Boot 4.1 đổi tên starter**: `spring-boot-starter-web` → `spring-boot-starter-webmvc`.
  `spring-boot-starter-test` tách thành `-webmvc-test`, `-data-jpa-test`, `-validation-test`.
- **springdoc 3.0.3 pin Boot 4.0.5**, mình chạy 4.1.0. Compile pass **không** đủ — `OpenApiDocsTest`
  boot context thật để chứng minh nó chạy. Đừng xoá test đó.
- **Wire DTO của Feign PHẢI `public`.** `ReservationResponse` / `ReserveStockRequest` từng là
  record package-private → compile sạch, test xanh, nhưng **chết lúc runtime**:
  ```
  java.lang.IllegalAccessError: failed to access class ...ReservationResponse
  from class jdk.proxy2.$Proxy160
  ```
  Feign sinh JDK dynamic proxy, proxy nằm trong module `jdk.proxy2` — khác runtime package —
  nên không đọc được type package-private. Đừng "dọn dẹp" bỏ `public` đi.

## Lỗ hổng test đã biết

**Không test nào chạm vào Feign client.** Bug `IllegalAccessError` ở trên compile sạch và qua
hết 19 test — chỉ `docker compose up` mới lộ. `OrderValidationTest` chặn request ở biên nên
không bao giờ gọi tới adapter.

Cần một test dựng stub HTTP (WireMock / MockWebServer) cho `ProductClientAdapter`, để đường dây
`ProductClient` → proxy → deserialize được chạy thật trong `mvn test`. Chưa làm.
