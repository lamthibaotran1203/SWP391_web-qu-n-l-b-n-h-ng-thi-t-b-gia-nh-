# Cách đọc code UC08, UC09, UC10

## Ba lớp chính

- Controller nhận request HTTP, kiểm tra kiểu dữ liệu và trả response.
- Service kiểm tra điều kiện nghiệp vụ, gọi Repository và chuẩn bị kết quả.
- Repository chứa SQL và chuyển từng dòng dữ liệu thành DTO.

Constructor injection truyền đối tượng cần dùng qua hàm khởi tạo. Spring tạo và nối các đối tượng có `@RestController`, `@Service`, `@Repository`.

DTO dạng `record` chỉ chứa dữ liệu. Giữ tên trường DTO để frontend nhận đúng response như trước.

## UC08: danh sách, tìm kiếm, danh mục

Đọc theo thứ tự `CatalogueBrowseController` → `CatalogueBrowseService` → `CatalogueBrowseRepository` / `PublicCategoryRepository`.

Trong `browse()`: kiểm tra trang → chuẩn hóa từ khóa → kiểm tra sort và tồn kho → kiểm tra giá → kiểm tra danh mục/thương hiệu → đếm kết quả → chọn trang hợp lệ → lấy sản phẩm.

Các hàm `validatePagination`, `normalizeKeyword`, `validateOptions`, `validatePriceRange`, `validateReferences` chia rõ từng điều kiện. Vòng `for` trong `categories()` chọn danh mục con hoặc danh mục gốc.

`grouped()` tạo truy vấn với tham số `?`, không ghép trực tiếp dữ liệu người dùng vào SQL. `keywordPattern()` xử lý ký tự tìm kiếm đặc biệt; `orderBy()` chỉ chọn SQL từ danh sách cố định; `mapProduct()` đọc một dòng kết quả.

SQL `WITH RECURSIVE` vẫn giữ để duyệt danh mục nhiều cấp. Phần đầu lấy gốc ACTIVE, phần sau lấy con ACTIVE. Đây là logic cây cần thiết, không đổi thành danh sách chỉ có một cấp để rút ngắn code.

## UC09: chi tiết sản phẩm

Đọc `CatalogController.productDetail()` → `CatalogQueryService.productDetail()` → `CatalogRepository.productDetail()`.

Service lấy SKU được bán công khai trước; nếu không có thì trả lỗi 404 và không đọc mô tả. Repository đọc mô tả và gom ảnh bằng `collectProductImages()`: ảnh bìa trước, ảnh SKU sau, bỏ ảnh trống và ảnh trùng.

## UC10: giỏ hàng

Đọc `CartController` → `CartService` → `CartRepository`.

- `view()`: xác định khách hàng → lấy các dòng với giá/tồn kho hiện tại → tính tổng.
- `save()`: khóa bản ghi khách hàng → kiểm tra số lượng → tính số lượng cần lưu → kiểm tra SKU/tồn kho → lưu → đọc lại giỏ.
- Tham số `add=true` cộng vào số lượng cũ; `add=false` thay bằng số lượng mới.
- `remove()`: chỉ xóa dòng thuộc khách hàng đang đăng nhập → đọc lại giỏ.
- `readCart()` dùng vòng `for` cộng thành tiền và kiểm tra mọi dòng trước khi cho checkout.

`@Transactional` bao trọn thao tác thay đổi: lỗi khi lưu hoặc đọc lại phải thoát ra để Spring rollback. Không bắt lỗi trong Repository rồi giả vờ lưu thành công. Khóa khách hàng giữ nguyên để tránh hai request cùng ghi đè giỏ.

Giỏ hàng không đặt giữ tồn kho và không tạo đơn hàng. `CartRepository` vẫn dùng `JdbcTemplate` như trước; việc tách lớp không yêu cầu đổi sang JPA.

## Kiểm tra sau refactor

`CartServiceTest` kiểm tra nghiệp vụ không cần kết nối database. Các test Service và WebMvc kiểm tra thêm validation, quyền Customer, CSRF và response lỗi. Test tích hợp PostgreSQL vẫn cần database có lịch sử migration hợp lệ; unit test qua không có nghĩa lỗi V8 đã được xử lý.

Code được ghép lại từ stash vào develop; các stash được giữ làm bản dự phòng. Không thay API, số tiền, điều kiện tồn kho hay cơ chế tự cập nhật số lượng trên frontend trong lần refactor này.
