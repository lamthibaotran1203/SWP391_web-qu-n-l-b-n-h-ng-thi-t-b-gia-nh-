# Các điểm AnNP cần chốt và sửa sau

Ghi ngày 05/10/2026 theo trao đổi với người dùng. Phạm vi: **UC-04, UC-05, UC-13,
UC-15, UC-32, UC-33, UC-34**. Các quyết định nền tảng đã được người dùng chốt và ghi vào Backlog ngày 05/10/2026.
V4 đã được triển khai; checklist dưới đây giữ lại công việc service/RDS cần tiếp tục. Khi thay đổi yêu cầu, ghi quyết định vào Backlog rồi cập nhật
RDS, sơ đồ, migration, code và test liên quan.

## Nền tảng chung

- [x] Đồng bộ Lead của bảy UC AnNP trong Backlog. Các tab Iter vẫn cần nhóm đồng bộ.
- [x] Giữ `app.user_account` của backend với `hm_user/hm_role/hm_userrole` trong sơ đồ.
  Đề xuất giữ account hiện có và nối FK nghiệp vụ vào cùng nguồn identity.
- [ ] Thống nhất Admin trong RDS với MANAGER trong code; không tự tạo thêm một role khác.
- [x] Dùng TIMESTAMPTZ cho sự kiện, Asia/Ho_Chi_Minh cho kỳ báo cáo và status của V4.
- [ ] Sửa registry BR bị trùng nghĩa, gồm BR-38/39/40/41/42 và các tham chiếu liên quan.
- [x] Áp dụng V4 bằng Flyway; giữ nguyên V1/V2/V3 đã áp dụng.

## Theo từng UC

- [ ] **UC-04:** unique default chỉ tính địa chỉ còn hiệu lực; còn địa chỉ thì phải có một
  default. Chuyển/xóa default có replacement trong cùng transaction; kiểm tra ownership.
- [ ] **UC-05:** sửa đúng password_hash của account đang login; kiểm tra password cũ,
  password policy và rollback khi lỗi. Chốt giữ/hủy session khác nếu đưa vào phạm vi.
- [ ] **UC-13:** chốt barcode bắt buộc lúc tạo hay publish, vị trí ảnh đại diện và khả năng
  lưu draft thiếu category/brand. Chặn publication thiếu dữ liệu, phát hiện stale edit,
  giữ snapshot giá order cũ và không thay đổi stock khi sửa catalog.
- [ ] **UC-15:** chọn một provider; phân biệt attempt reference và gateway reference;
  lưu provider/callback evidence/completion time. Chốt callback, timeout, cancellation
  và late success; bảo đảm không ghi payment hay nhả reservation hai lần.
- [ ] **UC-32:** link return line tới original order_item, kiểm tra cùng order và quantity
  trả cộng dồn; lưu inspection actor/time/evidence, approved quantity và restock quantity.
  Staff kiểm tra, Admin duyệt nếu nhóm chốt flow này; restock một lần trong transaction.
- [ ] **UC-33:** kiểm tra request approved, payment completed thuộc đúng order, balance
  còn hoàn và kênh gốc. Chốt một logical refund/request hay partial payouts; reference
  ổn định qua retry; lưu payout actor/time/receipt, giữ completed evidence và chống hoàn lặp.
- [ ] **UC-34:** chốt settled statuses và completion timestamps; cộng payment/refund riêng,
  tránh SUM bị nhân đôi do join; lọc theo kỳ/múi giờ của sự kiện thu/hoàn tiền. Phân biệt
  doanh số với tiền thực thu; report chỉ đọc và dành cho Admin.

## Các hợp đồng liên UC cần nhóm chốt

- [ ] **UC-14/15 ↔ UC-19:** hiện reserve tại checkout rồi lại reserve khi confirm.
  Đề xuất reserve một lần tại checkout; confirm kiểm tra reservation hiện có.
  Chốt vòng đời reservation sau payment thành công.
- [ ] **UC-31 ↔ UC-32/33:** UC-31 hiện cập nhật stock/khởi động refund sớm.
  Đề xuất UC-31 tạo request, UC-32 quyết định/restock, UC-33 disburse refund.
- [ ] Đồng bộ quyền approve/reject UC-32 giữa overview, screen description và flow chi tiết.
- [ ] Xác nhận riêng exchange/voucher, nhiều provider, nhiều đợt refund hoặc báo cáo kế toán
  nếu nhóm muốn thêm; không mặc nhiên triển khai những phần mở rộng này.

## Tình trạng triển khai

Schema chính thức là Flyway V4, đã lên Supabase. File chuyển từ sơ đồ
`HomeMart_From_Diagram_PostgreSQL.sql` chỉ là bằng chứng review, không chạy thêm vào Supabase.

UC-13 đã chốt barcode nội bộ bắt buộc, ảnh ở product, INACTIVE cho dữ liệu chưa đủ;
UC-33 một logical refund/request; UC-34 dùng thời điểm settled của từng luồng.
UC-14/15 reserve một lần tại checkout, UC-19 kiểm tra reservation; UC-31 tạo request,
UC-32 quyết định/restock, UC-33 disburse. Cần cập nhật RDS và triển khai service theo các quyết định này.

Giới hạn quan trọng: ownership, default replacement, return cộng dồn, số tiền hoàn còn lại,
lock/idempotency và đồng bộ cột product cũ vẫn là việc code cần làm; không xem migration là đã hoàn tất UC.

Xem [quyết định và bằng chứng triển khai](HomeMart_Schema_Deployment_2026-10-05.md).

## RDS cập nhật sau — catalog chuẩn hóa V5

- [ ] UC-13: product chứa thông tin chung; SKU là đơn vị bán nhập thủ công; giá bán ở SKU,
  tồn ở inventory. Bỏ hướng dẫn đọc/ghi product.price/stock_quantity cũ; legacy_* là archive.
- [ ] Mô tả INACTIVE cho bản nháp; ACTIVE cần category/brand/ảnh; product/SKU/category/brand
  đã xóa không xuất hiện công khai; hết hàng vẫn hiển thị nhưng không mua được.
- [ ] UC-09/10/12/14 và các UC kho/đơn/return: dùng skuId và nguồn giá/tồn thống nhất;
  order giữ snapshot. Đây là hợp đồng tích hợp, không xác nhận đã làm UC của thành viên khác.
- [ ] RDS physical schema/ERD: thể hiện V5 và loại các cột legacy khỏi nguồn nghiệp vụ;
  ghi FK product->category/brand, SKU->product, inventory->SKU và version.
- [ ] API/screen evidence: tham chiếu Catalog_Shared_Contract.md; trang quản lý UC-13 và
  API ghi vẫn cần triển khai/test đầy đủ; không dùng API đọc làm bằng chứng UC-13 hoàn thành.

Nền catalog đã chuẩn hóa; không còn yêu cầu đồng bộ hai nguồn giá/tồn. Các đề xuất đồng bộ
cột product cũ ở ghi chú trước đã được V5 thay thế bằng archive bất biến.
