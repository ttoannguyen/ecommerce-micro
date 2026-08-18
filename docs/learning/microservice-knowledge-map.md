# Bản đồ kiến thức microservice

## Mục tiêu

Dự án không chỉ thêm công nghệ hoặc tính năng. Mỗi phần triển khai phải chứng
minh được:

1. nguyên lý microservice liên quan;
2. failure mode thực tế;
3. cách thiết kế xử lý;
4. bằng chứng bằng test, metric hoặc demo tái tạo được.

Không có một “chuẩn microservice” duy nhất. Nền tảng cần học là service boundary,
data ownership, distributed consistency, communication, reliability,
observability, security, testing và deployment.

## Khung học cho mỗi milestone

Mỗi milestone hoặc feature phải trả lời bốn câu hỏi:

| Phần | Câu hỏi |
|---|---|
| Concept | Nguyên lý nào đang được áp dụng? |
| Failure scenario | Hệ thống thực tế hỏng như thế nào nếu thiếu nó? |
| Implementation | Dự án xử lý bằng model, transaction và protocol nào? |
| Evidence | Test, log, trace hoặc metric nào chứng minh nó hoạt động? |

Ví dụ:

~~~text
Concept: Idempotency
Failure: Client timeout rồi gửi lại request
Implementation: Idempotency-Key + unique constraint + stored result
Evidence: Gửi cùng request 10 lần nhưng chỉ tạo một reservation
~~~

## Ma trận kiến thức nền tảng

| Chủ đề | Điều phải hiểu | Dự án thể hiện bằng |
|---|---|---|
| Service decomposition | Tách theo ownership/lifecycle, không theo bảng CRUD | Order, Inventory và Catalog bounded context |
| Data ownership | Chỉ owner được bảo vệ invariant của dữ liệu | Inventory quyết định available; database per service |
| Synchronous contract | HTTP semantics, timeout, error model, compatibility | OpenAPI và Order → Inventory REST |
| Asynchronous contract | Event là fact, delivery có thể lặp | Kafka event envelope và schema version |
| Local transaction | Transaction chỉ atomic trong một database | Inventory hold và Order save có boundary riêng |
| Distributed consistency | Không có rollback xuyên service | Saga, compensation, TTL và reconciliation |
| Idempotency | Retry không được nhân đôi side effect | Idempotency key và stored result |
| Eventual consistency | State giữa service hội tụ theo thời gian | Outbox, consumer và read model |
| Delivery semantics | At-least-once là mặc định thực tế | Inbox/deduplication và replay |
| Resilience | Dependency chậm/lỗi không kéo sập toàn hệ thống | Timeout, retry, circuit breaker, bulkhead |
| Observability | Có thể giải thích trạng thái của request và backlog | Logs, metrics, traces và correlation ID |
| Security | Identity và permission phải được kiểm tra server-side | OAuth2/OIDC, JWT và authorization |
| Testing | Test đúng boundary và failure mode | Unit, PostgreSQL integration, contract, E2E, load |
| Deployment | Version cũ/mới phải cùng tồn tại khi rollout | Health probe, graceful shutdown, compatibility |

## Service decomposition và ownership

### Concept

Microservice được tách theo business capability và ownership. Một service phải
có dữ liệu, invariant và vòng đời đủ độc lập; không tách chỉ vì muốn có thêm
container.

### Failure scenario

- Order đọc stock rồi tự quyết định, dẫn tới TOCTOU và oversell.
- Hai service cùng sửa một bảng, không còn owner rõ ràng.
- Shared database tạo coupling schema và deployment.
- Service quá nhỏ chỉ làm CRUD rồi gọi vòng qua mạng.

### Implementation

- Order sở hữu order lifecycle và commercial snapshot.
- Inventory sở hữu balance, reservation và stock movement.
- Catalog sở hữu mô tả, giá và trạng thái kinh doanh.
- Mỗi service dùng database riêng.
- Quan hệ xuyên service dùng ID và contract, không dùng foreign key xuyên DB.

### Evidence

- Architecture test hoặc review dependency.
- Không có datasource/repository truy cập database service khác.
- Business rule về available chỉ tồn tại trong Inventory.

## HTTP contract và ambiguous outcome

### Concept

Một HTTP call có ba nhóm kết quả:

- business response rõ ràng;
- technical failure trước khi server xử lý;
- kết quả không xác định vì server có thể đã commit nhưng response bị mất.

Timeout không chứng minh operation chưa xảy ra.

### Failure scenario

Inventory commit hold rồi connection reset. Order nhận exception và không biết
có nên retry hay release. Retry mù có thể giữ hai lần; release mù có thể tạo
available không tồn tại.

### Implementation

- Mỗi hold có reservation ID.
- Caller gửi idempotency key.
- Inventory lưu key, request fingerprint và kết quả.
- Caller query/retry bằng identity ổn định.
- TTL là backstop khi caller không thể hoàn tất.
- Error response có code ổn định và correlation ID.

### Evidence

- WireMock/proxy fault đóng connection sau khi upstream commit.
- Retry trả lại cùng reservation.
- Database chỉ có một side effect.
- Trace cho thấy request đầu timeout và request sau phục hồi kết quả.

## Idempotency

### Concept

Idempotency không chỉ là unique key. Hệ thống phải lưu phạm vi key, fingerprint
của payload, trạng thái xử lý và kết quả đủ để replay response.

### Failure scenario

- Client double-click tạo hai order.
- Load balancer retry POST sau connection reset.
- Scheduler chạy lại cùng expiry task.
- Consumer nhận lại event đã xử lý.

### Implementation

- Key unique theo caller + operation.
- Cùng key/cùng payload trả kết quả cũ.
- Cùng key/payload khác trả conflict.
- Concurrent request cùng key chỉ có một request thực thi.
- Retention dài hơn retry window.
- Terminal command confirm/release/expire là idempotent.

### Evidence

- Gửi đồng thời nhiều request cùng key.
- Chỉ một aggregate/side effect được tạo.
- Tất cả response thành công tham chiếu cùng aggregate ID.
- Payload khác với cùng key bị từ chối.

## Saga, compensation và TTL

### Concept

Saga là chuỗi local transaction. Mỗi bước đã commit không thể bị rollback từ
service khác; hệ thống cần command bồi hoàn hoặc cơ chế tự phục hồi.

### Failure scenario

- Inventory hold thành công nhưng Order save thất bại.
- Compensation call thất bại.
- Order process chết sau hold.
- Compensation bị gửi hai lần.

### Implementation

- Order lưu saga/reservation reference.
- Release dùng reservation ID và idempotent.
- Reservation HELD có expiresAt.
- Expiry worker khóa batch an toàn khi chạy nhiều instance.
- Terminal failure được lưu thành trạng thái/audit, không chỉ log.
- Reconciliation tìm saga/reservation bị kẹt.

### Evidence

- Fault injection tại từng điểm giữa các bước.
- Compensation thành công trả balance đúng.
- Compensation thất bại vẫn được TTL phục hồi.
- Nhiều worker không expire cùng reservation hai lần.

## Inventory concurrency

### Concept

Invariant phải được bảo vệ tại nơi sở hữu dữ liệu trong một transaction.
Pessimistic lock, optimistic lock và conditional update có trade-off khác nhau.

### Failure scenario

- Nhiều request cùng đọc available cũ.
- Hot SKU làm request xếp hàng và tăng latency.
- Multi-SKU hold lock theo thứ tự khác nhau gây deadlock.
- Transaction dài làm cạn connection pool.

### Implementation

- Kiểm tra available và tạo hold trong một transaction.
- Lock theo SKU ID ổn định khi giữ nhiều SKU.
- Transaction không gọi network khi đang giữ database lock.
- Cấu hình lock/statement timeout.
- Đo conflict, wait time và pool saturation.

### Evidence

- PostgreSQL Testcontainers, không chỉ H2.
- Concurrent test chứng minh không oversell.
- Multi-SKU test không để partial hold.
- Load test hot SKU ghi nhận throughput và P95/P99.

## Inventory balance, reservation và ledger

### Concept

Ba khái niệm không được dùng lẫn:

~~~text
available = on-hand - reserved
~~~

Reservation là quyền giữ hàng; StockMovement là fact thay đổi lượng vật lý.

### Failure scenario

- Hold bị ghi như ISSUE làm báo cáo tưởng hàng đã rời kho.
- Release tạo movement vật lý giả.
- Projection đúng nhưng ledger thiếu dòng.
- Ledger đúng nhưng reserved projection bị kẹt.

### Implementation

- Hold/release/expire chỉ thay đổi reserved.
- Fulfillment giảm reserved và on-hand, đồng thời ghi ISSUE.
- Receipt/return/damage/adjustment tạo movement tương ứng.
- Movement append-only; sửa sai bằng reversal.
- Reconciliation kiểm tra on-hand với ledger và reserved với HELD reservations.

### Evidence

- Query tái tạo on-hand từ ledger.
- Query tái tạo reserved từ reservation lines.
- Test rollback chứng minh aggregate và projection không lệch.
- Test reversal giữ nguyên lịch sử.

## Transactional outbox

### Concept

Database và Kafka không tham gia cùng local transaction. Gọi save rồi publish
tuần tự luôn có một cửa sổ làm mất event hoặc publish event cho transaction đã
rollback.

### Failure scenario

- Order commit nhưng process chết trước khi publish.
- Kafka publish thành công nhưng DB đánh dấu chưa gửi thất bại.
- Publisher gửi cùng event nhiều lần.

### Implementation

- Aggregate update và outbox insert trong cùng transaction.
- Publisher claim batch, publish và cập nhật trạng thái.
- Event có ID ổn định.
- Publisher retry với backoff.
- Outbox backlog có metric và reconciliation.

### Evidence

- Kill process sau DB commit.
- Khởi động lại vẫn publish event.
- Duplicate publish không tạo duplicate business side effect.
- Dashboard hiển thị outbox pending và publish failures.

## At-least-once delivery và idempotent consumer

### Concept

Consumer có thể xử lý business side effect thành công nhưng chết trước khi commit
offset. Event sẽ được giao lại. Exactly-once broker không tự đảm bảo exactly-once
cho database hoặc external API.

### Failure scenario

- Notification gửi hai lần.
- Inventory issue hai lần.
- Consumer poison message chặn partition.
- Replay event cũ phá state mới.

### Implementation

- Event envelope có eventId, eventType, aggregateId, occurredAt, schemaVersion.
- Inbox/dedup record được ghi cùng transaction với side effect.
- Handler kiểm tra state transition.
- Retry có giới hạn; poison event đi dead-letter topic.
- Có procedure inspect, fix và replay.

### Evidence

- Publish cùng event nhiều lần.
- Chỉ một side effect tồn tại.
- Poison event đi DLQ mà partition tiếp tục xử lý.
- Replay được audit.

## Retry, timeout, circuit breaker và bulkhead

### Concept

Resilience pattern giải quyết các failure khác nhau:

- timeout giới hạn thời gian chờ;
- retry xử lý lỗi tạm thời;
- circuit breaker ngừng gọi dependency đang lỗi;
- bulkhead giới hạn blast radius.

### Failure scenario

- Retry endpoint không idempotent nhân đôi dữ liệu.
- Nhiều instance retry cùng lúc tạo retry storm.
- Dependency chậm làm cạn thread/connection pool.
- Circuit breaker che lỗi business nếu cấu hình sai.

### Implementation

- Connect/read/overall timeout rõ ràng.
- Retry chỉ operation idempotent và lỗi được phân loại retryable.
- Exponential backoff, jitter và retry budget.
- Circuit breaker không tính business rejection là technical failure.
- Bulkhead riêng cho downstream quan trọng.

### Evidence

- Fault injection latency, reset và 5xx.
- Metric retry/circuit state/pool saturation.
- Load test chứng minh dependency lỗi không làm toàn service treo.

## Contract evolution

### Concept

Rolling deployment khiến producer/consumer version cũ và mới chạy cùng lúc.
Contract phải tương thích trong cửa sổ chuyển đổi.

### Failure scenario

- Đổi tên field làm consumer cũ deserialize lỗi.
- Migration xóa cột khi instance cũ còn dùng.
- Event schema mới làm DLQ tăng.

### Implementation

- REST dùng additive change trước breaking change.
- Database migration theo expand → migrate → contract.
- Event có schema version.
- Consumer chịu được field bổ sung và optional hợp lý.
- Contract test chạy trong CI.

### Evidence

- Test consumer cũ với producer payload mới.
- Deploy version cũ/mới đồng thời trong E2E.
- Migration rollback/forward rehearsal.

## Observability

### Concept

Logs trả lời chuyện gì đã xảy ra; metrics cho biết mức độ/tần suất; traces cho
biết request đi qua đâu. Cả ba phải dùng chung identity/correlation.

### Failure scenario

- Có 500 nhưng không biết service nào gây ra.
- Reservation bị kẹt nhưng chỉ phát hiện khi hết hàng.
- Consumer lag tăng mà không có cảnh báo.
- Log nhiều nhưng không liên kết được một order.

### Implementation

- Structured JSON log.
- Correlation ID và W3C trace context xuyên REST/event.
- OpenTelemetry trace.
- Micrometer metrics.
- Dashboard và alert cho error, latency, saturation và backlog.

Metric tối thiểu:

- orders created/rejected;
- reservations held/released/expired;
- reservation conflict và latency;
- outbox pending/publish failures;
- consumer lag/DLQ;
- HTTP error và latency theo dependency.

### Evidence

- Một order có thể truy vết xuyên Order → Inventory → Kafka consumer.
- Dashboard hiển thị RED metrics và backlog.
- Runbook chỉ ra cách điều tra reservation/outbox bị kẹt.

## Security

### Concept

Authentication xác định actor; authorization quyết định actor được làm gì.
Gateway không thay thế authorization tại service sở hữu dữ liệu.

### Failure scenario

- Client tự gửi customer/store ID của người khác.
- Internal endpoint bị gọi từ bên ngoài.
- Token bị ghi vào log.
- Service tin role do client gửi.

### Implementation

- OAuth2/OIDC và JWT hoặc opaque token phù hợp.
- Authorization kiểm tra server-side theo actor/resource.
- Propagate identity tối thiểu cần thiết.
- Secret nằm ngoài source code.
- Audit cho operation nhạy cảm.
- Không log token hoặc dữ liệu nhạy cảm.

### Evidence

- Test horizontal/vertical privilege escalation.
- Token hết hạn/revoke bị từ chối.
- Audit ghi actor, action, resource và reason.

## Testing strategy

| Tầng | Mục tiêu |
|---|---|
| Domain unit | Invariant và state transition |
| Application | Saga orchestration với fake/mock ports |
| Persistence integration | PostgreSQL transaction, lock, migration |
| HTTP integration | Serialization, error mapping, timeout |
| Contract | Compatibility giữa producer/consumer |
| End-to-end | Luồng qua toàn bộ stack |
| Load | Contention, latency, saturation |
| Chaos/fault injection | Crash, timeout, duplicate và dependency failure |

H2 vẫn hữu ích cho test nhanh, nhưng không thay thế PostgreSQL ở test locking,
isolation và vendor-specific migration.

## Deployment và vận hành

### Concept

Microservice chỉ có ý nghĩa khi deploy và vận hành độc lập mà không phá contract
hoặc làm mất work đang xử lý.

### Failure scenario

- Container bị kill khi đang xử lý event.
- Readiness báo healthy trước khi migration/dependency sẵn sàng.
- Rolling update chạy hai schema version không tương thích.
- Consumer nhận work mới trong khi đang shutdown.

### Implementation

- Liveness và readiness tách biệt.
- Graceful shutdown ngừng nhận work mới và hoàn tất work đang giữ.
- Container chạy non-root.
- Config/secret externalized.
- Migration được chạy có kiểm soát.
- Backup/restore và rollback procedure.

### Evidence

- Kill/restart test.
- Rolling deployment E2E.
- Backup restore rehearsal.
- Runbook cho DLQ, outbox backlog và reconciliation.

## Kịch bản bắt buộc phải demo

- Không oversell dưới concurrent load.
- Timeout rồi retry không tạo order/reservation trùng.
- Order save thất bại không giữ hàng vĩnh viễn.
- Process chết giữa saga vẫn tự phục hồi.
- Release/expire lặp chỉ tác động một lần.
- Event bị gửi hai lần nhưng side effect chỉ xảy ra một lần.
- Database commit khi Kafka lỗi không làm mất event.
- Poison event đi DLQ và replay được.
- Contract cũ/mới cùng chạy trong rolling deployment.
- Một order được truy vết xuyên suốt bằng trace ID.
- Dashboard phát hiện backlog, latency và error rate.

## Mức bao phủ theo roadmap

| Milestone | Kiến thức chính |
|---|---|
| 0 | Build reproducibility, CI và migration discipline |
| 1 | Aggregate, idempotency, saga, TTL và reconciliation |
| 2 | Integration, contract, concurrency và fault testing |
| 3 | Transaction boundary, state machine và deadlock avoidance |
| 4 | Eventual consistency, outbox, at-least-once và DLQ |
| 5 | Timeout, retry, circuit breaker, metrics, logs và traces |
| 6 | Security, deployment, performance và operations |

Hoàn thành đến Milestone 5 với evidence tái tạo được là mốc thể hiện đầy đủ phần
cốt lõi của một hệ thống microservice thực tế. Milestone 6 đưa dự án gần hơn tới
production readiness.

