# Quy tắc nhánh và pull request

## Quyết định

main là nhánh dài hạn duy nhất của repository. Không tạo hoặc sử dụng master.

Không thêm develop ở giai đoạn hiện tại. Repository nhỏ với continuous
integration không cần hai nhánh dài hạn có cùng vai trò.

## Vai trò của main

main phải luôn:

- build được;
- qua quality gate;
- có migration tiến về phía trước hợp lệ;
- có tài liệu phản ánh code hiện tại;
- sẵn sàng tạo release hoặc deploy demo.

Không commit trực tiếp vào main cho thay đổi thông thường. Mọi thay đổi đi qua
nhánh ngắn hạn và pull request.

## Tên nhánh

Mẫu:

~~~text
<type>/<short-description>
~~~

| Type | Dùng cho |
|---|---|
| feat | Tính năng hoặc capability mới |
| fix | Sửa bug |
| refactor | Thay đổi cấu trúc không đổi behavior |
| test | Bổ sung hoặc sửa test |
| docs | Chỉ thay đổi tài liệu |
| chore | Build, dependency, tooling hoặc CI |
| perf | Cải thiện performance có đo lường |
| hotfix | Sự cố khẩn cấp trên release/deploy |

Ví dụ:

~~~text
chore/root-build-ci
feat/reservation-lifecycle
test/postgres-reservation-concurrency
fix/idempotent-release
docs/inventory-business-rules
~~~

Tên nhánh dùng chữ thường, dấu gạch ngang và mô tả outcome. Không dùng tên cá
nhân hoặc tên chung như update, changes và work.

## Vòng đời nhánh

1. Cập nhật main từ origin.
2. Tạo nhánh ngắn hạn từ main mới nhất.
3. Giữ PR nhỏ và chỉ có một outcome chính.
4. Push nhánh và mở pull request vào main.
5. Chờ required checks hoàn thành.
6. Giải quyết review và conversation.
7. Rebase lên main nếu branch bị lệch hoặc có conflict.
8. Squash merge vào main.
9. Xóa nhánh sau khi merge.

Không merge main ngược vào feature branch chỉ để né conflict nếu có thể rebase
sạch. Không force-push main.

## Pull request

Mỗi PR phải có:

- vấn đề hoặc outcome;
- phạm vi và phần cố ý không làm;
- quyết định/trade-off quan trọng;
- test đã chạy và kết quả;
- migration/deployment impact nếu có;
- docs được cập nhật khi behavior hoặc kiến trúc thay đổi.

PR không được tự nhận pass nếu command chưa chạy. Nếu môi trường chặn một kiểm
tra, ghi rõ command, lỗi và phần chưa xác minh.

## Merge policy

Mặc định dùng squash merge để main có một commit rõ nghĩa cho mỗi PR.

Commit squash dùng dạng:

~~~text
<type>(<scope>): <outcome>
~~~

Ví dụ:

~~~text
chore(build): add root Maven reactor and quality workflow
feat(inventory): add idempotent reservation lifecycle
test(inventory): verify PostgreSQL locking under contention
~~~

Không merge khi required check đỏ/chưa chạy, còn unresolved conversation,
migration không có forward path, breaking contract chưa có compatibility plan,
hoặc tài liệu khác behavior đã triển khai.

## GitHub ruleset cho main

Repository settings nên cấu hình ruleset áp dụng cho main:

- chặn force push;
- chặn xóa branch;
- yêu cầu pull request trước khi merge;
- yêu cầu required status check của workflow quality;
- yêu cầu branch up-to-date trước khi merge;
- yêu cầu resolve conversation;
- chỉ cho squash merge hoặc rebase merge;
- không cho bypass, trừ tài khoản emergency được kiểm soát.

Với repository cá nhân, có thể không yêu cầu approval để tránh tự chặn chính
mình, nhưng vẫn yêu cầu PR và status checks. Khi có collaborator, yêu cầu tối
thiểu một approval.

Ruleset trên GitHub là enforcement thật; file này chỉ là policy. Khi Milestone 0
hoàn thành, workflow quality phải được chọn làm required check trong repository
settings.

## Release và hotfix

- Release được đánh tag từ main, không tạo release từ feature branch.
- Dùng semantic version tag khi bắt đầu có release ổn định.
- Hotfix vẫn tạo từ main và đi qua PR/quality gate.
- Emergency bypass phải có follow-up PR, lý do và evidence ngay sau đó.

## Agent rule

Coding agent:

- kiểm tra current branch và git status trước khi sửa;
- không làm việc trên master;
- không tự đổi branch nếu người dùng đã chỉ định branch;
- không commit, push, merge hoặc tạo PR nếu chưa được yêu cầu;
- không xóa hoặc ghi đè thay đổi chưa commit của người dùng;
- báo rõ branch và verification trong handoff.

