# SWP391_Group03 - Household Appliances Management System

## Stack

Current integration: UC-05, separate UC-13 management pages and the initial UC-04 Customer
profile slice are integrated with `develop` e333c0b (registration and public catalog updates).
Local verification on 2026-10-12: 62 frontend tests plus lint/build, 90 backend tests against
isolated PostgreSQL, and Java 21 packaging. UC-04 delivery addresses remain pending.
Supabase runs PostgreSQL17 with Flyway migrations V1–V7. Hosted CI and full UC sign-off remain separate.

## Requirements documents

The team's 35-UC Google Sheet Backlog is the canonical register for ownership, names, actors
and scope. The submitted RDS is comparison evidence; the live RDS is work in progress.
Record requirement decisions in the Backlog before propagating them to code or documents.
Current code and migrations show implemented behavior, not the complete accepted scope.
Personal learning notes, agent context and unapproved planning/review drafts stay local and
are ignored by Git. Shared API contracts are in `docs/contracts/`.

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
```

Authentication uses server sessions and CSRF. Read the security package and the UC-05 flow
below before changing login or password behavior.

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

## Admin đầu tiên

Nếu database chưa có tài khoản Admin, thêm vào `.env` trước khi chạy backend:

```dotenv
BOOTSTRAP_ADMIN_EMAIL=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=<password>
BOOTSTRAP_ADMIN_FULL_NAME=Quản trị viên cửa hàng
BOOTSTRAP_ADMIN_UPDATE_EXISTING=false
```

Tài khoản chỉ được tạo khi chưa tồn tại. Chạy lại backend không đổi password cũ. Nếu cần đồng
bộ tên hiển thị của Admin đã tồn tại với `BOOTSTRAP_ADMIN_FULL_NAME`, chỉ bật
`BOOTSTRAP_ADMIN_UPDATE_EXISTING=true` khi chủ động muốn cập nhật. Mặc định là `false`; tùy
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

Tài khoản `STAFF` và `ADMIN` có thể mở mục **Tra cứu đơn hàng** trên trang chủ hoặc vào
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
are INACTIVE.

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

Preserve the shared catalogue boundaries when implementing product, cart, order or stock
code. Supabase is now at V5. Live prices belong to `hm_sku` and
live quantities to `hm_inventory`; old product fields are immutable `legacy_*` archives.
The public read API is `/api/v1/catalog/skus`; UC-13 management uses the separate routes documented below.

```text
npm run frontend:check
npm run backend:verify:postgres
npm run db:seed:catalog
```

The verification command creates/removes an isolated PostgreSQL Docker database and builds
with Java 21; it never uses Supabase credentials. Demo seed is explicit/idempotent and targets
the configured database. The shared frontend catalogue client is in `frontend/src/features/catalog/api.js`.

## UC-05 — Change own password

AnNP owns UC-05. The canonical Backlog row 6 records the actor (Authenticated User) and
user-confirmed policy/session decisions on 2026-10-06. The live RDS wording/BR references
still need synchronization; this implementation does not claim the other owned UCs are done.

Open the avatar menu → **Đổi mật khẩu**, or `/account/password`, while logged in. The Bootstrap
form calls `POST /api/v1/auth/password` with JSON `currentPassword`, `newPassword`,
`confirmation`, the server session cookie and CSRF header. No target account ID is accepted.
CUSTOMER, STAFF and ADMIN can change only their own active account.

The service verifies current password, requires matching confirmation and a different new
password, then stores a salted BCrypt hash atomically. Policy: 8–64 Unicode code points AND
at most 72 UTF-8 bytes; preserve spaces/case, with no mandatory composition or periodic rotation.
Success returns `data.changed=true`, `data.loginRequired=true`, and a Vietnamese message;
it logs out the submitting session after commit. Other old sessions are rejected at their
next API request (already in-flight requests are not canceled). No new schema/migration is needed.
Bootstrap uses the shared `PasswordPolicy`; registration/reset owners must adopt it during integration.

Failures return the existing safe API problem format: validation 400, missing/expired session
401, invalid CSRF 403, unavailable persistence 503. Failed writes preserve the existing hash
and do not log out a valid submitting session. API responses contain no password/hash/stamp.

Verification, from repository root with Docker and Node 24/npm 11:

```text
npm run frontend:check
npm run backend:verify:postgres
```

Evidence: `PasswordChangeIntegrationTest` exercises all three roles, old sessions, invalid
inputs, UTF-8 limits/spaces, CSRF, anonymous/inactive accounts, concurrent changes, stale JPA
updates and database-write rollback on isolated PostgreSQL. A packaged HTTP smoke with two
sessions also passed on disposable PostgreSQL; browser login, form and direct route refresh were
checked. Temporary smoke containers were removed. Page/client tests cover redirects,
confirmation, server errors, duplicate submission, clearing fields and no automatic POST replay.

Manual demo uses a disposable **local** account: wrong current password must fail without
logging out; matching valid new password succeeds and requires login again; old password fails,
new password succeeds. A second browser session must become anonymous/401 on its next API call.
Do not use a shared Supabase account or real credentials for this demo. Full manual browser
password-change proof and GitLab-hosted pipeline evidence remain pending.

For a direct-link demo, open `/login`, sign in, then enter `/account/password` in the address bar.
Use the same host/port throughout: `http://localhost:5173` for Vite development or
`http://localhost:8080` for the packaged Spring application. Password and profile pages share
the HomeMart header and simple white forms in `frontend/src/styles/account.css`. The password
form includes an optional **Hiện mật khẩu** checkbox. The avatar menu supports keyboard focus,
Escape, outside-click dismissal and logout. Login retains `frontend/src/styles/plain-admin.css`.
UC-13 reuses the team's HomeMart header and accent colour, with restrained controls scoped in
`frontend/src/features/catalog/management/management.css`. Public product pages retain the
teammate's search/view implementation.

## UC-13 — Catalogue management

ADMIN opens **Quản lý sản phẩm** (`/admin/products`). Each task has its own page:

| Page | Client route |
| --- | --- |
| Search/filter the management list | `/admin/products` |
| Create a product with initial manual SKUs | `/admin/products/new` |
| Read product details, SKU prices and stock warnings | `/admin/products/:productId` |
| Edit product metadata/status | `/admin/products/:productId/edit` |
| Add one manual SKU | `/admin/products/:productId/skus/new` |
| Edit one owned SKU/price/threshold/status | `/admin/products/:productId/skus/:skuId/edit` |

The old `/admin/catalog` link redirects to the list. Direct links and refresh work in the
packaged Spring application. No edit forms appear below the management table. Saving returns
to freshly loaded details; failed saves retain entered values. A stale-version error offers
**Làm mới dữ liệu**, which replaces the draft with current data before another save.
Deactivation requires the existing confirmation checkbox. Stock warnings recalculate on detail
reads/refresh. The actor remains ADMIN (RDS Admin mapping); lecturer feedback about separate
screens does not grant STAFF access. This scoped presentation is provisional, not a team-wide
design decision. Public `/products` and `/products/:id` remain owned by their UC implementer.

Read [management APIs, rules and traceability](docs/contracts/UC13_Catalog_Management_Contract.md)
before integrating. Writes use `/api/v1/admin/catalog/products`, master productId and owned skuId,
plus returned productVersion/skuVersion. Product+initial SKUs save atomically; code/barcode conflicts
or database failure leave no partial creation. Existing app.product/hm_sku tables suffice; no new
migration. This UC never changes inventory, order snapshots or legacy_* archives.

Old `/api/v1/products` POST/PUT/DELETE used archive fields and are retired: Admin receives410
CATALOG_WRITE_MOVED; Customer/Staff denied. Public GET routes stay compatible. Barcode is a required
unique internal label, not a manufacturer EAN requirement. Prices are whole VND; no VAT engine added.

`npm run frontend:check` and `npm run backend:verify:postgres` passed with 13 frontend/34 backend tests.
Real browser verified Admin login, creation of two SKUs, low-stock display and duplicate barcode.
Packaged HTTP smoke on local disposable PostgreSQL verified price edits, addSKU, stale409,
deactivation preserving references/public404 and retired410. The old native confirmation stalled
the app browser; the inline checkbox now passed browser verification too: save without confirmation
was blocked, confirmed deactivation succeeded and the SKU/quantity remained. Plain UI was inspected
on desktop and mobile; direct UC05 route refresh passed. RDS updates, hosted CI and team deployment
are not claimed complete.

The explicit catalogue fixture also includes three SUNHOUSE products with real photo URLs from
the manufacturer's [rice cooker](https://sunhouse.com.vn/dien-gia-dung/noi-com-dien/sunhouse-shd8602.html),
[kettle](https://sunhouse.com.vn/dien-gia-dung/am-sieu-toc/sunhouse-shd1182.html) and
[cookware set](https://sunhouse.com.vn/do-gia-dung/bo-noi-nhom/bo-noi-nhom-chong-dinh-day-tu-sunhouse-eco-shg1122mx.html)
pages. Prices and quantities are demonstration values, not current offers. Re-running the fixture
adds missing items without resetting earlier edits; opening inventory has matching stock movements.
Photos need Internet access; management thumbnails show “Chưa có ảnh” if a URL fails.
User-facing labels use “Quản lý sản phẩm”, “Đang bán”, “Ngừng bán”, “Chi tiết”, “Có thể bán”,
“Sắp hết hàng” and “Hết hàng”. Password byte limits remain enforced but are not technical UI copy.


UC-13 screen split verified locally on 2026-10-12: frontend lint/44 tests/build and
PostgreSQL 17 V1–V7 migration, Hibernate validation, 76 backend tests and JAR packaging passed.
Browser checks on that JAR with disposable PostgreSQL covered Admin login, old-link redirect,
direct edit refresh, product save, price save, cancel SKU, separate create form and desktop/mobile
layout (1280/390px, no whole-page overflow or captured console errors). The disposable app/database
were removed. This does not claim hosted CI or new writes to shared Supabase.

## UC-04 — Personal profile (initial slice)

CUSTOMER opens the avatar menu → **Thông tin tài khoản** at `/account/profile`, or the link on Home.
The basic slice reads the current account's email, full name and phone. Email is read-only;
name (2–100 characters after normalization) and optional phone (10 digits starting with 0)
can be saved. Clearing the phone stores NULL. Cancel restores the last loaded/saved values.
Field errors or save failures retain the draft. Successful saves update the displayed session name.

`GET /api/v1/account/profile` returns `{data: {email, fullName, phone}}`.
`PUT /api/v1/account/profile` accepts `{fullName, phone}` with the existing session and CSRF.
The owner comes from the authenticated principal, never a client-supplied account ID/email.
CUSTOMER authorization is enforced by Spring and rechecked in the service. The service locks
the account row, saves only contact fields in one transaction and returns a safe projection.
Credential, role/status, saved addresses and order delivery snapshots are unchanged. Existing
Flyway tables suffice; there is no new migration. The prior name-only `/auth/me` helper stays
compatible for other roles and is not expanded into customer-address access.

This follows UC-04 in the canonical Backlog and live working RDS checked on 2026-10-12.
**UC-04 remains partial:** address list/add/edit/remove/default selection and formal RDS/SDS
evidence are deferred. No placeholder address CRUD or checkout implementation is included.
Run `npm run frontend:check` and `npm run backend:verify:postgres` for validation; do not run
test fixtures or failure triggers against shared Supabase.

Initial UC-04 verification on 2026-10-12 passed frontend lint/52 tests/build and 89 backend
tests with disposable PostgreSQL/JAR packaging. Browser on that JAR verified Customer login
from a direct profile link, name/phone save, updated header, persistence after refresh, invalid
phone feedback/cancel and desktop/mobile layout. No shared Supabase contact data was changed.
