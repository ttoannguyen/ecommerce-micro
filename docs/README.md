# Tài liệu dự án

Tài liệu trong thư mục này mô tả sản phẩm, domain và các quyết định kiến trúc
bền vững. Trạng thái triển khai được tách khỏi kiến trúc mục tiêu để tránh mô tả
một tính năng chưa có như thể nó đã chạy.

## Bản đồ tài liệu

| Tài liệu | Câu hỏi được trả lời |
|---|---|
| [Định hướng sản phẩm](product/project-direction.md) | Dự án giải quyết bài toán gì và ứng dụng ở đâu? |
| [Hiện trạng](product/current-state.md) | Code hiện có gì, thiếu gì và rủi ro nào đang tồn tại? |
| [Roadmap](product/roadmap.md) | Nên triển khai theo thứ tự nào và khi nào được xem là xong? |
| [Milestone 0 implementation plan](implementation-plans/milestone-0-build-ci.md) | Root build và CI được triển khai, kiểm chứng thế nào? |
| [Milestone 1 implementation plan](implementation-plans/milestone-1-reservation-lifecycle.md) | Reservation lifecycle, idempotency và inventory balance được triển khai thế nào? |
| [Prompt triển khai Milestone 0](prompts/implement-milestone-0.md) | Giao trọn task Milestone 0 cho coding agent |
| [Quy tắc nhánh và pull request](development/branching-strategy.md) | main, branch naming, PR, merge và GitHub ruleset |
| [Kiến trúc](architecture.md) | Service, dữ liệu và transaction được phân ranh giới thế nào? |
| [Domain model](domain/domain-model.md) | Aggregate nào sở hữu dữ liệu và vòng đời nào? |
| [Business rules](domain/business-rules.md) | Những invariant nào backend và database phải bảo vệ? |
| [Bản đồ kiến thức microservice](learning/microservice-knowledge-map.md) | Mỗi concept được áp dụng, làm hỏng và kiểm chứng thế nào? |
| [ADR](adr/) | Vì sao các quyết định kiến trúc quan trọng được chọn? |

## Quy ước

- product/current-state.md là ảnh chụp trạng thái và phải ghi ngày cập nhật.
- product/roadmap.md chỉ chứa công việc chưa làm và tiêu chí nghiệm thu.
- domain/ mô tả ngôn ngữ nghiệp vụ và invariant, không mô tả controller/JPA.
- adr/ là append-only decision log. Quyết định cũ bị thay thế bằng ADR mới,
  không sửa lịch sử để giả vờ quyết định cũ chưa từng tồn tại.
- HANDOFF.md phục vụ bàn giao phiên làm việc; không phải nguồn sự thật lâu dài
  cho product scope hoặc kiến trúc.
