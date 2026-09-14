# SWP391_Group03 - Household Appliances Management System

## Stack

- Backend: Java 21, Spring Boot, Spring Security, JPA, Flyway
- Frontend: React, Vite, Bootstrap
- Database: PostgreSQL 17 trên Supabase

`master` là bản chạy ổn định. Planning và tài liệu làm việc của nhóm nằm ở `develop`.

## Chạy project

Cần cài Java 21, Node.js 24 và npm 11. Maven đã có wrapper nên không cần cài riêng.

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
```

Tài khoản chỉ được tạo khi chưa tồn tại. Chạy lại backend không đổi password cũ.

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
