import { Link, Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../../auth/useAuth'
import { WorkspaceHeader } from '../../../components/AppShell'
import './management.css'

export function CatalogWorkspace() {
  const { user, connection, reload } = useAuth()
  const location = useLocation()

  if (connection.status === 'loading') return <p className="container py-4" role="status">Đang kiểm tra phiên đăng nhập…</p>
  if (connection.status === 'error') return <div className="container py-4" role="alert">
    <p>{connection.message || 'Chưa kết nối được máy chủ.'}</p>
    <button className="btn btn-outline-dark" onClick={reload}>Kết nối lại</button>
  </div>
  if (!user.authenticated) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  if (user.role !== 'ADMIN') return <div className="container py-4">
    <h1>Không có quyền quản lý sản phẩm</h1><Link to="/">Về trang chủ</Link>
  </div>

  return <div className="catalog-workspace">
    <main className="container catalog-workspace-content"><Outlet /></main>
  </div>
}

export function CatalogPageHeading({ title, children }) {
  return <>
    <WorkspaceHeader title={title}>{children}</WorkspaceHeader>
  </>
}

export function CatalogLoadError({ error, onRetry }) {
  return <div className="alert alert-danger management-load-error" role="alert">
    <span>{error.message || 'Chưa tải được dữ liệu sản phẩm.'}</span>
    <button className="btn btn-outline-dark btn-sm" onClick={onRetry}>Thử tải lại</button>
  </div>
}
