# Prompt cho coding agent: triển khai Milestone 0

Sao chép toàn bộ nội dung bên dưới để giao cho coding agent.

---

Bạn đang làm việc trong repository ecommerce-micro.

## Objective

Triển khai đầy đủ Milestone 0: root Maven reactor, root Maven Wrapper, build
hygiene, GitHub Actions quality gate và cập nhật tài liệu. Hoàn thành công việc,
xác minh trong phạm vi môi trường cho phép và báo cáo evidence cụ thể.

## Required reading

Đọc hoàn chỉnh trước khi sửa:

1. docs/README.md
2. docs/implementation-plans/milestone-0-build-ci.md
3. docs/product/current-state.md
4. docs/product/roadmap.md
5. README.md
6. order-service/pom.xml
7. product-service/pom.xml
8. docker-compose.yml
9. Maven Wrapper properties của cả hai service
10. docs/development/branching-strategy.md

Implementation plan là nguồn yêu cầu chính. Nếu source khác tài liệu, ưu tiên
source hiện tại, ghi lại khác biệt và cập nhật tài liệu tương ứng.

## Required outcome

- Root ./mvnw verify build và test cả order-service lẫn product-service.
- Root có Maven Wrapper cùng version/distribution với wrapper hiện tại.
- Mỗi service chỉ khai báo flyway-database-postgresql một lần.
- Có GitHub Actions workflow dùng Java 21.
- main là nhánh dài hạn duy nhất; không sử dụng master.
- Workflow chạy root Maven verify trước Docker build.
- Workflow kiểm tra docker compose config và build hai application image.
- README mô tả root build/test command.
- Current-state, roadmap và implementation plan phản ánh trạng thái sau triển khai.

## Constraints

- Giữ nguyên Java 21, Spring Boot, Spring Cloud, springdoc và Maven version.
- Root POM chỉ là aggregator; không biến nó thành shared parent trong task này.
- Không thay đổi domain, API, migration hoặc business behavior.
- Không thêm Kafka, Gateway, Eureka, Testcontainers hoặc feature khác.
- Giữ wrapper của từng service.
- Không xóa hoặc ghi đè thay đổi hiện có của người dùng.
- Không commit, push hoặc tạo PR nếu chưa được yêu cầu.
- Không tạo, checkout hoặc tham chiếu master.
- Không tuyên bố CI xanh nếu workflow chưa chạy trên GitHub.
- Dùng apply_patch cho file edits.

## Implementation sequence

1. Kiểm tra current branch là main, kiểm tra git status và đọc required files.
2. Kiểm tra baseline build nếu Java 21 khả dụng.
3. Xóa dependency Flyway bị lặp trong hai service POM.
4. Tạo root pom.xml packaging pom với hai modules.
5. Thêm root mvnw, mvnw.cmd và wrapper properties từ wrapper hiện tại.
6. Bảo đảm root mvnw có executable bit.
7. Thêm .github/workflows/quality.yml.
8. Cập nhật README và các docs được nêu trong implementation plan.
9. Chạy verification.
10. Rà git diff để bảo đảm không có thay đổi ngoài phạm vi.

## CI requirements

Workflow phải chạy cho push trên main và pull_request vào main, có quyền tối
thiểu, và thực hiện:

~~~text
checkout
setup Temurin Java 21 + Maven cache
./mvnw -B verify
git diff --check
docker compose config
docker compose build product-service order-service
~~~

Không dựa vào Maven cài global. Không đặt credential hoặc secret vào workflow.

## Verification

Chạy:

~~~bash
./mvnw -B verify
docker compose config
docker compose build product-service order-service
git diff --check
git status --short
git branch --show-current
~~~

Kiểm tra Maven reactor summary có cả hai module. Kiểm tra test report của từng
module. Nếu một lệnh không chạy được do thiếu JDK, Docker hoặc network:

- ghi chính xác lệnh đã chạy;
- ghi lỗi;
- tiếp tục các kiểm tra không phụ thuộc blocker;
- không đánh dấu requirement đó đã pass.

## Documentation updates

Sau khi code hoàn thành:

- đổi Status của implementation plan từ planned sang completed nếu toàn bộ
  requirement đã hoàn thành;
- tick Definition of Done theo evidence thật;
- ghi mục Verification evidence và Known limitations nếu cần;
- current-state không còn nói duplicate Flyway hoặc CI chưa có;
- roadmap không còn trình bày Milestone 0 như công việc chưa làm;
- README dùng root command làm happy path.
- workflow và tài liệu không tham chiếu master;
- branching policy được giữ nguyên và được liên kết từ docs index.

## Final response

Báo cáo ngắn gọn:

1. Outcome.
2. Files chính đã thay đổi.
3. Verification command và kết quả.
4. Phần chưa xác minh hoặc rủi ro còn lại.
5. Xác nhận không commit/push nếu chưa được yêu cầu.
6. Xác nhận branch hiện tại là main.

Không chỉ mô tả kế hoạch. Hãy thực hiện thay đổi và tiếp tục cho đến khi
Milestone 0 hoàn thành hoặc gặp blocker thực sự không thể xử lý trong phạm vi.

---
