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
- `addItem()`: khóa Customer → kiểm tra mã request đã xử lý chưa → kiểm tra số lượng/SKU/tồn kho → cộng số lượng → lưu giỏ và receipt → đọc kết quả. Receipt giúp thử lại cùng request không cộng hai lần.
- `updateItem()`: khóa Customer → tìm dòng thuộc Customer → kiểm tra số lượng → nếu không đổi thì chỉ đọc; nếu đổi thì kiểm tra version/SKU/tồn kho → lưu số lượng thay thế → đọc kết quả. Version là mã UUID của lần ghi trước, không phải số lượng.
- `remove()`: gọi CartRemovalService để xóa và commit → đọc lại giỏ; nếu chỉ phần đọc lỗi thì vẫn xác nhận đã xóa và cho refresh.
- `readCart()` dùng vòng `for` cộng thành tiền và kiểm tra mọi dòng trước khi cho checkout.

`@Transactional` bao trọn add/update: lỗi lưu hoặc đọc kết quả phải thoát ra để rollback. Riêng remove có transaction ở CartRemovalService; Spring commit khi lời gọi sang Service này trả về, trước khi CartService đọc giỏ mới. Tách thành hai Service để tránh gọi hàm transactional trong cùng đối tượng (Spring không đi qua proxy trong trường hợp đó). Lỗi xóa vẫn rollback; lỗi tải lại sau commit không được báo xóa thất bại.

Controller gọi rõ từng hàm Service và dùng @ExceptionHandler cho lỗi database; không còn Supplier/lambda hay cờ true/false để phân biệt thêm/sửa. SQL version và receipt nằm trong Repository. Đây là phần kiểm soát đồng thời cần giữ, không bỏ để làm code ngắn hơn.

Giỏ hàng không đặt giữ tồn kho và không tạo đơn hàng. `CartRepository` vẫn dùng `JdbcTemplate` như trước; việc tách lớp không yêu cầu đổi sang JPA.

## Kiểm tra sau refactor

`CartServiceTest` kiểm tra nghiệp vụ không cần kết nối database. Các test Service và WebMvc kiểm tra thêm validation, quyền Customer, CSRF và response lỗi. Test tích hợp PostgreSQL vẫn cần database có lịch sử migration hợp lệ; unit test qua không có nghĩa lỗi V8 đã được xử lý.

Trong lần hoàn thiện theo đặc tả mới, POST bổ sung Idempotency-Key, PUT bổ sung expectedVersion và DELETE bổ sung kết quả xác nhận xóa/tải lại. Frontend đã cập nhật tương ứng; số tiền và cơ chế tự lưu sau 400ms vẫn giữ. /checkout chỉ là bước bàn giao chờ UC14, không tạo đơn hàng.
