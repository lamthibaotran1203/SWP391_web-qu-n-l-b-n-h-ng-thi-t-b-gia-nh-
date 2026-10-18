# UC-10 — Manage Shopping Cart

Customer đã đăng nhập có thể thêm SKU từ UC-09, xem giỏ tại `/cart`, cập nhật số lượng và xóa một hoặc nhiều SKU. Giỏ lưu theo tài khoản trong `app.hm_cart` (migration V4 đã có sẵn).

## API

| Method | Endpoint | Request |
| --- | --- | --- |
| GET | `/api/v1/cart` | Không có body |
| POST | `/api/v1/cart/items` | `{ "skuId": 1, "quantity": 2 }` |
| PUT | `/api/v1/cart/items/{skuId}` | `{ "quantity": 3 }` |
| DELETE | `/api/v1/cart/items` | `{ "skuIds": [1, 2] }` |

Mọi endpoint yêu cầu role CUSTOMER; thao tác ghi yêu cầu CSRF. Chủ giỏ lấy từ phiên đăng nhập, không nhận userId từ client. Response sử dụng ApiEnvelope: items, subtotal dạng chuỗi số nguyên VND, checkoutEligible. Mỗi item gồm thông tin Product/SKU, quantity, unitPrice, lineTotal, availableQuantity, eligible và available.

## Quy tắc xử lý

- Một dòng cho mỗi cặp Customer/SKU. Add cộng số lượng vào dòng đã có; update thay số lượng.
- Trên trang giỏ, số lượng tự lưu sau 400ms ngừng chỉnh; không cần nút Cập nhật. Lưu thất bại trả ô số lượng về giá trị đã lưu và hiển thị lỗi.
- Số lượng phải là số nguyên dương và tổng số lượng của dòng không vượt tồn kho hiện tại. Từ chối SKU/product/category/brand không còn được công khai/bán.
- Giá lấy từ hm_sku, tồn kho lấy từ hm_inventory.available_quantity. Không lưu snapshot giá trong cart.
- Khi sản phẩm ngừng bán hoặc tồn kho giảm, vẫn hiển thị dòng giỏ kèm cảnh báo và cho phép xóa.
- Ghi trong transaction; khóa tài khoản để tuần tự hóa các thao tác cùng giỏ; lỗi lưu rollback. Không cập nhật tồn kho, reserve stock hoặc tạo order.
- Tiền tính bằng BigDecimal ở server; frontend format bằng BigInt.
- Session hết hạn trả 401 và chuyển Customer về đăng nhập; cart lưu trong database vẫn được giữ.

## Điểm nối UC-14

Repository hiện chưa có route/API Checkout UC-14. Nút Thanh toán hiển thị chưa sẵn sàng. Khi UC-14 được triển khai, nối nút tới route chính thức; UC-14 phải xác thực lại eligibility, giá và stock trước khi tạo order. Cart không chịu trách nhiệm giữ tồn kho.

Đặc tả viện dẫn BR-10, BR-11, BR-12, BR-16, BR-17 nhưng chưa cung cấp nội dung các BR này trong yêu cầu; cần đối chiếu tài liệu BR chính thức khi bàn giao.

## File chính

- Backend: cart/CartController.java, cart/CartService.java; SecurityConfig.java.
- Frontend: features/cart/CartPage.jsx, features/cart/api.js, features/cart/cart.css.
- Nối UC-09: ProductDetailPage.jsx và app/App.jsx.
- Kiểm thử: CartSecurityTest.java, CartIntegrationTest.java, CartPage.test.jsx, ProductDetailPage.test.jsx.
