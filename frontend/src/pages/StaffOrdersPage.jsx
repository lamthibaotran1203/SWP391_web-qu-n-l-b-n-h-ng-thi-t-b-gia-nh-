import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { getRoleLabel } from '../auth/role-labels'
import { getSalesOrders } from '../api/http'
import { OrderDetailsDialog } from './orders/OrderDetailsDialog'
import { SalesOrdersTable } from './orders/SalesOrdersTable'
import { orderStatusLabels, paymentStatusLabels } from './orders/order-formatters'
import '../styles/orders.css'

const pageSize = 20

function StaffOrdersContent({ user }) {
  const [draft, setDraft] = useState({
    q: '',
    channel: '',
    status: '',
    paymentStatus: '',
    from: '',
    to: '',
  })
  const [filters, setFilters] = useState(draft)
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedOrderId, setSelectedOrderId] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true

    getSalesOrders({ ...filters, page, size: pageSize })
      .then((response) => {
        if (active) setResult(response)
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || 'Không tải được danh sách đơn hàng.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [filters, page, reloadKey])

  function updateDraft(event) {
    setDraft((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  function applyFilters(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    setResult(null)
    setPage(0)
    setFilters({ ...draft, q: draft.q.trim() })
  }

  function clearFilters() {
    const emptyFilters = { q: '', channel: '', status: '', paymentStatus: '', from: '', to: '' }
    setLoading(true)
    setError('')
    setResult(null)
    setDraft(emptyFilters)
    setFilters(emptyFilters)
    setPage(0)
  }

  function changePage(nextPage) {
    setLoading(true)
    setError('')
    setResult(null)
    setPage(nextPage)
  }

  function retrySearch() {
    setLoading(true)
    setError('')
    setResult(null)
    setReloadKey((key) => key + 1)
  }

  const totalPages = result?.totalPages || 0
  const orders = result?.content || []

  return (
    <main className="orders-page">
      <header className="orders-header">
        <div className="container orders-header-inner">
          <Link className="orders-brand" to="/">Gia Dụng Việt</Link>
          <nav className="orders-nav" aria-label="Điều hướng nhân viên">
            <span>{user.fullName}</span>
            <span className="orders-role">{getRoleLabel(user.role)}</span>
            <Link className="btn btn-outline-light btn-sm" to="/">Trang chủ</Link>
          </nav>
        </div>
      </header>

      <div className="container orders-content">
        <div className="orders-title-row">
          <div>
            <h1>Tra cứu đơn hàng</h1>
            <p className="orders-subtitle">
              Tìm và xem thông tin đơn hàng.
            </p>
          </div>
          <div className="orders-total" aria-live="polite">
            <strong>{result?.totalElements?.toLocaleString('vi-VN') ?? '—'}</strong>
            <span>đơn hàng</span>
          </div>
        </div>

        <form className="orders-filter-card" onSubmit={applyFilters}>
          <div className="orders-search-field">
            <label htmlFor="order-query">Tìm kiếm</label>
            <input
              className="form-control"
              id="order-query"
              name="q"
              maxLength={120}
              placeholder="Mã đơn, tên, email hoặc số điện thoại"
              value={draft.q}
              onChange={updateDraft}
            />
          </div>
          <div>
            <label className="form-label" htmlFor="order-channel">Kênh bán</label>
            <select className="form-select" id="order-channel" name="channel" value={draft.channel} onChange={updateDraft}>
              <option value="">Tất cả kênh</option>
              <option value="ONLINE">Trực tuyến</option>
              <option value="COUNTER">Tại quầy</option>
            </select>
          </div>
          <div>
            <label className="form-label" htmlFor="order-status">Trạng thái đơn</label>
            <select className="form-select" id="order-status" name="status" value={draft.status} onChange={updateDraft}>
              <option value="">Tất cả trạng thái</option>
              {Object.entries(orderStatusLabels).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="form-label" htmlFor="payment-status">Thanh toán</label>
            <select className="form-select" id="payment-status" name="paymentStatus" value={draft.paymentStatus} onChange={updateDraft}>
              <option value="">Tất cả thanh toán</option>
              {Object.entries(paymentStatusLabels).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="form-label" htmlFor="order-from">Từ ngày</label>
            <input className="form-control" id="order-from" name="from" type="date" value={draft.from} onChange={updateDraft} />
          </div>
          <div>
            <label className="form-label" htmlFor="order-to">Đến ngày</label>
            <input className="form-control" id="order-to" name="to" type="date" value={draft.to} onChange={updateDraft} />
          </div>
          <div className="orders-filter-actions">
            <button className="btn btn-success" type="submit" disabled={loading}>
              Tìm đơn hàng
            </button>
            <button className="btn btn-outline-secondary" type="button" onClick={clearFilters}>
              Xóa bộ lọc
            </button>
          </div>
        </form>

        {error && (
          <div className="alert alert-danger d-flex align-items-center justify-content-between" role="alert">
            <span>{error}</span>
            <button className="btn btn-sm btn-outline-danger" type="button" onClick={retrySearch}>
              Thử lại
            </button>
          </div>
        )}

        <SalesOrdersTable
          orders={orders}
          loading={loading}
          page={page}
          totalPages={totalPages}
          totalElements={result?.totalElements ?? 0}
          showResults={!error}
          onSelectOrder={setSelectedOrderId}
          onPageChange={changePage}
        />
      </div>

      {selectedOrderId !== null && (
        <OrderDetailsDialog
          orderId={selectedOrderId}
          onClose={() => setSelectedOrderId(null)}
        />
      )}
    </main>
  )
}

export function StaffOrdersPage() {
  const { user, connection, reload } = useAuth()
  const location = useLocation()

  if (connection.status === 'loading') {
    return <main className="orders-gate" role="status">Đang kiểm tra phiên đăng nhập…</main>
  }

  if (connection.status === 'error') {
    return (
      <main className="orders-gate">
        <div className="alert alert-danger" role="alert">
          <p>{connection.message}</p>
          <button className="btn btn-outline-danger" type="button" onClick={reload}>Thử kết nối lại</button>
        </div>
      </main>
    )
  }

  if (!user.authenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (!['STAFF', 'MANAGER'].includes(user.role)) {
    return (
      <main className="orders-gate">
        <div className="alert alert-warning">
          Tài khoản của bạn không có quyền tra cứu đơn hàng.
          <p className="mt-3 mb-0"><Link to="/">Quay về trang chủ</Link></p>
        </div>
      </main>
    )
  }

  return <StaffOrdersContent user={user} />
}
