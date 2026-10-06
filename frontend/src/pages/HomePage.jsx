import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { ConnectionStatus } from '../components/ConnectionStatus'

export function HomePage() {
  const { user, connection, logout, reload } = useAuth()

  return (
    <main>
      <nav className="home-nav container"><Link to="/" className="brand-link">Gia Dụng Việt</Link><Link to="/products">Xem sản phẩm</Link></nav>
      <section className="hero-section">
        <div className="container hero-grid">
          <div>
            <p className="eyebrow">SWP391 · Group 03</p>
            <h1>Gia dụng gọn nhà, vận hành gọn hệ thống.</h1>
            <p className="hero-copy">
              Nền tảng bán hàng và quản lý tồn kho cho một cửa hàng gia dụng Việt Nam.
              Mọi phiên làm việc được xác thực tập trung và dữ liệu được quản lý thống nhất.
            </p>

            {user.authenticated ? (
              <div className="session-panel">
                <div>
                  <span className="label">Phiên đăng nhập</span>
                  <strong>{user.fullName}</strong>
                  <span>{user.email} · {user.role}</span>
                </div>
                <button className="btn btn-outline-light" type="button" onClick={logout}>
                  Đăng xuất
                </button>
              </div>
            ) : (
              <Link className="btn btn-warning btn-lg" to="/login">
                Đăng nhập hệ thống
              </Link>
            )}
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
    </main>
  )
}
