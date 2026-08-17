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
| **`release` không idempotent** | Release theo `(productId, qty)`; gọi 2 lần = tạo hàng từ không khí | Release theo `reservationId`, no-op nếu đã `RELEASED` |
| **Không retry / circuit breaker** | Product chết → Order 500 | Resilience4j (tuần 4) |
| **URL product-service hard-code** | | Eureka + Gateway (tuần 2) |
| **Message lỗi tiếng Việt, comment tiếng Anh** | Lệch ngôn ngữ | Chọn một |

### 7. Flyway sở hữu schema, chạy trên cả H2 lẫn Postgres
`ddl-auto: update` đã bị bỏ. `V1__init.sql` là nguồn sự thật duy nhất; Hibernate chỉ còn
`validate` — nó **chỉ đối chiếu**, không được viết DDL nữa.

Migration chạy cả ở profile `dev` và `test` (H2), không riêng Postgres. Chủ ý: migration nào
chỉ được chạy lúc `docker compose up` là migration không ai test. Giờ `V*.sql` sai cú pháp
làm **đỏ `./mvnw test`**, không phải đỏ lúc deploy. Giá phải trả: SQL phải portable —
`GENERATED BY DEFAULT AS IDENTITY` (dạng ANSI của `BIGSERIAL`), không dùng cú pháp riêng của
Postgres. Khi nào cần cú pháp riêng thì tách thư mục migration theo vendor.

**`validate` yếu hơn bạn tưởng.** Nó so tên bảng + tên cột + kiểu cơ sở. Nó **không** so
`length`, `precision`, `scale`, `NOT NULL`. Nên `varchar(32)` vs `varchar(255)` hay
`numeric(19,2)` vs `numeric(38,2)` trôi qua im lặng. Vì thế `@Column(nullable, length,
precision, scale)` được khai báo **tường minh** trên entity, khớp từng chữ với `V1__init.sql`.
Không có annotation đó, Hibernate mặc định `numeric(38,2)` nullable — lệch mà không ai biết.

Cách kiểm chứng không cần Postgres: bật script-generation với dialect Postgres và
`hibernate.boot.allow_jdbc_metadata_access=false`, cho Hibernate in ra DDL nó **kỳ vọng**,
rồi so tay với `V1__init.sql`. Đã làm lúc viết V1; DDL sinh ra trùng khớp.

### 8. Tồn kho là sổ cái, `product.stock` chỉ là bản chiếu
`stock` từng là một con số tự do — cộng trừ trực tiếp, không ai trả lời được *"vì sao tồn
là 7?"*. Giờ mọi thay đổi đều đi qua `stock_movement`, bảng **chỉ ghi thêm**, và bất biến là:

```
SUM(stock_movement.quantity)  ==  product.stock
```

Hệ quả trên code:
- `Product.create(name, price)` **không nhận stock nữa** — sản phẩm mới bắt đầu từ 0 và phải
  được `receive()`. Nếu tạo được stock từ hư không thì bất biến trên **sai ngay từ dòng đầu**,
  và mọi lần đối soát về sau đều vô nghĩa.
- `reserve()/release()/receive()/adjust()` trả về `StockChange` = (product mới, movement).
  Đi thành cặp có chủ đích.
- `SaveProductPort.apply(StockChange)` — **một** method, không phải hai. Không có chữ ký nào
  cho phép caller ghi bản chiếu mà quên ledger.
- `quantity` mang **dấu**, không phải số dương kèm cờ hướng. Balance là `SUM` thuần, và không
  có cột thứ hai để mâu thuẫn với cột thứ nhất.
- `adjust()` bắt buộc có `ReasonCode`. Kiểm kê lệch được **ghi nhận**, không bị ghi đè nuốt mất.

`ReserveStockConcurrencyTest` giờ khẳng định thêm `balanceOf(id) == stock`. Dưới tranh chấp,
mất một dòng ledger sẽ để lại số tồn trông vẫn đúng nhưng không giải thích được — đúng loại lỗi
mà bảng này sinh ra để lộ.

Nợ mới cố ý: đọc `SUM()` sẽ chậm khi ledger lớn. Hiện bản chiếu được cập nhật cùng transaction
nên chưa đau; khi ledger to thì phải nghĩ tới snapshot theo kỳ.

Chưa có `ref_type`/`ref_id` (chứng từ nguồn) — sẽ thêm ở bước idempotency, khi đã có id đơn hàng
thật để điền. Thêm cột rỗng trước là đoán mò.

## Lộ trình

Thứ tự đã đổi so với bản gốc: **correctness trước infra**. Eureka/Gateway chỉ là config, không
đụng tới model; trong khi nợ reservation đã làm mất hàng thật một lần rồi.

- ~~**#0 Flyway**~~ — xong. `V1__init.sql`, `ddl-auto: validate`.
- ~~**#0.5 Ledger tồn kho**~~ — xong. `V2__stock_movement.sql`, xem mục 8.
  Đây là nền cho hướng mới: dự án đi về **quản lý tồn kho**, không dừng ở ecommerce demo.
- **#1 Reservation thành entity + TTL** — bảng `reservation(id, product_id, quantity, status,
  expires_at, idempotency_key UNIQUE)`, job `@Scheduled` quét `HELD` quá hạn. Đây là thứ biến
  ambiguous outcome ở `PlaceOrderService` thành vô hại.
- **#2 Release theo `reservationId` + `Idempotency-Key`** — mở đường retry an toàn. Retry mà
  không idempotent = oversell có hệ thống.
- **#3 WireMock test cho Feign** — vá lỗ hổng test ở mục dưới.
- **#4 Kafka + outbox** — cần Flyway (#0) và hiểu atomic commit (#1) trước.
- **#5 Eureka + Gateway + Resilience4j + Zipkin** — config, để cuối.

## Bẫy môi trường

- **Không có Maven global** — dùng `mvnw` (script-only, tự tải Maven 3.9.16).
- **Windows nuốt exec bit**: `mvnw` từng bị commit thành `100644` → `Permission denied` trên
  Linux/mac. Đã fix bằng `git update-index --chmod=+x`. Kiểm tra lại nếu thêm script mới.
- **PowerShell 5.1 đọc `.ps1` theo ANSI** → nát tiếng Việt trong script. Để chuỗi UTF-8 ra
  file JSON riêng, script chỉ dùng ASCII.
- **Boot 4.1 đổi tên starter**: `spring-boot-starter-web` → `spring-boot-starter-webmvc`.
  `spring-boot-starter-test` tách thành `-webmvc-test`, `-data-jpa-test`, `-validation-test`.
- **Boot 4 tách auto-configuration ra module riêng.** Thêm `org.flywaydb:flyway-core` trần thì
  **build sạch, chạy im, Flyway không bao giờ chạy** — app khởi động trên schema rỗng rồi chết ở
  `Schema validation: missing table [product]`. Phải dùng `spring-boot-starter-flyway`. Cùng bẫy
  này áp cho mọi tích hợp khác: lấy starter, đừng lấy thư viện trần.
- **Flyway 10+ tách driver theo DB**: `flyway-core` không biết Postgres. Cần thêm
  `flyway-database-postgresql` (scope `runtime`). H2 thì core lo được.
- **DB Postgres cũ có sẵn bảng sẽ làm Flyway chết**: `Found non-empty schema "public" without
  schema history table`. Volume tạo từ thời `ddl-auto: update` chính là trường hợp đó. Cách xử
  lý sạch nhất cho repo học: `docker compose down -v` (xoá sạch data, seeder tạo lại).
  Đừng dùng `baseline-on-migrate=true` — nó **bỏ qua** `V1__init.sql` và giữ nguyên schema cũ
  lệch chuẩn, mà `validate` lại không đủ chặt để phát hiện.
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
