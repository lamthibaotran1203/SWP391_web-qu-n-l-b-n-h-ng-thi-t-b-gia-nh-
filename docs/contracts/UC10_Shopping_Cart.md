# UC-10 — Manage Shopping Cart

Customer đã đăng nhập có thể thêm SKU từ UC-09, xem giỏ tại `/cart`, cập nhật số lượng và xóa một hoặc nhiều SKU. Giỏ lưu theo tài khoản trong `app.hm_cart` (migration V4 đã có sẵn).

## API

| Method | Endpoint | Request |
| --- | --- | --- |
| GET | `/api/v1/cart` | Không có body |
| POST | `/api/v1/cart/items` | Header `Idempotency-Key: UUID`, body `{ "skuId": 1, "quantity": 2 }` |
| PUT | `/api/v1/cart/items/{skuId}` | `{ "quantity": 3, "expectedVersion": "UUID nhận từ GET giỏ" }` |
| DELETE | `/api/v1/cart/items` | `{ "skuIds": [1, 2] }` |

Mọi endpoint yêu cầu role CUSTOMER; thao tác ghi yêu cầu CSRF. Chủ giỏ lấy từ phiên đăng nhập, không nhận userId từ client. GET/POST/PUT trả ApiEnvelope chứa items, subtotal dạng chuỗi số nguyên VND và checkoutEligible. Mỗi item có thông tin Product/SKU, quantity, unitPrice, lineTotal, availableQuantity, eligible, available và version UUID.

DELETE trả `{removed, cart, refreshRequired, message}`. Nếu xóa đã commit nhưng tải lại thất bại: `removed=true`, `cart=null`, `refreshRequired=true`; frontend xác nhận đã xóa và cho tải lại, không hiển thị giỏ rỗng giả hoặc gửi lại thao tác xóa.

## Quy tắc xử lý

- Một dòng cho mỗi cặp Customer/SKU. Add cộng số lượng vào dòng đã có; update thay số lượng.
- UC10.1 xem giỏ chỉ đọc dữ liệu; giá hiện tại đã gồm VAT, không tính phí giao hàng/ưu đãi tại đây.
- UC10.2 lưu mã request theo Customer trong cùng transaction với thay đổi giỏ. Gửi lại cùng mã và nội dung chỉ đọc giỏ; tái sử dụng mã cho nội dung khác trả 409. Không xóa receipt theo thời gian nếu vẫn cần bảo đảm retry không cộng lần nữa.
- UC10.3 kiểm tra dòng thuộc Customer trước khi tra cứu SKU. Số lượng không đổi không ghi database. Khi số lượng thay đổi, phiên bản không khớp trả 409 và yêu cầu tải lại. Mỗi lần ghi dùng UUID phiên bản mới, kể cả khi dòng SKU được xóa rồi thêm lại.
- Trên trang giỏ, số lượng tự lưu sau 400ms ngừng chỉnh; không cần nút Cập nhật. Lưu thất bại trả ô số lượng về giá trị đã lưu và hiển thị lỗi.
- Số lượng phải là số nguyên dương và tổng số lượng của dòng không vượt tồn kho hiện tại. Từ chối SKU/product/category/brand không còn được công khai/bán.
- Giá lấy từ hm_sku, tồn kho lấy từ hm_inventory.available_quantity. Không lưu snapshot giá trong cart.
- Khi sản phẩm ngừng bán hoặc tồn kho giảm, vẫn hiển thị dòng giỏ kèm cảnh báo và cho phép xóa.
- Khóa tài khoản để tuần tự hóa các thao tác cùng giỏ. Add/update và việc đọc kết quả nằm cùng transaction: lỗi lưu/đọc rollback cả giỏ lẫn receipt. Xóa được commit trong CartRemovalService trước khi đọc kết quả: lỗi xóa rollback, lỗi đọc sau xóa chỉ yêu cầu refresh theo 10.4.0.E3. Không cập nhật tồn kho hoặc tạo order.
- Tiền tính bằng BigDecimal ở server; frontend format bằng BigInt.
- Session hết hạn trả 401 và chuyển Customer về đăng nhập; cart lưu trong database vẫn được giữ.

## Điểm nối UC-14

Theo lựa chọn của người dùng, chỉ chuẩn bị bước chuyển, không triển khai đặt hàng UC14. Nút Thanh toán mở `/checkout` khi giỏ hợp lệ và không có số lượng đang chờ lưu. Trang này yêu cầu Customer và đọc lại giỏ từ server, cảnh báo nếu giỏ rỗng/không còn hợp lệ, hiển thị tạm tính và thông báo UC14 đang chờ tích hợp. Không có nút tạo đơn hoặc API tạo đơn giả. UC14 thật phải kiểm tra lại eligibility, giá, phí giao hàng, ưu đãi và stock.

Đối chiếu theo bốn UC10.1–10.4 người dùng gửi. Danh mục ACTIVE và cây cha công khai được kiểm tra thống nhất với UC08/BR44. Các BR được viện dẫn nhưng chưa cung cấp nguyên văn vẫn cần đối chiếu tài liệu chính thức khi bàn giao.

## Migration và kiểm thử

V12 thêm version UUID vào hm_cart và bảng hm_cart_add_request; không sửa V1–V11. Backend và frontend phải được cập nhật cùng nhau vì PUT nay cần expectedVersion và POST cần Idempotency-Key. Không dùng Flyway repair để bỏ qua lịch sử V8 khác nhau trên database chung; database mới chạy được chuỗi migration trong source không có nghĩa database chung đã đồng bộ.

CartTransactionIntegrationTest kiểm tra PostgreSQL thật: rollback add/receipt khi lỗi đọc, xóa commit trước lỗi tải lại, rollback toàn bộ khi xóa nhiều dòng gặp lỗi, hai request thêm đồng thời và hai update từ một revision. Các test tích hợp phải dùng database test riêng, không chạy lên database chung.

## File chính

- Backend: cart/CartController.java, cart/CartService.java, cart/CartRemovalService.java, cart/CartRepository.java; SecurityConfig.java.
- Frontend: features/cart/CartPage.jsx, features/cart/api.js, features/cart/cart.css.
- Nối UC-09: ProductDetailPage.jsx và app/App.jsx.
- Kiểm thử: CartSecurityTest.java, CartIntegrationTest.java, CartPage.test.jsx, ProductDetailPage.test.jsx.
