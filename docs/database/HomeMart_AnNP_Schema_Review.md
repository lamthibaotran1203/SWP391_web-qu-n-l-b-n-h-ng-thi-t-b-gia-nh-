> Update 2026-10-05: this is the original diagram review. The user subsequently approved
> the foundation decisions, which were recorded in Backlog and deployed through Flyway V4.
> See [current deployment and remaining service/RDS work](HomeMart_Schema_Deployment_2026-10-05.md).

# Đánh giá schema HomeMart cho các UC của AnNP

Ngày kiểm tra: 05/10/2026.

**Kết luận:** SQL chuyển từ sơ đồ chạy được trên PostgreSQL 17, nhưng chưa đủ điều kiện làm migration nghiệp vụ chính thức. Phạm vi AnNP do người dùng xác nhận ngày 05/10/2026 là **UC-04, UC-05, UC-13, UC-15, UC-32, UC-33, UC-34**. UC-05 có nền tảng lưu credential; UC-04/13 cần hoàn thiện invariant; UC-15/32/33 cần dữ liệu bằng chứng và transaction; UC-34 phụ thuộc payment/refund ledger đáng tin cậy.

## Nguồn và phạm vi

- [Backlog chuẩn](https://docs.google.com/spreadsheets/d/1iCXEHbgqBN1cf5sfLmjPiv0GJnYjauJqVg-ZxIxJKCY/edit), xuất đọc đủ Backlog và Iter1–Iter4 ngày kiểm tra. Bản đọc iteration cũ khác danh sách người dùng vừa xác nhận; danh sách xác nhận là phạm vi review hiện tại. Chưa sửa Sheet để đồng bộ phân công. Cột Lead / Reviewer của bảy UC này trên tab Backlog vẫn là Unassigned; đây là thiếu đồng bộ trong Sheet.
- [Live RDS](https://docs.google.com/document/d/1Qc5Kk2D8MX09AV_7Z4iEg2fsXBElREgomhF-0MBxrDA/edit), xuất Markdown ngày kiểm tra. Đây là bản đang chỉnh sửa, không xác nhận là revision đã nộp.
- File XML người dùng cung cấp: `mxGraphModel dx=3973 dy=2150 grid=1.txt`. Trích đủ 23 table boxes, 277 cột, 25 cạnh FK; suy ra thêm FK tự tham chiếu của category từ marker `FK? parent_id`.
- `docs/HomeMart_RDS_Consolidated_Review.docx`: hướng dẫn review và các đề xuất đã ghi trước đây, không phải quyết định đã duyệt.
- `backend/src/main/resources/db/migration/V1__create_app_schema_and_identity.sql`, entity `UserAccount`, enum `AccountRole` và cấu hình security hiện tại.

Đây là review và chuyển đổi tài liệu. Không thay đổi Sheet, RDS, sơ đồ gốc, migration đã áp dụng, business code hoặc Supabase. Các sửa thiết kế đề xuất phải được chốt trong Backlog trước khi triển khai.

## Phạm vi AnNP đã được người dùng xác nhận

| UC | Tên theo Backlog | Đánh giá dữ liệu |
| --- | --- | --- |
| UC-04 | Manage Personal Profile & Addresses | Chưa đảm bảo đúng một default còn hiệu lực |
| UC-05 | Change Own Password | Có chỗ lưu hash; phải dùng đúng account đang login |
| UC-13 | Manage Products and SKUs | Có product/SKU, thiếu chốt image/barcode/publication và stale update |
| UC-15 | Process Online Payment | Có attempt/idempotency; thiếu bằng chứng provider/callback đầy đủ |
| UC-32 | Review Return Request | Thiếu liên kết order line, inspection và decision chi tiết |
| UC-33 | Record Refund | Có refund cơ bản; thiếu bảo đảm đúng payment/balance và xử lý retry |
| UC-34 | View Financial Reports | Có nguồn tiền vào/ra; cần status/time semantics và ledger đáng tin cậy |

Người dùng xác nhận danh sách trên sau bản review đầu tiên. Bản đọc Iter1–Iter4 ngày 05/10 ghi UC-10/11/04/05/15/16/32 cho AnNP, UC-13 cho Nhã, UC-33 cho Trân và UC-34 cho Chuẩn. Đây là discrepancy cần đồng bộ lại trong Backlog; không dùng phân công iteration cũ để thay danh sách người dùng xác nhận. UC-10/11/16 được đưa ra ngoài phạm vi AnNP của review này. Không dùng Created By trong RDS để suy ra ownership.

## Điểm phải chốt chung trước migration

1. **Một nguồn identity.** Backend login hiện đọc `app.user_account(user_id, password_hash, role, status, version)`; sơ đồ có `hm_user`, `hm_role`, `hm_userrole` và mọi FK user trỏ `hm_user.id`. Tạo cả hai mô hình sẽ không làm account đăng nhập tự trở thành owner của cart/address/order. Khuyến nghị giữ baseline đang chạy và ánh xạ các bảng nghiệp vụ vào nó, trừ khi nhóm quyết định thay mô hình qua migration mới. Chốt cả Admin trong tài liệu so với MANAGER trong code.
2. **Thời gian.** Bản chuyển đổi giữ `datetime2(6)` thành `timestamp(6) without time zone` để giữ ý nghĩa kiểu gốc. Baseline dùng Instant/timestamptz. Trước tích hợp, chốt timestamp sự kiện theo timestamptz và múi giờ nghiệp vụ cho timeout/return window; không tự diễn giải thời gian cũ.
3. **Concurrency.** Không có version trong sơ đồ. Điều này không tự làm schema sai: có thể dùng row lock trong transaction. Nhóm phải chốt một chiến lược và lock order thống nhất cho payment, timeout, return và inventory; không yêu cầu vừa version vừa locking nếu không cần.
4. **Bằng chứng nghiệp vụ.** Audit chung `modified_by/date` chỉ giữ lần sửa cuối. Nó không thay lịch sử order hoặc tách người kiểm tra hàng khỏi người ra quyết định return. Principal audit phải ổn định và do server lấy từ session; FK account hay principal string là lựa chọn cần thống nhất.

## Đánh giá từng UC

### UC-04 Manage Personal Profile & Addresses

Các cột profile và địa chỉ bao phủ dữ liệu cơ bản. `hm_shipment` lưu bản chụp recipient/address độc lập, thuận lợi để đổi address book mà không sửa địa chỉ đơn đã đặt.

Partial unique index hiện tại chỉ bảo đảm **tối đa một** default, không bảo đảm **ít nhất một** khi còn địa chỉ. Đã xác nhận có thể bỏ default của toàn bộ địa chỉ. Đồng thời index vẫn tính địa chỉ `is_deleted = true`; default đã xóa mềm có thể chặn default mới.

Đề xuất index `WHERE is_default AND NOT is_deleted` nếu nhóm chốt quy tắc địa chỉ còn hiệu lực. Service transaction phải quản lý first-address default, chuyển default, replacement khi xóa default và rollback nếu thiếu replacement. Lock account hoặc tập địa chỉ của customer theo cách thống nhất để chống hai request đồng thời. Ownership và kiểm tra phone/required fields thuộc service/validation.

### UC-05 Change Own Password

Không cần bảng riêng. `hm_user.password` có thể chứa BCrypt hash về mặt dung lượng, nhưng backend thực tế đọc `user_account.password_hash`. Cần sửa đúng nguồn credential và không trả hash qua DTO.

Current-password verification, password policy, confirmation và việc khác password cũ thuộc service, không phải SQL. Mọi failure phải giữ nguyên hash. Nếu dùng `UserAccount` hiện tại, đã có version để kiểm soát stale write; tránh concurrent profile update ghi đè credential. Chính sách giữ/hủy các session khác sau đổi password cần được nhóm chốt nếu muốn đưa vào phạm vi.

### UC-13 Manage Products and SKUs

`hm_product` và `hm_sku` có quan hệ parent, unique product/SKU code, unique barcode khi có giá trị, giá numeric(18,0), CHECK giá/threshold không âm, manual attributes và trạng thái/xóa mềm. Phù hợp nền tảng quản lý SKU nhập tay; không cần variant engine tự sinh tổ hợp.

Những điểm cần chốt trước triển khai:

- Live RDS yêu cầu mỗi SKU có barcode; sơ đồ cho barcode NULL. UNIQUE của PostgreSQL cho phép nhiều NULL. Cần chốt barcode bắt buộc ngay khi tạo hay chỉ lúc publication, rồi thêm validation/nullability phù hợp.
- Product không có trường representative image riêng; image_url nằm trên SKU và được phép NULL. Có thể dùng ảnh SKU làm ảnh đại diện nếu nhóm chốt rõ, hoặc thêm ảnh product. Service phải chặn ACTIVE/publication khi thiếu ảnh, category/brand hợp lệ hoặc SKU đủ điều kiện. FK chỉ bảo đảm category/brand tồn tại, không kiểm tra deactivated.
- category_id/brand_id NOT NULL trên mọi product, trong khi UC có tình huống giữ bản chưa published khi thiếu publication data. Chốt có cho lưu draft thiếu category/brand hay chỉ giữ form chưa lưu; không tự thay nullability.
- `attributes_json` hiện là text. Nếu UI gửi cấu trúc JSON, phải validate shape tối thiểu; jsonb là lựa chọn sau khi chốt hợp đồng, không tự thêm generic attribute engine.
- Có ngoại lệ stale catalogue record nhưng chưa có version/chiến lược chống ghi đè. Chọn optimistic version hoặc một cơ chế phát hiện stale tương đương; row lock đơn thuần sau khi nhận payload cũ chưa tự phát hiện stale edit.
- Product/SKU save phải atomic và không đổi physical/reserved stock. Khởi tạo inventory row zero nếu hợp đồng chung cần là shared prerequisite, không thay UC receiving/adjustment.
- Đổi giá hiện tại không viết lại unit_price đã chốt trong order_item. Deactivation phải giữ FK lịch sử. Giá whole VND, VAT-inclusive và cảnh báo threshold cần validation/service chung với owner inventory.

Không có bảng product_image riêng không tự là lỗi nếu scope chỉ cần một ảnh; thiếu publication guard mới là điểm cần xử lý. Test ưu tiên: duplicate barcode/code, giá/threshold âm, publish thiếu ảnh, category inactive, stale edit, rollback product/SKU khi một line lỗi.

### UC-15 Process Online Payment

Điểm tốt: order có reservation_expiry; payment cho nhiều attempt trên một order; transaction_code và idempotency_key unique; amount numeric(18,0) với CHECK không âm. Đã xác nhận duplicate idempotency bị chặn.

Thiếu hoặc chưa rõ:

- Chưa có trường provider rõ ràng như live RDS yêu cầu; payment_method không đồng nghĩa provider. Chốt transaction_code là mã attempt nội bộ hay mã gateway. Nếu là mã gateway, NOT NULL ngay khi tạo attempt sẽ không phù hợp với provider chỉ trả mã sau đó.
- Một callback_payload không giữ được đầy đủ nhiều callback hợp lệ/lặp/sai chữ ký và lý do reconciliation của late success. Cần một cách lưu audit được chốt; có thể dùng event record nhỏ, không cần hệ thống event tổng quát. Payload phải được lọc dữ liệu nhạy cảm trước lưu.
- Unique idempotency không tự bảo đảm state transition một lần. Callback, timeout và cancellation phải lock/recheck order, payment, reservation rồi ghi trong transaction. Callback đến muộn không mở lại đơn đã hủy.
- Chưa chốt status values và cơ chế immutable transaction history; không suy ra từ varchar(50).

PROJECT_STATUS trước review ghi online payment đang deferred, chưa chọn provider. Backlog vẫn giao UC-15 cho AnNP. Cần quyết định lịch triển khai/provider trong Backlog; không hiểu deferred là tự xóa UC khỏi scope.

### UC-32 Review Return Request

Đây là phần thiếu nhiều nhất. `hm_return_item` chỉ FK tới SKU, không tới `hm_order_item`. Đã xác nhận một request cho order mua SKU A có thể ghi return SKU B với quantity 999. FK hiện tại không chứng minh hàng đã mua, giá tại thời điểm mua hoặc giới hạn trả cộng dồn.

Đề xuất tối thiểu cần chốt:

- Link return line → original order_item. Service phải xác nhận order_item thuộc order của request; một FK đơn tới order_item vẫn chưa bảo đảm cùng order. Quy tắc nhiều request cùng line cần kiểm tra quantity cộng dồn trong transaction.
- Phân biệt requested quantity, approved quantity và resalable/restocked quantity khi duyệt một phần hoặc hàng không thể bán lại; bổ sung bounds phù hợp sau khi chốt flow.
- Lưu inspection actor/time, condition findings, evidence/note và recommendation; tách Admin decision actor/time/reason. reviewed_by/date trên header và condition_status trên line chưa biểu đạt đủ flow Staff kiểm tra → Admin quyết định → yêu cầu thêm thông tin.
- Quyền approve/reject chỉ Admin theo phần chi tiết live RDS; authorization thuộc service. is_restocked có thể hỗ trợ chống retry nếu được kiểm tra dưới row lock và ghi cùng transaction với inventory/movement. Cần reference line-level đủ rõ để đối soát lần restock.
- Khóa return/order line liên quan và inventory theo lock order thống nhất để chống hai reviewer, nhiều request vượt quantity và duplicate restock. Refund completion thuộc UC-33, cũng nằm trong danh sách AnNP vừa xác nhận; inspection/approval và disbursement vẫn là hai transaction nghiệp vụ có ranh giới riêng.

### UC-33 Record Refund

`hm_refund` có FK return_request/payment, refund_code unique, amount không âm, method, gateway reference, status và refunded_at. Nó lưu được một refund tổng trên mỗi request. Tuy nhiên hai FK độc lập chưa bảo đảm payment thuộc order của request; service phải xác nhận đúng order, payment đã completed và request đã approved trước disbursement.

Các điểm thiếu/chưa rõ:

- Unique(return_request_id) chỉ cho một record/request. Điều này đủ nếu cam kết một logical refund tổng và retry cùng record/reference; chưa đủ nếu cần nhiều attempt ledger hoặc partial payouts. Chốt mô hình retry trước khi bỏ unique; không mặc định mọi retry phải tạo dòng mới.
- refund_code có thể làm idempotency reference nếu tạo trước khi gọi provider và giữ nguyên qua retry. gateway_transaction_id chưa unique; chốt provider-scoped unique reference khi provider có mã ổn định để chống duplicate outcome across records.
- CHECK amount >= 0 không chặn tổng refund vượt captured amount. Cần lock payment/những refund liên quan, kiểm tra tổng completed và pending allocations theo policy rồi ghi atomically. Không tính pending là tiền đã trả, nhưng phải tránh cho hai pending refunds cùng chiếm toàn bộ balance.
- `hm_return_item.refund_amount` chưa CHECK không âm. Lấy approved quantity và snapshot order_item price/discount allocation để tính tiền; không lấy selling_price hiện tại. Tổng approved lines cần khớp logical refund theo hợp đồng.
- Chưa có trường riêng cho payout actor/receipt reference tiền mặt; created_by chỉ dùng được nếu định nghĩa nó là principal của hành động disbursement. gateway_transaction_id không mặc nhiên là receipt code tiền mặt. Cần cách lưu evidence ổn định cho từng channel.
- status/time/audit phải tách eligibility, pending, failed và completed; cập nhật trạng thái logical attempt được phép, completed financial evidence phải immutable theo policy. Gateway result chưa xác minh không được ghi completed.

Test ưu tiên khi implement: request chưa approved; payment sai order/chưa paid; amount vượt balance; retry cùng reference; pending → completed; hai refunds đồng thời trên cùng payment; cash payout có actor/reference/time. Các invariant liên bảng cần service transaction, không chỉ thêm CHECK vào SQL.

### UC-34 View Financial Reports

Có thể tính báo cáo cơ bản từ `hm_payment.amount/status/payment_time`, `hm_refund.amount/status/refunded_at` và `hm_order.order_type`. Không bắt buộc thêm bảng report vì UC này chỉ đọc ledger.

Cần hoàn thiện hợp đồng dữ liệu và query:

- Chốt status nào là settled/completed cho payment và refund; yêu cầu timestamp hoàn thành khi settled. Hiện status varchar tự do và payment_time/refunded_at nullable nên SQL chưa bảo đảm dữ liệu đủ để tính theo kỳ.
- Chốt timezone store và dùng khoảng `[start, next-day-end)` trên event settlement time. Refund tuần này cho payment tuần trước phải là outflow tuần này; không lọc refund bằng order.created_date hay ngày payment gốc.
- Cộng payment và refund riêng, rồi ghép tổng theo channel. Không join thẳng nhiều payment × nhiều refund rồi SUM vì dễ nhân đôi số tiền. Net settled cash flow = completed inflow - completed outflow; pending/failed bị loại.
- Channel lấy từ original order; refund phải nối đúng payment/order như UC-33 đã kiểm tra. Chốt revenue recognition nếu UI còn gọi một số liệu là sales revenue; tiền thu, doanh số order và net cash flow không mặc nhiên là cùng một phép tính.
- Cần immutable settled evidence và chính sách điều chỉnh rõ để báo cáo lịch sử không đổi do sửa bản ghi. `modified_date` không thay ledger audit. Index status/event-time hoặc composite phù hợp sẽ thêm theo query đã chốt; không cần dựng report engine.
- Admin-only, read-only; một lần báo cáo phải có snapshot nhất quán nếu chạy nhiều query trong lúc payment/refund ghi mới. Failure không được trình bày tổng một phần như kết quả hoàn chỉnh.

Test ưu tiên khi implement: pending/failed excluded; refund kỳ khác payment; online/counter breakdown; một payment nhiều refunds không double-count; giao dịch đúng nửa đêm store timezone; non-Admin denied; không có transaction thì zero totals. Review này chưa chạy report query hay test concurrency vì chưa có implementation UC-34.

## Mâu thuẫn cần ghi vào Backlog

- UC-14/15 và BR-11 giữ stock tại checkout, nhưng UC-19 Description/Postconditions/step 6 lại tăng Reserved khi confirm. Phải chốt thời điểm reserve một lần và xử lý reservation sau verified payment; nếu giữ flow checkout, UC-19 chỉ xác nhận/revalidate reservation, không cộng stock lần hai. Đây là shared contract ảnh hưởng trực tiếp UC-15, chưa sửa UC của owner khác.
- UC-31 hiện cho request có thể Approved, cập nhật inventory ngay ở bước 7 và khởi động refund/exchange; UC-32 yêu cầu Admin quyết định/restock và UC-33 mới disburse. Chốt UC-31 tạo request/inspection evidence, UC-32 approve/restock, UC-33 refund nếu nhóm chọn flow này. Exchange trong UC-31 là phạm vi chưa xác nhận, không tự thêm engine exchange.
- UC-32 overview/screen description trong live RDS cho Staff approve/reject, nhưng phần UC chi tiết chỉ cho Admin. Chưa tự chọn và sửa các artifact.
- BR-40 đang được dùng cho hai quy tắc khác nhau: stock reservation trong UC-19 và return decision/restock trong UC-32. BR-39 cũng có va chạm giữa phần UC-19 và payment. Cần ổn định BR IDs và tham chiếu, giữ UC IDs.
- Phân công Iter1–Iter4 khác danh sách AnNP người dùng vừa xác nhận; đồng bộ UC-13/33/34 và UC-10/11/16 trong Sheet trước khi dựa vào iteration để giao task.
- BR-41/42 trong appendix live RDS đang nói fulfillment/COD, trong UC-33/34 lại là refund limit/settled totals. Cần sửa registry BR cùng với BR-39/40 để không dùng một ID cho hai rule.
- Số bảng: XML cung cấp có 23 bảng; Record of Changes/live RDS nhắc 24 bảng. Phải đối chiếu entity thiếu, không tự thêm bảng để khớp số.

Ngoài phần AnNP: thiếu goods_receipt_item cho partial receiving UC-29; `hm_po_item` có nhãn CHECK bị cụt thành `CK 0`. Các kiểm tra cart/category trong script vẫn là kiểm chứng sơ đồ toàn cục, không chứng minh UC-10/11 thuộc AnNP. UC-16 cần lịch sử trạng thái từ các writer, là hợp đồng tích hợp với owner được nhóm xác nhận. Không tự triển khai các flow ngoài bảy UC hiện tại.

## Kiểm chứng đã thực hiện

- PostgreSQL 17.11 trong container riêng, network none, không mở host port. DDL chạy hết và COMMIT.
- 23 bảng / 277 cột / 26 FK; đầy đủ cột nguồn và FK tự tham chiếu đã nêu.
- 9 kiểm tra bảo vệ đạt: số bảng/cột; duplicate cart line; quantity 0; orphan SKU; available stock tự tính/cập nhật; over-reservation; reserved âm; hai default; duplicate payment idempotency.
- 5 khoảng trống được chứng minh bằng insert/update: zero default; soft-deleted default chặn replacement; duplicate root category name; self-parent category; return unpurchased SKU/quantity vượt đơn.
- Fixtures ROLLBACK; số user fixture sau kiểm tra = 0. Các khoảng trống được đánh dấu PASS GAP trong script để phân biệt với kiểm tra bảo vệ.
- Không chạy lại build/backend/browser của ứng dụng vì không sửa business code. Chưa xác minh concurrency flow hoặc khả năng hoàn thành UC; chưa deploy Supabase.

Lệnh kiểm tra từ repo root, dùng Docker Desktop trên macOS/Linux hoặc PowerShell:

```text
docker run --detach --rm --name homemart-diagram-review-pg17 --network none -e POSTGRES_HOST_AUTH_METHOD=trust postgres:17
docker exec homemart-diagram-review-pg17 pg_isready -U postgres
docker cp docs/database/HomeMart_From_Diagram_PostgreSQL.sql homemart-diagram-review-pg17:/tmp/schema.sql
docker exec homemart-diagram-review-pg17 psql -X -U postgres -v ON_ERROR_STOP=1 -f /tmp/schema.sql
docker cp docs/database/HomeMart_Diagram_Verification.sql homemart-diagram-review-pg17:/tmp/verify.sql
docker exec homemart-diagram-review-pg17 psql -X -U postgres -v ON_ERROR_STOP=1 -f /tmp/verify.sql
docker stop homemart-diagram-review-pg17
```

Chờ pg_isready báo accepting connections trước chạy DDL. Container dùng trust chỉ trong môi trường kiểm tra network none; không dùng cấu hình này cho Supabase hoặc server triển khai.

## Thứ tự tiếp theo

1. Đồng bộ danh sách AnNP đã xác nhận trong Backlog; chốt identity/Admin mapping và timezone.
2. Hoàn thiện UC-04 default còn hiệu lực, UC-05 credential contract và UC-13 publication/barcode/stale-write; tạo migration mới cho phần đã được chốt.
3. Chốt provider và transaction/state contracts của UC-15; original-order-line/inspection/approval cho UC-32; idempotency/balance/payout evidence cho UC-33.
4. Làm UC-34 sau khi UC-15/33 cùng các writer POS/COD cung cấp settled ledger đúng hợp đồng. Triển khai một UC vertical slice mỗi lần, có test permission/failure/concurrency phù hợp.
