# Định hướng sản phẩm

## Kết luận

Dự án phát triển theo mảng **Inventory Management & Order Fulfillment**, tập
trung vào giữ tồn kho chính xác khi nhiều kênh bán cùng đặt hàng.

Tên mô tả phù hợp là **Order & Inventory Platform**. Dự án không cố trở thành
một website thương mại điện tử đầy đủ.

## Bài toán

Một doanh nghiệp có thể bán cùng một SKU qua website, ứng dụng, POS, marketplace
và đơn B2B. Nếu mỗi kênh đọc tồn kho rồi tự quyết định, nhiều request đồng thời
có thể cùng nhìn thấy số lượng cũ và bán vượt tồn.

Hệ thống là nơi quyết định tập trung cho tồn kho:

1. Sales channel tạo yêu cầu đặt hàng.
2. Inventory giữ hàng trong một transaction.
3. Order ghi nhận đơn và điều phối vòng đời.
4. Reservation được confirm khi đơn tiếp tục.
5. Reservation được release hoặc tự hết hạn khi đơn thất bại.
6. Xuất kho vật lý tạo stock movement có thể đối soát.

Hệ thống phải trả lời được:

- Bao nhiêu hàng đang có thực tế, đang được giữ và còn có thể bán?
- Reservation nào đang giữ từng SKU?
- Vì sao tồn kho thay đổi?
- Đơn hàng nào liên quan tới lần giữ hoặc xuất kho?
- Projection có khớp reservation và ledger không?

## Ứng dụng

### Checkout trực tuyến

Giữ SKU trong thời gian khách thanh toán. Thanh toán hoặc xác nhận thành công sẽ
confirm reservation; hủy hoặc timeout sẽ giải phóng phần giữ.

### Bán hàng đa kênh

Website, mobile app, POS và marketplace connector dùng chung một inventory
boundary, tránh mỗi kênh duy trì một số tồn riêng.

### Flash sale

Nhiều request tranh một SKU nóng. Đây là nơi locking, idempotency, backpressure
và load test trở thành yêu cầu nghiệp vụ.

### WMS-lite

Nhân viên kho có thể nhập hàng, xuất hàng, ghi nhận hàng hỏng và điều chỉnh kiểm
kê. Ledger cho phép truy vết thay vì sửa trực tiếp một con số tồn kho.

### Click-and-collect, preorder và B2B

Reservation có TTL hoặc thời hạn được gia hạn theo policy, phù hợp với quy trình
không hoàn tất ngay tại thời điểm tạo order.

### Fulfillment nội bộ

Order tiếp nhận yêu cầu từ nhiều sales channel; Inventory quyết định khả năng đáp
ứng; các consumer xử lý thông báo, đóng gói và tích hợp hệ thống ngoài.

## Người dùng và hệ thống liên quan

| Tác nhân | Mục tiêu |
|---|---|
| Sales channel | Tạo order và nhận kết quả giữ hàng |
| Customer | Xem trạng thái, thanh toán hoặc hủy đơn |
| Warehouse operator | Nhập, xuất, kiểm kê và xử lý fulfillment |
| Operations staff | Theo dõi reservation, outbox và reconciliation |
| Payment provider | Xác nhận hoặc từ chối thanh toán |
| ERP/marketplace/POS | Trao đổi order, catalog hoặc inventory |

## Phạm vi cốt lõi

- Order và order item.
- Inventory balance theo SKU.
- Reservation có TTL và state machine.
- Stock movement ledger.
- Idempotency và reconciliation.
- Đồng bộ liên service có khả năng phục hồi.
- Quan sát được request, event và backlog.

## Ngoài phạm vi trước mắt

- Recommendation engine.
- Full-text product search.
- Promotion/coupon engine.
- Review/rating và CMS.
- Frontend thương mại điện tử phức tạp.
- AI không gắn trực tiếp với bài toán tồn kho.
- Tách nhiều service CRUD chỉ để tăng số container.

## Chỉ dấu sản phẩm thành công

- Không bán vượt available dưới tải đồng thời.
- Request lặp lại không tạo order hoặc reservation trùng.
- Hold bị bỏ quên luôn được expire và giải phóng.
- Mọi thay đổi on-hand đều truy được về movement và chứng từ nguồn.
- Có thể đối soát order, reservation, balance và ledger.
- Lỗi downstream không làm mất dữ liệu hoặc tạo tồn kho từ không khí.

