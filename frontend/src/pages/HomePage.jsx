import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { AppFooter, AppHeader } from '../components/AppShell'
import { ConnectionStatus } from '../components/ConnectionStatus'

export function HomePage() {
  const { user, connection, reload } = useAuth()

  return (
    <main className="home-page">
      <AppHeader variant="dark" />
      <section className="hero-section">
        <div className="container hero-grid">
          <div className="hero-content">
            <p className="eyebrow">HomeMart · Gia Dụng Việt</p>
            <h1>Gia dụng gọn nhà, vận hành gọn hệ thống.</h1>
            <p className="hero-copy">
              Nền tảng bán hàng và quản lý tồn kho cho một cửa hàng gia dụng Việt Nam.
              Mọi phiên làm việc được xác thực tập trung và dữ liệu được quản lý thống nhất.
            </p>

            <div className="hero-trust-row" aria-label="Cam kết của HomeMart">
              <span>✓ Sản phẩm rõ nguồn gốc</span>
              <span>✓ Quản lý tồn kho thống nhất</span>
              <span>✓ Hỗ trợ minh bạch</span>
            </div>

            <div className="hero-actions">
              <Link className="btn btn-warning btn-lg" to="/products">Khám phá sản phẩm</Link>
              {user.authenticated && ['STAFF', 'ADMIN'].includes(user.role) ? (
                <Link className="btn btn-outline-light btn-lg" to={user.role === 'ADMIN' ? '/admin/products' : '/staff/orders'}>
                  Mở khu vực làm việc
                </Link>
              ) : !user.authenticated ? (
                <Link className="btn btn-outline-light btn-lg" to="/login">Đăng nhập</Link>
              ) : null}
            </div>
          </div>

          <aside className="status-panel" aria-label="Trạng thái hệ thống">
            <p className="eyebrow">Hệ thống cửa hàng</p>
            <h2>Sẵn sàng cho phiên làm việc.</h2>
            <ConnectionStatus connection={connection} onRetry={reload} />
            <p className="status-note">
              Kết nối máy chủ được kiểm tra trước khi bắt đầu hoặc khôi phục phiên đăng nhập.
            </p>
          </aside>
        </div>
      </section>
      <section className="home-benefits">
        <div className="container benefit-grid">
          <article><span>01</span><h2>Danh mục dễ tìm</h2><p>Tìm theo tên, thương hiệu, danh mục và khoảng giá trong một màn hình.</p></article>
          <article><span>02</span><h2>Dữ liệu nhất quán</h2><p>Giá bán và số lượng tồn được lấy từ nguồn dữ liệu tập trung của cửa hàng.</p></article>
          <article><span>03</span><h2>Vận hành rõ ràng</h2><p>Nhân viên và quản lý truy cập đúng công cụ theo vai trò được phân công.</p></article>
        </div>
      </section>
      <AppFooter />
    </main>
  )
}
