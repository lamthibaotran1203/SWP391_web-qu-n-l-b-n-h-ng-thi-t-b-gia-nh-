import { useContext } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { AuthContext } from '../auth/auth-context'
import { AccountMenu } from './AccountMenu'

function Brand() {
  return (
    <Link className="app-brand" to="/" aria-label="Gia Dụng Việt - Trang chủ">
      <span className="app-brand-mark" aria-hidden="true">G</span>
      <span>
        <strong>Gia Dụng Việt</strong>
        <small>HomeMart</small>
      </span>
    </Link>
  )
}

export function AppHeader({ variant = 'light' }) {
  const auth = useContext(AuthContext)
  const user = auth?.user || { authenticated: false, fullName: '', role: '' }
  const logout = auth?.logout
  const isEmployee = ['STAFF', 'MANAGER'].includes(user.role)

  return (
    <header className={`app-header app-header--${variant}`}>
      <div className="container app-header-inner">
        <Brand />
        <nav className="app-navigation" aria-label="Điều hướng chính">
          <NavLink to="/" end>Trang chủ</NavLink>
          <NavLink to="/products">Sản phẩm</NavLink>
          {user.role === 'MANAGER' && <NavLink to="/staff/products">Quản lý sản phẩm</NavLink>}
          {user.role === 'MANAGER' && <NavLink to="/manager/brands">Thương hiệu</NavLink>}
          {isEmployee && <NavLink to="/staff/orders">Đơn hàng</NavLink>}
          {user.role === 'MANAGER' && <NavLink to="/staff/suppliers">Nhà cung cấp</NavLink>}
        </nav>
        <div className="app-header-actions">
          {user.authenticated ? (
            <AccountMenu user={user} logout={logout} />
          ) : (
            <Link className="btn btn-primary btn-sm" to="/login">Đăng nhập</Link>
          )}
        </div>
      </div>
    </header>
  )
}

export function AppFooter() {
  return (
    <footer className="app-footer">
      <div className="container app-footer-grid">
        <div>
          <Brand />
          <p>Đồ gia dụng đáng tin cậy, vận hành cửa hàng rõ ràng và hiệu quả.</p>
        </div>
        <div>
          <strong>Khám phá</strong>
          <Link to="/products">Danh mục sản phẩm</Link>
          <Link to="/login">Tài khoản của bạn</Link>
        </div>
        <div>
          <strong>Hỗ trợ</strong>
          <span>support@homemart.vn</span>
          <span>1900 6868</span>
        </div>
      </div>
      <div className="container app-footer-bottom">© {new Date().getFullYear()} HomeMart · Gia Dụng Việt</div>
    </footer>
  )
}

export function WorkspaceHeader({ eyebrow, title, description, children }) {
  return (
    <div className="workspace-heading">
      <div>
        {eyebrow && <p className="page-eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {description && <p>{description}</p>}
      </div>
      {children && <div className="workspace-heading-actions">{children}</div>}
    </div>
  )
}
