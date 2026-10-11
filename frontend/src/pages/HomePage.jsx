import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { getRoleLabel } from '../auth/role-labels'
import { AppFooter, AppHeader } from '../components/AppShell'
import { ConnectionStatus } from '../components/ConnectionStatus'

export function HomePage() {
  const { user, connection, logout, reload, updateFullName } = useAuth()
  const [editingName, setEditingName] = useState(false)
  const [fullName, setFullName] = useState('')
  const [profileError, setProfileError] = useState('')
  const [savingName, setSavingName] = useState(false)

  async function saveName(event) {
    event.preventDefault()
    setProfileError('')
    setSavingName(true)
    try {
      await updateFullName(fullName)
      setEditingName(false)
    } catch (error) {
      setProfileError(error.message || 'Không thể cập nhật tên hiển thị.')
    } finally {
      setSavingName(false)
    }
  }

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

            {user.authenticated ? (
              <div className="session-panel">
                <div>
                  <span className="label">Phiên đăng nhập</span>
                  <strong>{user.fullName}</strong>
                  <span>{user.email} · {getRoleLabel(user.role)}</span>
                  {editingName ? (
                    <form className="profile-name-form" onSubmit={saveName}>
                      <label className="visually-hidden" htmlFor="profile-full-name">
                        Tên hiển thị mới
                      </label>
                      <input
                        autoFocus
                        className="form-control form-control-sm"
                        id="profile-full-name"
                        maxLength={100}
                        value={fullName}
                        onChange={(event) => setFullName(event.target.value)}
                        required
                      />
                      {profileError && <span className="profile-name-error" role="alert">{profileError}</span>}
                      <div className="profile-name-actions">
                        <button className="btn btn-warning btn-sm" type="submit" disabled={savingName}>
                          {savingName ? 'Đang lưu…' : 'Lưu tên'}
                        </button>
                        <button
                          className="btn btn-outline-light btn-sm"
                          type="button"
                          disabled={savingName}
                          onClick={() => {
                            setEditingName(false)
                            setProfileError('')
                          }}
                        >
                          Hủy
                        </button>
                      </div>
                    </form>
                  ) : (
                    <button
                      className="btn btn-outline-light btn-sm align-self-start mt-2"
                      type="button"
                      onClick={() => {
                        setFullName(user.fullName)
                        setProfileError('')
                        setEditingName(true)
                      }}
                    >
                      Đổi tên hiển thị
                    </button>
                  )}
                  {['STAFF', 'MANAGER'].includes(user.role) && (
                    <Link className="btn btn-warning btn-sm mt-2" to="/staff/orders">
                      Tra cứu đơn hàng
                    </Link>
                  )}
                  {user.role === 'MANAGER' && (
                    <>
                      <Link className="btn btn-primary btn-sm mt-2 ms-2" to="/manager/suppliers">
                        Quản lý nhà cung cấp
                      </Link>
                      <Link className="btn btn-success btn-sm mt-2 ms-2" to="/manager/purchase-orders/create">
                        Lập đơn PO
                      </Link>
                    </>
                  )}
                </div>
                {user.role === 'MANAGER' && <Link className="btn btn-outline-light" to="/manager/products">Quản lý sản phẩm</Link>}
                <Link className="btn btn-outline-light" to="/account/password">Đổi mật khẩu</Link>
                <button className="btn btn-outline-light" type="button" onClick={logout}>
                  Đăng xuất
                </button>
              </div>
            ) : (
              <div className="hero-actions">
                <Link className="btn btn-warning btn-lg" to="/products">Khám phá sản phẩm</Link>
                <Link className="btn btn-outline-light btn-lg" to="/login">Đăng nhập hệ thống</Link>
              </div>
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
