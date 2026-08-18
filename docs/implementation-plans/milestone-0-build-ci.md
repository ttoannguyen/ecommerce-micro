# Milestone 0: Root build và CI

- Status: completed
- Updated: 2026-08-18
- Scope: build hygiene, reproducible verification, branch governance và CI

## Outcome

Một checkout mới của repository có thể build và test cả hai service từ root bằng
một lệnh. CI dùng cùng lệnh đó, sau đó xác nhận Docker image của hai service build
được.

Lệnh chuẩn:

~~~bash
./mvnw verify
docker compose build product-service order-service
~~~

## Vì sao làm trước

Reservation là thay đổi domain và schema lớn. Nếu repository chưa có root build
và CI, lỗi migration, integration hoặc dependency có thể chỉ xuất hiện trên máy
khác. Milestone này tạo safety net trước khi bắt đầu Reservation + TTL +
idempotency.

## Hiện trạng trước milestone

- Order và Product là hai Maven project độc lập.
- Mỗi service có Maven Wrapper 3.3.4 và Maven 3.9.16.
- Hai bộ wrapper có checksum giống nhau.
- Root repository chưa có pom.xml hoặc Maven Wrapper.
- Chưa có workflow trong .github/workflows.
- Remote default branch và local branch đều là main; master không được sử dụng.
- Cả hai POM khai báo flyway-database-postgresql hai lần.
- Dockerfile build artifact với skipTests.
- Integration test khởi động Spring context trên H2 nên Flyway migration được
  chạy trong verify.
- Runtime yêu cầu Java 21.

## Phạm vi

### 1. Root Maven reactor

Thêm root pom.xml với:

- packaging là pom;
- module order-service;
- module product-service;
- tên/artifact rõ ràng cho reactor.

Root POM chỉ làm aggregator trong milestone này. Không chuyển dependency hoặc
plugin management từ service POM lên root, vì việc đó mở rộng phạm vi và có thể
làm thay đổi dependency resolution.

### 2. Root Maven Wrapper

Thêm tại root:

- mvnw;
- mvnw.cmd;
- .mvn/wrapper/maven-wrapper.properties.

Dùng cùng wrapper version và distribution đang có trong hai service. Giữ wrapper
của từng service để các lệnh standalone và tài liệu cũ tiếp tục hoạt động.

Root mvnw trên Unix phải có executable bit.

### 3. Dependency hygiene

Trong mỗi service POM, chỉ giữ một dependency:

~~~text
org.flywaydb:flyway-database-postgresql
~~~

Không nâng Spring Boot, Spring Cloud, springdoc hoặc Maven trong milestone này.

### 4. GitHub Actions

Thêm workflow quality cho pull request vào main và push trên main.

Job bắt buộc:

1. Checkout source.
2. Setup Temurin Java 21 với Maven cache.
3. Chạy ./mvnw -B verify từ root.
4. Chạy git diff --check.
5. Chạy docker compose config.
6. Build product-service và order-service bằng Docker Compose.

Maven verify phải chạy trước Docker build. Dockerfile có thể tiếp tục skip test
vì artifact đã được xác nhận ở bước reactor; không dùng Docker build để thay thế
test.

CI không khởi động service/database trong milestone này. PostgreSQL
Testcontainers và end-to-end Compose thuộc Milestone 2.

### 5. Branch governance

- main là nhánh dài hạn duy nhất.
- Không dùng hoặc tạo master/develop.
- Tài liệu hóa branch naming, pull request và merge policy.
- Workflow dùng main trong trigger; không tham chiếu master.
- Sau khi workflow chạy thành công trên GitHub, cấu hình quality làm required
  status check trong GitHub ruleset của main.
- Chặn force push và xóa main; yêu cầu PR và resolved conversation.

GitHub ruleset là repository setting bên ngoài source. Agent không được tuyên bố
đã enforce nếu chỉ mới viết tài liệu hoặc workflow.

### 6. Developer documentation

Cập nhật README:

- bỏ nhãn “Tuần 1” đã lỗi thời;
- thêm prerequisites Java 21 và Docker;
- thêm mục chạy toàn bộ test từ root;
- giữ hướng dẫn chạy từng service;
- giải thích verify chạy test cả hai module;
- thêm lệnh Docker build/Compose hiện tại.

Cập nhật tài liệu:

- product/current-state.md: ghi nhận root build và CI sau khi hoàn thành;
- product/current-state.md: xóa nợ duplicate Flyway và “chưa có CI”;
- product/roadmap.md: chuyển Milestone 0 khỏi phần chưa làm hoặc đánh dấu hoàn
  thành theo quy ước tài liệu;
- implementation plan này: Status thành completed và ghi evidence thực tế.
- development/branching-strategy.md phản ánh policy được chọn.

Không ghi “CI xanh” nếu workflow chưa thực sự chạy trên GitHub. Trong trường hợp
chỉ kiểm tra local, ghi rõ workflow đã được tạo và local verification đã pass.

## Ngoài phạm vi

- Reservation, TTL và idempotency.
- Thay đổi domain hoặc API.
- Kafka, Gateway, Eureka hoặc observability stack.
- Testcontainers/PostgreSQL integration test.
- Refactor service POM thành shared parent.
- Nâng dependency version.
- Xóa wrapper ở từng service.
- Deploy hoặc publish image.

## Kế hoạch thực hiện

### Task 0.1 — Baseline

- Kiểm tra git status và giữ nguyên thay đổi không liên quan.
- Ghi lại version wrapper/JDK yêu cầu.
- Chạy test baseline nếu môi trường có Java 21.
- Nếu không có JDK, ghi rõ giới hạn; CI vẫn phải được cấu hình Java 21.

### Task 0.2 — Sửa POM

- Xóa dependency Flyway lặp ở order-service.
- Xóa dependency Flyway lặp ở product-service.
- Chạy parser/build để bảo đảm POM hợp lệ.

### Task 0.3 — Tạo root reactor

- Tạo pom.xml packaging pom.
- Khai báo hai module.
- Thêm root Maven Wrapper.
- Kiểm tra reactor nhận đủ hai module.

### Task 0.4 — Thêm workflow

- Tạo .github/workflows/quality.yml.
- Pin major version ổn định của checkout/setup-java action.
- Dùng cache Maven từ setup-java.
- Chạy root verify và Docker build.
- Không thêm secret hoặc quyền ghi không cần thiết.

### Task 0.5 — Branch policy

- Xác nhận current/default branch là main.
- Bảo đảm source và workflow không tham chiếu master.
- Rà quy tắc tại development/branching-strategy.md.
- Ghi rõ GitHub ruleset nào cần cấu hình thủ công.

### Task 0.6 — Cập nhật tài liệu

- README dùng root command làm đường chạy chính.
- Current state và roadmap phản ánh code thật.
- Plan ghi evidence và giới hạn xác minh.

### Task 0.7 — Verification

Chạy tối thiểu:

~~~bash
./mvnw -B verify
docker compose config
docker compose build product-service order-service
git diff --check
git status --short
git branch --show-current
~~~

Nếu Docker hoặc network không khả dụng, không bỏ qua im lặng. Ghi command, lỗi và
phần nào vẫn được xác minh.

## Test và evidence

| Evidence | Chứng minh |
|---|---|
| Maven reactor summary có hai module SUCCESS | Root build gom đúng service |
| Test report của mỗi module | Unit/context test được chạy |
| Flyway log trên test profile | Migration tạo schema H2 từ trống |
| docker compose config thành công | Compose syntax và reference hợp lệ |
| Hai Docker image build thành công | Dockerfile tạo runtime artifact |
| Workflow YAML | GitHub có thể tái chạy cùng quality gate |
| git diff --check sạch | Không có whitespace error |

## Verification evidence

Đã xác minh local trên nhánh `main`:

- `./mvnw -B verify` chạy với Temurin JDK 21: reactor có `order-service`,
  `product-service` và root aggregator đều `SUCCESS`.
- Order có 13 test pass; Product có 14 test pass.
- Log test xác nhận Flyway tạo schema H2 từ trạng thái trống và áp dụng đúng
  migration của từng service.
- `docker compose config` thành công.
- `docker compose build product-service order-service` thành công; hai image
  runtime đã được tạo.
- `git diff --check` thành công.
- `.github/workflows/quality.yml` dùng Temurin 21, Maven cache, root verify
  trước Compose config và Docker build, chỉ có quyền `contents: read`.

Workflow chưa được chạy trên GitHub trong phiên này, vì vậy chưa tuyên bố CI
đã xanh và quality chưa được cấu hình thành required check trên repository.

## Known limitations

- GitHub ruleset của `main` (required quality check, PR bắt buộc, chặn force
  push/xóa nhánh và resolved conversation) là repository setting, cần cấu hình
  thủ công sau khi workflow chạy thành công trên GitHub.
## Rủi ro và cách xử lý

### Root POM vô tình trở thành parent

Chỉ dùng modules; không sửa parent của service trong milestone này.

### CI và local dùng lệnh khác nhau

README và workflow đều gọi root ./mvnw verify.

### Docker build bỏ test

Giữ verify thành quality gate bắt buộc trước Docker build.

### Wrapper mất executable bit

Kiểm tra git file mode và chạy root ./mvnw trực tiếp trên Linux.

### Build phụ thuộc cache cục bộ

CI Maven cache ban đầu có thể rỗng; workflow phải tải dependency từ source chính
thống và build được từ clean runner.

## Definition of Done

- [x] Root pom.xml khai báo đúng hai module.
- [x] Root Maven Wrapper chạy được trên Unix.
- [x] Hai POM không còn dependency Flyway trùng.
- [x] Root verify chạy test cả hai service.
- [x] Workflow quality dùng Java 21 và root verify.
- [x] Workflow chỉ dùng main cho push/pull-request target.
- [x] Không tạo hoặc checkout nhánh ngoài policy `main`.
- [x] Branching strategy được liên kết từ docs index.
- [x] Workflow kiểm tra Compose và build hai image.
- [x] README có root build/test command.
- [x] Current-state và roadmap được cập nhật đúng trạng thái.
- [x] git diff --check sạch.
- [x] Không có thay đổi domain/API ngoài phạm vi.
- [x] Evidence và giới hạn verification được ghi trong plan.

## Handoff sang Milestone 1

Sau khi milestone này hoàn tất, thay đổi tiếp theo là
[Reservation lifecycle](../adr/003-reservation-lifecycle.md). Root verify và CI
phải là safety net cho migration, aggregate, expiry worker và idempotency test.
