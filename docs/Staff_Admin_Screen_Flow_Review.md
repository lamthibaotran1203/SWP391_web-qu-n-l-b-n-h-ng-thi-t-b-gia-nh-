# Staff & Admin Screen Flow — bản review để vẽ lại

Ngày đối chiếu: 26/09/2026. Nguồn: Google Sheet `SWP391_G3_UCs` (tab Backlog, 35 UC), trang `ScreenFlow` trong `SWP391_Diagram.drawio` trên Drive, và `HomeMart_RDS_Consolidated_Review.docx`. File draw.io đi kèm: `Staff_Admin_Screen_Flow_Review.drawio`.

Đây là luồng màn hình **đề xuất để nhóm review**, chưa phải quyết định đã duyệt trong Backlog hoặc bằng chứng UI đã code. Giữ nguyên ID và tên UC hiện tại của Sheet ở các chỗ tham chiếu. Trong draw.io có đúng hai trang theo actor: `Staff Screen Flow` và `Admin Screen Flow`. Admin được gắn quyền trực tiếp trên luồng của mình, không kế thừa actor Staff.

Mỗi trang là một flow liền mạch: `Home Page -> Login -> Dashboard` đúng actor, rồi từ Dashboard rẽ tới mọi nhánh nghiệp vụ. Mỗi actor chỉ có một Home Page, một Login và một Dashboard. Logout thành công quay về chính Login ở đầu flow; không tạo thêm screen `Public Home / Login`. Các screen chung xuất hiện trên cả trang Staff và Admin để quyền của Admin đọc độc lập.

## Cách đọc

- Hình chữ nhật = page/screen; hình oval = popup/dialog. Đây là hai loại hình duy nhất trên canvas; tất cả dùng nền trắng, viền đen.
- Không đặt tiêu đề module, UC number, legend hoặc note thuyết minh rời trên canvas. UC ID chỉ nằm trong tài liệu traceability, không nằm trong tên screen.
- Mọi mũi tên dùng cùng một kiểu đen và phải ghi trigger cụ thể như `Click "Login"`, `Click "Back"`, `Click "Cancel"`, `Click "Confirm"`.
- Không vẽ chiều ngược cho một giao dịch đã commit. Ví dụ `Confirm Receipt` có thể dẫn tới Receipt Detail; mũi tên quay lại là `Back to PO`, không có nghĩa hoàn tác nhập kho.
- `-->` = thao tác/điều hướng; `{điều kiện}` = nút chỉ hiện hoặc submit chỉ hợp lệ khi điều kiện đúng.
- Hủy/đóng popup quay về màn hình đã mở popup và giữ dữ liệu chưa submit. Lỗi validation, lỗi quyền, stale state hoặc trùng request ở lại màn hình hiện tại với thông báo rõ. Thành công trở về detail hoặc list đã refresh từ server.
- Tìm kiếm, lọc, tab và toast không mặc nhiên là full screen. Form dài như Product/SKU, Purchase Order hoặc POS có thể là full screen; form ngắn như category/brand có thể là popup. Đây là quyết định UI, không tạo thêm top-level UC.

## Quyền và điểm vào chung

| Nhóm | Staff | Admin | Ghi chú |
| --- | --- | --- | --- |
| UC-02, UC-03, UC-05 | Login, Logout, Change Password | Cùng quyền | Sau login vào dashboard đúng vai trò; người sai role nhận Access Denied. |
| UC-18–22, UC-24, UC-29, UC-31–32 | Các tác vụ vận hành được Sheet gán Staff/Admin | Cùng quyền ở cấp UC | Với UC-32, **Staff ghi kết quả kiểm tra**, Admin quyết định approve/reject. Chưa được dùng tên UC chung để cho Staff duyệt. |
| UC-07, UC-11–13, UC-23, UC-25–28, UC-33–35 | Không có quyền | Có quyền | UC-23/25/33 là thao tác nhạy cảm; mở từ detail, bắt buộc lý do/điều kiện phù hợp. |

```text
Home Page
  └─ Click "Login" ──> Login (UC-02)
       ├─ Click "Back" ──> Home Page
       ├─ Click "Sign In" + valid Staff credentials ──> Staff Dashboard
       ├─ Click "Sign In" + valid Admin credentials ──> Admin Dashboard
       ├─ wrong role for protected URL ──> show Access Denied on Login
       └─ invalid credentials ──> show inline error; remain on the same Login

Dashboard ── Change Password (UC-05) ──> Change Password
       └─ successful update ──> Success; return to previous protected screen
Any protected screen ── Click "Logout" (UC-03) ──> Confirm Logout
       ├─ Cancel ──> same screen, session intact
       └─ Click "Confirm Logout" ──> session invalidated; the same Login screen
```

Nếu nhóm bỏ Confirm Logout thì Logout kết thúc session ngay; Screen Flow và Screen Description phải cùng một cách xử lý. Nếu giữ popup, Cancel phải quay về đúng màn hình đã mở popup. Login lỗi là trạng thái inline trên Login, không phải một screen mới.

## Quy tắc chiều về trong file draw.io

| Luồng đi | Chiều về phải có | Ý nghĩa |
| --- | --- | --- |
| Home Page → Login | Login → Home Page: `Click "Back"` | Quay lại trang public. |
| Dashboard → màn hình danh sách | List → Dashboard: `Click "Dashboard"` | Quay về điểm điều hướng chính. |
| List → Detail | Detail → List: `Click "Back"` | Giữ filter và vị trí cuộn nếu UI hỗ trợ. |
| Detail/Form → popup xác nhận | Popup → màn hình gọi: `Click "Cancel"` | Đóng popup, không ghi dữ liệu. |
| List/Detail → form nhập liệu | Form → màn hình cha: `Click "Back"` | Nếu có dữ liệu chưa lưu, UI phải hỏi xác nhận trước khi rời. |
| Confirm/Submit thành công → detail/list đã refresh | `Back to List/Detail` khi người dùng điều hướng tiếp | Đây là điều hướng sau commit, không phải hoàn tác nghiệp vụ. |

Thông báo lỗi validation, sai quyền hoặc thanh toán thất bại nằm trên màn hình hiện tại. Nó không cần `Back` vì người dùng chưa rời màn hình; nếu vẽ khung trạng thái để giải thích thì mũi tên phải ghi hành động gây ra lỗi và hành động phục hồi như `Click "Retry"`.

## Staff — đơn online và giao hàng (UC-18, 19, 21, 22)

```text
Staff Dashboard
  └─ Sales Orders ──> Sales Order List (UC-18)
       ├─ keyword/status/date filter ──> cùng list, kết quả cập nhật
       └─ chọn một order ──> Sales Order Detail (UC-18)
            ├─ {Pending Confirmation} Confirm (UC-19)
            │    └─ Confirm Order ── Confirm ──> Order Detail: Confirmed
            ├─ {Confirmed, stock reserved} Pack & Dispatch (UC-21)
            │    └─ Dispatch Preparation ── Submit ──> Order Detail: Dispatched
            └─ {Dispatched} Record Delivery Outcome (UC-22)
                 └─ Delivery Outcome
                      ├─ Delivered ──> Order Detail: Delivered
                      └─ Delivery failed ──> Order Detail: failed/return pending
```

- `Sales Order List` chứa tìm kiếm/lọc ngay trên list. `Search & Filter` không cần một screen riêng nếu UI thực tế là thanh lọc. UC-18 vẫn phải đưa người dùng vào **Order Detail** trước UC-19, đúng nhận xét của giảng viên.
- `Order Detail` hiển thị mã đơn, người mua/địa chỉ, các order item và SKU, số lượng/giá, phương thức và trạng thái thanh toán, số lượng đã reserve, trạng thái giao, lịch sử thao tác. Nút Confirm/Dispatch/Record Outcome chỉ hiện khi trạng thái tương ứng hợp lệ.
- Confirm Order kiểm tra lại trạng thái và tồn/reservation trên server. Nếu người khác đã xác nhận/hủy đơn, báo stale state và refresh detail; không ghi hai lần.
- Dispatch Preparation ghi số lượng pick/pack, người thực hiện và thời điểm bàn giao. Chỉ chuyển reserved stock thành outbound issue **một lần** sau khi xác nhận bàn giao; thiếu hàng phải dừng và báo lỗi, không tự điều chỉnh tồn để cho qua.
- Delivery Outcome thành công ghi kết quả và COD nếu áp dụng. Thất bại cần lý do; không tự cộng lại physical stock khi hàng chưa thật sự về kho/được kiểm tra. Điểm này cần nhóm chốt quy tắc với RDS và ERD.

## Staff — bán tại quầy (UC-20)

```text
Staff Dashboard ── Counter Sales ──> POS Sale
  ├─ tìm SKU trên chính POS ──> thêm/sửa/xóa dòng hàng trên POS
  ├─ quantity > available ──> Insufficient Stock; giữ draft
  └─ Confirm & Take Payment ──> Payment Summary
       ├─ Cancel / payment failed ──> POS Sale, draft còn nguyên; chưa trừ tồn
       └─ payment accepted ──> Sale Receipt
            └─ New Sale ──> POS Sale, draft trống
```

`Search Product` là control trong POS, không cần full screen. Sau khi xác nhận thanh toán thành công, tạo sale/receipt và trừ physical stock trong cùng giao dịch hoặc quy tắc idempotent tương đương; không cấp receipt thành công khi stock issue thất bại. UC-20 chưa được đưa vào Iter1–Iter4 của Sheet, nên người phụ trách/iteration phải được chốt ở đó.

## Staff — theo dõi tồn và nhận hàng mua (UC-24, 29)

```text
Staff Dashboard ── Inventory ──> Stock List (UC-24)
  └─ chọn SKU ──> SKU Stock Detail
       └─ xem physical / reserved / available / low-stock threshold

Staff Dashboard ── Purchase Orders ──> Purchase Order List
  └─ chọn PO có số lượng còn nhận ──> Purchase Order Detail
       └─ Receive Goods (UC-29) ──> Goods Receipt Form
            ├─ nhập SKU, quantity thực nhận, note, chứng từ
            ├─ quantity vượt phần còn lại ──> Error; giữ form
            └─ Submit ──> Confirm Receipt
                 ├─ Cancel ──> Goods Receipt Form
                 └─ Confirm ──> Receipt Detail + PO remaining updated
```

- Staff chỉ xem PO để nhận hàng; Backlog giao tạo PO và quản lý supplier cho Admin. Không đặt `Create PO` trên Staff menu.
- `Purchase Order Detail` phải cho thấy ordered, previously received, remaining của từng PO item để partial receiving có nghĩa. Trạng thái PO nào cho phép nhận còn cần chốt; bản này dùng điều kiện chung “có số lượng còn nhận”.
- Confirm Receipt tăng physical stock đúng số lượng nhận, tạo `goods_receipt` và dòng chi tiết theo PO item/SKU, cùng stock movement trong một transaction. Gửi lại cùng một request không được cộng tồn lần hai. Tạo PO đơn thuần không tăng tồn.

## Staff — tiếp nhận và kiểm tra hàng trả (UC-31, 32)

```text
Staff Dashboard ── Returns ──> Return Request List
  ├─ khách mang hàng đến quầy ──> In-Store Return Intake (UC-31)
  │    ├─ tìm receipt/order gốc ──> Select Original Order
  │    ├─ chọn purchased order item + quantity + reason
  │    └─ Submit ──> Return Detail: Awaiting Inspection
  └─ chọn request ──> Return Detail (UC-32)
       └─ {Awaiting Inspection} Inspect Items ──> Inspection Form
            ├─ ghi tình trạng từng item, số lượng, bằng chứng
            └─ Submit ──> Return Detail: Awaiting Admin Decision
```

Staff không chọn Approve/Reject và không thấy nút Refund. `Select Original Order` phải chọn dòng hàng đã bán, không chọn SKU tự do rồi tạo return không liên quan đơn gốc. Tiếp nhận hoặc kiểm tra hàng không tự hoàn tiền và không tự cộng vào tồn bán được. Nếu nhóm quyết định Staff được duyệt, phải sửa actor matrix trong Sheet trước khi đổi flow.

## Admin — quản trị tài khoản và catalog (UC-07, 11–13)

```text
Admin Dashboard
  ├─ Accounts ──> Account List (UC-07)
  │    ├─ Create/Edit ──> Account Form ── Save ──> Account Detail/List refreshed
  │    └─ Lock/Unlock ──> Confirm + reason ──> Account Detail refreshed
  ├─ Categories ──> Category List (UC-11)
  │    └─ Add/Edit/Deactivate ──> Category Form/Confirm ──> list refreshed
  ├─ Brands ──> Brand List (UC-12)
  │    └─ Add/Edit/Deactivate ──> Brand Form/Confirm ──> list refreshed
  └─ Products & SKUs ──> Product List (UC-13)
       └─ chọn/tạo Product ──> Product Detail & SKU Editor
            └─ Save/Deactivate ──> Product Detail refreshed
```

Account form không hiển thị mật khẩu hiện có. Cách giao mật khẩu ban đầu/activation link phải thống nhất với RDS và bảo mật hiện hành. Category/Brand/Product đang được Backlog tách ba UC, dù UI có thể dùng chung một module Catalog. SKU là đơn vị bán nhập thủ công (model/capacity/package), không giả định engine size/color.

## Admin — đơn hàng, bán tại quầy và hủy bởi cửa hàng (UC-18–23)

```text
Admin Dashboard ── Sales Orders ──> Sales Order List (UC-18)
  └─ chọn order ──> Sales Order Detail
       ├─ {Pending Confirmation} Confirm Order (UC-19) ──> Confirm
       ├─ {Confirmed} Pack & Dispatch (UC-21) ──> Dispatch Preparation
       ├─ {Dispatched} Record Delivery Outcome (UC-22) ──> Outcome
       └─ {Not Shipped, policy allows} Cancel by Store (UC-23)
            └─ Cancel Order: reason + refund consequence
                 ├─ Back ──> same Order Detail
                 └─ Confirm ──> Order Detail: Cancelled / Refund Pending

Admin Dashboard ── Counter Sales (UC-20) ──> cùng POS Sale flow như Staff
```

Admin có đường đi **trực tiếp** tới toàn bộ UC-18–22 do Sheet gán Admin, không dựa vào ký hiệu actor inheritance. Store cancellation chỉ bắt đầu tại Order Detail sau khi xem trạng thái, payment và reservation. Refund vì hủy đơn cần theo luồng UC-23/payment của đơn; đừng gộp ngầm với UC-33 vốn là hoàn tiền sau return đã duyệt. Nếu đã shipped thì nút hủy cửa hàng biến mất hoặc báo không hợp lệ.

## Admin — tồn kho và mua hàng (UC-24–29)

```text
Admin Dashboard ── Inventory ──> Stock List (UC-24)
  └─ chọn SKU ──> SKU Stock Detail
       ├─ Adjust Count (UC-25) ──> Stock Count Adjustment
       │    └─ nhập actual count + reason ──> Confirm Adjustment
       │         └─ Confirm ──> SKU Stock Detail + new movement
       └─ View Movements (UC-26) ──> Movement History, filter by SKU/time/reference

Admin Dashboard ── Suppliers ──> Supplier List (UC-27)
  └─ Add/Edit/Deactivate ──> Supplier Form ── Save ──> Supplier List

Admin Dashboard ── Purchase Orders ──> Purchase Order List
  ├─ Create PO (UC-28) ──> PO Editor: supplier + SKU/qty/cost
  │    └─ Review & Submit ──> PO Detail (không tăng tồn)
  └─ chọn PO ──> PO Detail ── Receive Goods (UC-29)
       └─ cùng Goods Receipt Form/Confirm/Receipt Detail như Staff
```

Adjustment là chênh lệch kiểm kê có actual count và reason; không dùng thay Receipt. Movement History là danh sách audit read-only, không có nút sửa/xóa dòng ledger. PO Editor phải báo supplier/SKU không hợp lệ và giữ draft; đơn đặt mua chưa phải hàng đã vào kho.

## Admin — hàng trả và hoàn tiền (UC-31–33)

```text
Admin Dashboard ── Returns ──> Return Request List
  ├─ In-Store Return Intake (UC-31) ──> cùng luồng tiếp nhận như Staff
  └─ chọn request ──> Return Detail
       ├─ Inspect Items (UC-32) ──> Inspection Form, nếu Admin trực tiếp kiểm tra
       └─ {inspection recorded} Review Decision (UC-32)
            └─ Approval Decision: approve/reject + reason
                 ├─ Reject ──> Return Detail: Rejected
                 └─ Approve ──> Return Detail: Approved
                      └─ {refund required} Record Refund (UC-33)
                           └─ Refund Form: method/amount/reference
                                ├─ failure/pending gateway ──> Return Detail: Refund Pending/Failed
                                └─ completed ──> Return Detail: Refunded
```

Phê duyệt và hoàn tiền là hai bước khác nhau. Không mở `Record Refund` trực tiếp từ Return Request List. Hàng đủ điều kiện nhập lại kho chỉ được tăng tồn một lần sau quy tắc kiểm tra/phê duyệt đã chốt; hàng hỏng/không bán lại được phải đi nhánh khác. Gateway callback phải xác thực và idempotent nếu hoàn tiền online; không đánh dấu Refunded chỉ vì Admin bấm Submit.

## Admin — báo cáo (UC-34, 35)

```text
Admin Dashboard ── Financial Reports (UC-34)
  └─ date range / sales channel / payment status filter
       └─ cùng screen: sales, cash inflow/outflow, source/drill-down

Admin Dashboard ── Inventory Roll-Forward (UC-35)
  └─ period / SKU filter
       └─ cùng screen: Opening + Receipts - Issues = Closing
            └─ chọn SKU/reference ──> Movement History (UC-26)
```

Báo cáo phải tính từ order/payment/refund/stock movement thật. Filter không cần screen riêng. Admin thấy quyền truy cập và nguồn dữ liệu rõ ràng; Staff không có menu báo cáo nếu Sheet chưa gán quyền.

## Những lỗi cụ thể trong trang ScreenFlow hiện có

1. Staff đi từ public `Home Page` sau login; nên vào `Staff Dashboard`/workspace đúng quyền. Admin lại bắt đầu thẳng từ `Admin Dashboard`, thiếu nhánh Login/Logout/Change Password.
2. `Search & Filter` ở Sales Orders, `Search Product` ở POS và nhiều form ngắn bị vẽ như screen riêng dù khả năng cao là control/popup. Phải chốt theo UI thật và ghi type nhất quán.
3. Admin `Sales Orders -> Cancel Order by Store` bỏ qua Order Detail và điều kiện chưa shipped. Đường đúng đi từ một order cụ thể đã xem.
4. Admin `Return Requests -> Record Refund Completion` bỏ qua Return Detail, inspection và approval. Đây là lỗi quyền/nghiệp vụ nghiêm trọng nhất.
5. Staff `Inspect Return -> Submit Decision` làm người đọc tưởng Staff được approve/reject. Đổi thành `Submit Inspection`; quyết định nằm ở Admin.
6. Staff `Purchase Orders -> Receive Stock` thiếu PO Detail, remaining quantity và partial receipt. Confirm Receipt phải tạo receipt/movement và tăng tồn đúng một lần.
7. `Counter Sale -> Payment & Receipt -> Counter Sale` chưa có trạng thái payment fail/stock fail, và receipt không nên biến mất ngay trong màn POS mới.
8. `Inventory Overview -> Adjust Stock` trên Admin thiếu SKU Detail, actual count, reason và xác nhận. `Stock Movement History` phải nối từ SKU/report khi cần tra cứu, không chỉ từ Dashboard.
9. Admin dashboard không biểu diễn đầy đủ quyền UC-18–22, UC-20, UC-29, UC-31–32 dù Backlog gán Admin trực tiếp. File mới lặp lại các luồng dùng chung ở trang Admin hoặc dẫn tới flow tương ứng bằng ghi chú rõ.

## Quyết định nhóm còn phải chốt trước khi vẽ bản nộp

1. Admin trong tài liệu tương ứng `MANAGER` trong code hay cần đổi từ vựng quyền.
2. Confirm Logout có thật trong UI không; nếu có, Cancel quay về màn hình nào.
3. Chính sách giao thất bại: khi nào hàng vật lý về kho và ai xác nhận.
4. PO trạng thái nào cho phép nhận; có cần bước duyệt PO hay không.
5. UC-31 có cần return tại quầy thật không; bằng chứng mua hàng và order item được tìm như thế nào.
6. UC-32: Staff chỉ inspect, Admin approve/reject như feedback; Admin có tự inspect được không.
7. Refund online/cash được coi completed ở thời điểm nào; cách đối soát refund khi hủy đơn khác return.
8. Actual screen/popup của Product/SKU, PO Editor, Goods Receipt, Inspection và Refund phụ thuộc UI thực tế; thống nhất Screen Description/Authorization với flow cuối.

File này và `.drawio` đi kèm là bản review để mày đọc/sửa. Khi nhóm chốt các điểm trên, sửa Backlog 35 UC trước rồi mới cập nhật bản nộp trên Drive.
