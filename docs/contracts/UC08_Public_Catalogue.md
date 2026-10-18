# UC-8.1 / UC-8.2 / UC-8.3 — Catalogue public

## Giá và đơn vị hiển thị

`hm_sku.selling_price` được cấu hình là giá bán cuối cùng đã bao gồm VAT, nguyên VND. API không cộng VAT lần nữa và không tính thuế ngược từ giá bán. Bộ phận quản trị chịu trách nhiệm nhập đúng giá đã gồm VAT; migration không thay đổi số tiền đã lưu. Frontend thông báo quy ước này ở catalogue và màn hình nhập giá.

Catalogue public hiển thị một thẻ cho mỗi Product, không đếm lặp theo SKU. Khoảng giá min–max, số phiên bản và tổng availableQuantity được tính trên các SKU public phù hợp tiêu chí hiện tại. Nếu chỉ có một mức giá, hiển thị một giá. Phân trang và totalElements đếm Product; price sort dùng mức giá thấp nhất trong các SKU phù hợp. Tìm kiếm kiểm tra tên Product, SKU và thương hiệu; ký tự `%`/`_` được tìm theo nghĩa đen.

## UC-8.1 — View Product Catalogue

- `/products` dành cho Guest/Customer; không yêu cầu đăng nhập.
- Truy vấn Product ACTIVE chưa xóa, SKU ACTIVE chưa xóa, brand chưa xóa và category public.
- Tải danh sách theo trang, có ảnh đại diện, tên, brand/category, giá và tồn kho.
- Phân biệt catalogue rỗng với search không khớp; lỗi tải có Retry.
- Dùng trang server trả về để hiển thị và chuyển trang, kể cả khi số trang giảm.
- Chọn Product tới `/products/{productId}` trực tiếp (UC-09); không gọi validation trung gian.

## UC-8.2 — Search Products

- Keyword được submit bằng nút Tìm kiếm/Enter. Gợi ý riêng từ 2 ký tự, debounce 300ms, tối đa 5 Product; không thay danh sách chính.
- Category/brand/availability áp dụng ngay khi chọn. Khoảng giá áp dụng khi rời ô nhập hoặc submit. Thay filter/sort reset page=0.
- Criteria lưu trong URL, được giữ khi phân trang, Retry, mở chi tiết rồi quay lại; Xóa đưa về catalogue mặc định.
- Supported: `search` tối đa 100 ký tự; `brandId`, `categoryId`; minPrice/maxPrice nguyên VND không âm tối đa 18 chữ số; availability `any|in_stock|out_of_stock`; sort `newest|oldest|name_asc|name_desc|price_asc|price_desc`; page>=0; size=1..100.
- ID reference không hợp lệ/không public, filter/sort không hỗ trợ, tham số lặp hoặc khoảng giá sai trả 400 kèm thông báo cụ thể. Lỗi database trả 503 để Retry.
- Availability được tính trên các SKU phù hợp tiêu chí: Product có tổng stock>0 là còn hàng; tổng=0 là hết hàng.

## UC-8.3 — View Product Categories

- `/categories` tải danh mục gốc; mở rộng tải các con trực tiếp; hỗ trợ nhiều cấp, tên/mô tả/ảnh tùy chọn, empty/loading/error/retry cho danh sách gốc và con.
- Chọn category truyền `categoryId` qua `/products?categoryId=...`; search gồm category đã chọn và các descendant public.
- BR-44 (người dùng cung cấp ngày 18/10/2026): chỉ danh mục ACTIVE được hiển thị cho Guest/Customer; danh mục INACTIVE bị loại khỏi danh sách public và không được chấp nhận làm bộ lọc public product browsing. API từ chối categoryId không public bằng HTTP 400 trước khi truy vấn sản phẩm.
- Quy tắc bổ sung của cây trong bản triển khai: category chưa xóa và mọi ancestor cũng public. Gốc là parent_id NULL; orphan/cycle bị ẩn. Việc ẩn nhánh con khi parent bị ẩn là lựa chọn triển khai cây, không phải yêu cầu được ghi trực tiếp trong BR-44. Danh mục rỗng vẫn public nếu thỏa điều kiện; visibility không phụ thuộc số sản phẩm.
- Schema mới: status ACTIVE/INACTIVE và image_url tùy chọn. Migration giữ nguyên danh mục cũ bằng default ACTIVE.

## API và tương thích

| API | Kết quả |
| --- | --- |
| GET `/api/v1/catalog/products` | `{items,page,size,totalElements}` theo Product |
| GET `/api/v1/catalog/public-categories` | Danh mục public phẳng với `{id,parentId,name,description,imageUrl,hasChildren}` |
| GET `/api/v1/catalog/public-categories?roots=true` | Chỉ các gốc public |
| GET `/api/v1/catalog/public-categories?parentId=ID` | Các con trực tiếp của parent public |

API SKU cũ và `/categories` dùng cho management vẫn giữ contract. UC-09 cũng kiểm tra category public để không lộ sản phẩm khi một category/ancestor bị ẩn. Toàn bộ public reads là GET trong transaction read-only; không thay đổi Product/stock/cart.

## Migration và chạy thử

Migration mới: `V11__public_category_browsing.sql`. Sau khi pull develop, source đã có V9 đổi role sang ADMIN và V10 bổ sung ràng buộc danh mục; phần tích hợp giữ nguyên role ADMIN. Lần kiểm tra database trước ghi nhận V8__use_shared_order_tables.sql, khác V8__add_brand_name_lower_unique_index.sql trên GitLab. Cần nhóm thống nhất V8 gốc trước khi chạy Flyway lên V11. Không repair/xóa history hoặc sửa migration cũ để bỏ qua. Lần refactor này không chạy migration hoặc thay đổi database chung.

Khởi động lại backend rồi frontend. Kiểm tra `/categories`, mở cây con, chọn category để vào search; thử keyword, còn hàng/hết hàng, giá, sort, Next/Previous, Xóa và Retry.

Các test: `CatalogueBrowseServiceTest`, `CatalogueBrowseSecurityTest`, `CatalogueBrowseIntegrationTest`, `ProductPages.test.jsx`, `ProductCategoriesPage.test.jsx`. PostgreSQL đã được kiểm tra bằng transaction rollback cho migration, grouping, price range, availability, category descendants và chặn category ẩn trong detail; không lưu schema/dữ liệu thử vào database chung.
