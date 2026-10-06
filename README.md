# SWP391_Group03 - Household Appliances Management System

## Stack

Last recorded runtime state: **R1 walking skeleton locally verified**. The Supabase JDBC/Flyway
connection was verified on 2026-09-14; the first GitLab-hosted pipeline and full browser
login proof against Supabase were still pending. No business use case is claimed complete.

## Requirements documents

Read [PROJECT_STATUS.md](PROJECT_STATUS.md) before using local planning files. The team
submitted a 35-UC Google Sheet Backlog and RDS to the lecturer, then received substantial
feedback. The live RDS is being edited. The 37-UC
`household-goods-sales-planning-handoff/` folder is an older, unapproved proposal and is
useful only as reference material until the team records revised decisions. Current code
and migrations show implementation status, not the full accepted scope.

For the current Use Case Diagram, Screen Flow, ERD, database-schema, and lecturer-feedback
review, read
[docs/HomeMart_RDS_Consolidated_Review.docx](docs/HomeMart_RDS_Consolidated_Review.docx).
The document uses one UC Diagram specification per actor and records the proposed
`include`/`extend` relationships in prose and tables. The 35-UC Google Sheet remains the
canonical register. Apply UC name, actor, scope, and iteration corrections in the Sheet
before propagating them to the RDS, diagrams, authorization matrix, code, or tests.

`master` là bản chạy ổn định. Planning và tài liệu làm việc của nhóm nằm ở `develop`.

## Chạy project

No Supabase browser key is required. This application uses Supabase only as PostgreSQL, so
later configuration needs the backend JDBC URL, database username and database password.
React must never receive those values.

## Pinned toolchain

| Tool | Version |
| --- | --- |
| Java | 21 |
| Maven | 3.9.16 through Maven Wrapper |
| Node.js | 24.19.0 |
| npm | 11.x |
| Spring Boot | 3.5.16 |
| React | 19.3.0 |
| Vite | 8.2.2 |
| Local/CI/Supabase PostgreSQL | 17 (Supabase currently 17.6) |

## Repository map

```text
backend/
  src/main/java/com/swp391/householdgoods/
    common/          stable API errors/envelopes and request trace IDs
    config/          framework configuration and typed environment settings
    security/        session, CSRF, login/logout and current-user boundary
    identity/
      domain/        account entity and enums
      application/   transactions and application flow
      infrastructure/JPA repository
  src/main/resources/
    db/migration/    append-only Flyway migrations
frontend/
  src/
    api/             only shared HTTP/CSRF client
    app/             router and top-level providers
    auth/            server-backed session state
    features/        approved feature modules only
    pages/           route-level UI
    components/      small reusable UI pieces
docs/                architecture, later RDS/SDS/diagrams/evidence
scripts/             OS-neutral Node launch/build helpers
household-goods-sales-planning-handoff/
                      historical 37-UC proposal; see PROJECT_STATUS.md
```

Read [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) before changing the login flow. It contains
the exact request path to explain during defense.

## Fresh clone setup

### 1. Prerequisites on macOS/Linux

Install Git, a JDK 21 distribution, Node.js 24.19.x with npm 11, and Docker Desktop or Docker
Engine with Compose. Maven does not need to be installed globally.

Verify from the repository root:

```bash
java -version
node --version
npm --version
docker compose version
```

`java -version` must show 21. Node must show 24 and npm must show 11.

### 1. Prerequisites on Windows PowerShell

Install Git, a JDK 21 distribution, Node.js 24.19.x with npm 11, and Docker Desktop. Enable
Linux containers in Docker Desktop. Open a new PowerShell window and verify:

```powershell
java -version
node --version
npm --version
docker compose version
```

If Java reports another version, set `JAVA_HOME` to the JDK 21 directory and reopen the
terminal. Do not install Maven separately; `backend\mvnw.cmd` supplies the pinned version.

### 2. Clone and install frontend dependencies

Clone the team repository:

```text
git clone https://gitlab.com/031153437/SWP391.git
cd SWP391
npm --prefix frontend ci
```

Tạo file `.env` và copy từ .env.example qua

Nhóm đang dùng chung database Supabase. Điền thông tin sau vào `.env` và lấy password từ
người giữ tài khoản Supabase:

```dotenv
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.hqelyoqvqofqzvdcwvna
DB_PASSWORD=<database-password>
SESSION_COOKIE_SECURE=false
```

Chạy backend và frontend ở hai terminal:

```text
# Terminal 1
npm run backend:dev

# Terminal 2
npm run frontend:dev
```

Mở [http://localhost:5173](http://localhost:5173).

Không commit file `.env`. Khi deploy bằng HTTPS, đổi `SESSION_COOKIE_SECURE` thành `true`.

## Manager đầu tiên

Nếu database chưa có tài khoản Manager, thêm vào `.env` trước khi chạy backend:

```dotenv
BOOTSTRAP_MANAGER_EMAIL=manager@example.com
BOOTSTRAP_MANAGER_PASSWORD=<password>
BOOTSTRAP_MANAGER_FULL_NAME=Quản lý cửa hàng
BOOTSTRAP_MANAGER_UPDATE_EXISTING=false
```

Tài khoản chỉ được tạo khi chưa tồn tại. Chạy lại backend không đổi password cũ. Nếu cần đồng
bộ tên hiển thị của Manager đã tồn tại với `BOOTSTRAP_MANAGER_FULL_NAME`, chỉ bật
`BOOTSTRAP_MANAGER_UPDATE_EXISTING=true` khi chủ động muốn cập nhật. Mặc định là `false`; tùy
chọn này chỉ cập nhật tên, không thay đổi mật khẩu hoặc thông tin đăng nhập khác.

Người dùng đã đăng nhập cũng có thể đổi tên hiển thị trực tiếp trên trang chủ bằng nút
**Đổi tên hiển thị**. Thay đổi này được lưu ngay vào tài khoản; không cần sửa `.env` hay khởi
động lại backend.

## Database local

Docker chỉ cần khi muốn chạy database local hoặc backend test. Để trống ba biến `DB_*` trong
`.env`, mở Docker Desktop rồi chạy:

```text
npm run db:up
npm run backend:dev
```

Database local dùng cổng `55432`. Dừng container bằng `npm run db:down`.

## Các lệnh chính

```text
npm run frontend:dev      # chạy frontend
npm run backend:dev       # chạy backend
npm run frontend:check    # lint + test + build frontend
npm run backend:test      # test backend, cần database local
npm run build             # đóng gói toàn bộ project
npm run db:up             # bật PostgreSQL local
npm run db:down           # tắt PostgreSQL local
```

## Tra cứu đơn hàng (UC-18)

Tài khoản `STAFF` và `MANAGER` có thể mở mục **Tra cứu đơn hàng** trên trang chủ hoặc vào
`/staff/orders`. Màn hình hỗ trợ tìm theo mã đơn/thông tin khách hàng, lọc theo kênh, trạng
thái đơn, trạng thái thanh toán và ngày đặt; chọn mã đơn để xem thông tin khách hàng, sản phẩm
và tổng tiền. API chỉ cho phép nhân viên/quản lý truy cập:

- `GET /api/v1/staff/orders` — tìm kiếm và phân trang.
- `GET /api/v1/staff/orders/{id}` — xem chi tiết đơn.

Schema đơn hàng được tạo bằng Flyway migration. UC-18 chỉ tra cứu; đơn hàng sẽ xuất hiện khi
luồng tạo đơn được triển khai và ghi dữ liệu vào database.

Trước khi push code nên chạy:

```text
npm run frontend:check
npm run backend:test
npm run build
```

File sau khi build:

```text
backend/target/household-goods-backend-0.1.0-SNAPSHOT.jar
```

## Cấu trúc

```text
backend/     Spring Boot API, security, service, repository và migration
frontend/    React pages, auth state và API client
scripts/     lệnh chạy/build dùng chung cho macOS và Windows
```

Luồng code chung:

```text
React page → API client → Controller → Service → Repository → PostgreSQL
```

Controller nhận/trả DTO, business rule và transaction nằm trong service, repository chỉ xử
lý truy vấn. Không trả JPA entity thẳng ra API.

Migration nằm trong `backend/src/main/resources/db/migration/`. Project đang dùng Flyway để
quản lý schema cho cả Supabase và PostgreSQL local. Migration đã chạy rồi thì không sửa; cần
đổi database thì tạo file version mới.

## Nếu chạy lỗi

- Sai Java version: kiểm tra `java -version` và `JAVA_HOME`.
- Frontend thiếu package: chạy lại `npm --prefix frontend ci`.
- Không vào được Supabase: kiểm tra internet và ba biến `DB_*` trong `.env`.
- Login trên localhost không giữ session: kiểm tra `SESSION_COOKIE_SECURE=false`.
- Backend test không vào được database: mở Docker Desktop và chạy `npm run db:up`.

Online payment chưa làm. SMTP chỉ cần cấu hình khi bắt đầu phần gửi email.

## Shared schema migration

Supabase was upgraded through Flyway V5 on 2026-10-05. It contains 24 business tables plus
Flyway history. Existing V1–V3 and product data were preserved; incomplete catalogue items
are INACTIVE. See [shared catalogue contract and verification](docs/contracts/Catalog_Shared_Contract.md).

On macOS/Linux or Windows PowerShell, from repository root with JDK 21 and the ignored
local DB environment configured:

```text
npm run build
npm run db:info
npm run db:migrate
npm run db:info
```

Expected after all current migrations: Flyway current version 5, pending migrations 0. The schema command does not start
the web server or bootstrap accounts. Never manually apply the historical diagram SQL to
shared Supabase. Run integration tests against isolated PostgreSQL, not the shared database.

## Shared catalogue foundation

Read [the catalogue contract](docs/contracts/Catalog_Shared_Contract.md) before implementing
product, cart, order or stock code. Supabase is now at V5. Live prices belong to `hm_sku` and
live quantities to `hm_inventory`; old product fields are immutable `legacy_*` archives.
The public read API is `/api/v1/catalog/skus`; management UC-13 remains to be implemented.

```text
npm run frontend:check
npm run backend:verify:postgres
npm run db:seed:catalog
```

The verification command creates/removes an isolated PostgreSQL Docker database and builds
with Java 21; it never uses Supabase credentials. Demo seed is explicit/idempotent and targets
the configured database. The shared frontend catalogue client is in `frontend/src/features/catalog/api.js`.
