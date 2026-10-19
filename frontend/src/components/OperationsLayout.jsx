import { useState } from 'react'
import { Link, Navigate, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { getRoleLabel as roleLabel } from '../auth/role-labels'
import './operations-layout.css'

const adminItems = [
  { to: '/admin/products', icon: 'products', label: 'Sản phẩm & SKU' },
  { to: '/admin/categories', icon: 'categories', label: 'Danh mục sản phẩm' },
  { to: '/admin/brands', icon: 'brands', label: 'Thương hiệu' },
  { to: '/staff/orders', icon: 'orders', label: 'Đơn hàng bán' },
  { to: '/staff/pos', icon: 'pos', label: 'Bán hàng tại quầy' },
  { to: '/admin/suppliers', icon: 'suppliers', label: 'Nhà cung cấp' },
  { to: '/admin/purchase-orders/create', icon: 'purchase', label: 'Lập đơn mua hàng' },
]

const staffItems = [
  { to: '/staff/orders', icon: 'orders', label: 'Đơn hàng bán' },
  { to: '/staff/pos', icon: 'pos', label: 'Bán hàng tại quầy' },
]

function MenuIcon({ name }) {
  const paths = {
    orders: <><path d="M4 5h16v14H4z" /><path d="M8 9h8M8 13h5" /></>,
    pos: <><path d="M4 6h16l-1 14H5L4 6z" /><path d="M8 9v2a4 4 0 0 0 8 0V9" /></>,
    products: <><path d="m4 8 8-4 8 4-8 4z" /><path d="m4 8 8 4 8-4v8l-8 4-8-4z" /></>,
    categories: <><path d="M4 5h6v6H4zM14 5h6v6h-6zM4 15h6v4H4zM14 15h6v4h-6z" /></>,
    brands: <><path d="M4 5h10l6 6-9 9-7-7z" /><circle cx="9" cy="10" r="1" /></>,
    suppliers: <><path d="M3 20h18M5 20V9h6v11M13 20V4h6v16" /><path d="M7 12h2M15 8h2M15 12h2M15 16h2" /></>,
    purchase: <><path d="M6 3h12v18H6z" /><path d="M9 7h6M9 11h6M9 15h3" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1A1.7 1.7 0 0 0 9 4.6 1.7 1.7 0 0 0 10 3V2.8h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1z" /></>,
    logout: <><path d="M10 5H5v14h5M14 8l4 4-4 4M18 12H9" /></>,
    menu: <><path d="M4 7h16M4 12h16M4 17h16" /></>,
    close: <><path d="m6 6 12 12M18 6 6 18" /></>,
  }

  return (
    <svg className="operations-icon" viewBox="0 0 24 24" aria-hidden="true">
      {paths[name]}
    </svg>
  )
}

export function RequireRole({ roles }) {
  const { user } = useAuth()
  if (!roles.includes(user.role)) {
    return (
      <section className="operations-forbidden" role="alert">
        <span className="operations-forbidden-code">403</span>
        <h1>Bạn không có quyền truy cập</h1>
        <p>Chức năng này chỉ dành cho {roles.map(roleLabel).join(', ')}.</p>
        <Link className="btn btn-primary" to={user.role === 'STAFF' ? '/staff/orders' : '/'}>
          Quay lại khu vực của bạn
        </Link>
      </section>
    )
  }
  return <Outlet />
}

export function OperationsLayout() {
  const { user, connection, logout, reload } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)

  if (connection.status === 'loading') {
    return <main className="operations-gate" role="status">Đang kiểm tra phiên đăng nhập…</main>
  }

  if (connection.status === 'error') {
    return (
      <main className="operations-gate">
        <div className="alert alert-danger" role="alert">
          <p>{connection.message || 'Chưa kết nối được máy chủ.'}</p>
          <button className="btn btn-outline-danger" type="button" onClick={reload}>Thử kết nối lại</button>
        </div>
      </main>
    )
  }

  if (!user.authenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }

  if (!['STAFF', 'ADMIN'].includes(user.role)) {
    return (
      <main className="operations-gate">
        <div className="alert alert-warning" role="alert">
          <h1>Khu vực dành cho nhân viên</h1>
          <p>Tài khoản của bạn không có quyền sử dụng khu vực vận hành.</p>
          <Link to="/">Quay về trang chủ</Link>
        </div>
      </main>
    )
  }

  const items = user.role === 'ADMIN' ? adminItems : staffItems

  async function handleLogout() {
    if (loggingOut) return
    setLoggingOut(true)
    try {
      await logout()
      navigate('/login', { replace: true })
    } finally {
      setLoggingOut(false)
    }
  }

  return (
    <div className={`operations-layout${menuOpen ? ' operations-layout--open' : ''}`}>
      <button
        className="operations-backdrop"
        type="button"
        aria-label="Đóng menu"
        onClick={() => setMenuOpen(false)}
      />

      <aside className="operations-sidebar" id="operations-navigation">
        <div className="operations-brand-row">
          <Link className="operations-brand" to="/">
            <span className="operations-brand-mark" aria-hidden="true">G</span>
            <span><strong>HomeMart</strong><small>{roleLabel(user.role)}</small></span>
          </Link>
          <button className="operations-close" type="button" aria-label="Đóng menu" onClick={() => setMenuOpen(false)}>
            <MenuIcon name="close" />
          </button>
        </div>

        <nav className="operations-navigation" aria-label="Điều hướng khu vực làm việc">
          {items.map((item) => (
            <NavLink className="operations-nav-link" to={item.to} key={item.to} onClick={() => setMenuOpen(false)}>
              <MenuIcon name={item.icon} /><span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="operations-sidebar-footer">
          <Link className="operations-secondary-link" to="/account/password" onClick={() => setMenuOpen(false)}><MenuIcon name="settings" /><span>Cài đặt tài khoản</span></Link>
          <button className="operations-logout" type="button" onClick={handleLogout} disabled={loggingOut}>
            <MenuIcon name="logout" /><span>{loggingOut ? 'Đang đăng xuất…' : 'Đăng xuất'}</span>
          </button>
        </div>
      </aside>

      <div className="operations-main">
        <header className="operations-mobile-bar">
          <button
            className="operations-menu-toggle"
            type="button"
            aria-label="Mở menu"
            aria-controls="operations-navigation"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen(true)}
          >
            <MenuIcon name="menu" />
          </button>
          <strong>HomeMart</strong>
        </header>
        <div className="operations-content"><Outlet /></div>
      </div>
    </div>
  )
}
