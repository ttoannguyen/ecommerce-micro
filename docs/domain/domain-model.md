# Domain model

## Ubiquitous language

| Thuật ngữ | Nghĩa |
|---|---|
| SKU | Đơn vị hàng hóa có thể giữ và xuất kho |
| On hand | Số lượng vật lý hiện có |
| Reserved | Số lượng đang được hold |
| Available | on-hand trừ reserved, lượng có thể hứa bán |
| Reservation | Quyền giữ một lượng SKU trong thời hạn |
| Fulfillment | Xác nhận xuất hàng để thực hiện order |
| Stock movement | Fact bất biến làm thay đổi on-hand |
| Reconciliation | Đối chiếu aggregate, projection và ledger |

Không dùng từ stock đơn lẻ trong model mới khi ý nghĩa có thể là on-hand,
reserved hoặc available.

## Order aggregate

~~~text
Order
├── id
├── idempotencyKey
├── status
├── items[]
│   ├── skuId
│   ├── nameSnapshot
│   ├── unitPriceSnapshot
│   └── quantity
├── reservationId
├── total
├── createdAt
└── version
~~~

Order sở hữu vòng đời đặt hàng và snapshot thương mại. Order không đọc một số
tồn rồi tự kết luận đủ hàng.

~~~text
PENDING -> STOCK_HELD -> CONFIRMED
   |            |
   +----------> REJECTED
                |
                +------> CANCELLED
~~~

## Reservation aggregate

~~~text
Reservation
├── id: UUID
├── orderReference
├── idempotencyKey
├── status: HELD | CONFIRMED | RELEASED | EXPIRED
├── lines[]
│   ├── skuId
│   └── quantity
├── createdAt
├── expiresAt
└── version
~~~

Reservation sở hữu quyền chuyển trạng thái của phần hàng được giữ. Các terminal
command phải idempotent.

Implementation note: Milestone 1 hiện lưu đúng một SKU/quantity trên mỗi
reservation. Aggregate đã có identity và lifecycle; `lines[]` và atomic multi-SKU
hold được mở rộng ở Milestone 3.

~~~text
             confirm
HELD --------------------> CONFIRMED
  |                           terminal
  | release
  +----------------------> RELEASED
  |
  | expires
  +----------------------> EXPIRED
~~~

## Inventory balance

~~~text
InventoryBalance
├── skuId
├── onHand
├── reserved
└── version
~~~

Invariant:

~~~text
onHand >= 0
reserved >= 0
reserved <= onHand
available = onHand - reserved
~~~

InventoryBalance là projection được khóa/cập nhật để quyết định nhanh. Nó không
thay thế ledger và reservation khi audit.

## Stock movement

~~~text
StockMovement
├── id
├── skuId
├── quantity (signed)
├── type
├── referenceType
├── referenceId
├── reasonCode
├── actorId
└── occurredAt
~~~

Movement type tối thiểu:

- RECEIPT;
- ISSUE;
- RETURN;
- DAMAGE;
- ADJUSTMENT;
- REVERSAL.

Completed movement không bị sửa/xóa; sai sót được sửa bằng movement bồi hoàn.

## Aggregate relationship

~~~text
Order 1 ---- 1 Reservation
                  |
                  +---- * ReservationLine ---- 1 SKU

SKU 1 ---- 1 InventoryBalance
SKU 1 ---- * StockMovement
~~~

Quan hệ xuyên service dùng ID/reference, không dùng database foreign key.

## Reconciliation

Hệ thống phải kiểm tra được:

~~~text
InventoryBalance.reserved
  == SUM(quantity của ReservationLine thuộc reservation HELD)

InventoryBalance.onHand
  == opening balance + SUM(StockMovement.quantity)
~~~

Sai lệch phải tạo cảnh báo và cần quy trình sửa bằng adjustment/reversal có audit.
