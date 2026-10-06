import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { getRoleLabel } from '../auth/role-labels'
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
    <main>
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
