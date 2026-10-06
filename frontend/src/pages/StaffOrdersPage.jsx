import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { getRoleLabel } from '../auth/role-labels'
import { getSalesOrderDetails, getSalesOrders } from '../api/http'

const pageSize = 20

const orderStatusLabels = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang xử lý',
  SHIPPED: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
}

const paymentStatusLabels = {
  PENDING: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  FAILED: 'Thanh toán lỗi',
  REFUNDED: 'Đã hoàn tiền',
  COD: 'Thanh toán khi nhận hàng',
}

const channelLabels = {
  ONLINE: 'Trực tuyến',
  IN_STORE: 'Tại cửa hàng',
}

const paymentMethodLabels = {
  COD: 'COD',
  CASH: 'Tiền mặt',
  CARD: 'Thẻ',
  BANK_TRANSFER: 'Chuyển khoản',
  ONLINE_GATEWAY: 'Cổng thanh toán',
}

function formatLabel(labels, value) {
  return labels[value] || value?.replaceAll('_', ' ') || '—'
}

function formatCurrency(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount)
}

function formatDate(value) {
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'Asia/Ho_Chi_Minh',
  }).format(new Date(value))
}

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
              <option value="IN_STORE">Tại cửa hàng</option>
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

        <section className="orders-table-card" aria-label="Danh sách đơn hàng">
          <div className="orders-table-heading">
            <div>
              <h2>Danh sách đơn</h2>
              <p>Đơn mới nhất được hiển thị trước.</p>
            </div>
            {loading && <span className="orders-loading" role="status">Đang tải…</span>}
          </div>
          <div className="table-responsive">
            <table className="table orders-table align-middle mb-0">
              <thead>
                <tr>
                  <th scope="col">Mã đơn / Thời gian</th>
                  <th scope="col">Khách hàng</th>
                  <th scope="col">Kênh</th>
                  <th scope="col">Trạng thái</th>
                  <th scope="col">Thanh toán</th>
                  <th scope="col" className="text-end">Tổng tiền</th>
                </tr>
              </thead>
              <tbody>
                {!loading && !error && orders.map((order) => (
                  <tr key={order.id}>
                    <td>
                      <button
                        className="order-number-button"
                        type="button"
                        onClick={() => setSelectedOrderId(order.id)}
                        aria-label={`Xem chi tiết đơn ${order.orderNumber}`}
                      >
                        {order.orderNumber}
                      </button>
                      <span className="orders-cell-note">{formatDate(order.placedAt)}</span>
                    </td>
                    <td>
                      <strong>{order.customerName}</strong>
                      <span className="orders-cell-note">{order.customerPhone || order.customerEmail || '—'}</span>
                    </td>
                    <td>{formatLabel(channelLabels, order.channel)}</td>
                    <td><span className={`order-badge order-badge--${order.status.toLowerCase()}`}>{formatLabel(orderStatusLabels, order.status)}</span></td>
                    <td>{formatLabel(paymentStatusLabels, order.paymentStatus)}</td>
                    <td className="text-end fw-semibold">{formatCurrency(order.totalAmount)}</td>
                  </tr>
                ))}
                {!loading && !error && orders.length === 0 && (
                  <tr>
                    <td className="orders-empty-cell" colSpan="6">
                      <span className="orders-empty-icon" aria-hidden="true">⌕</span>
                      <strong>Chưa tìm thấy đơn hàng</strong>
                      <span>Thử thay đổi từ khóa hoặc bộ lọc để tìm kết quả khác.</span>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <footer className="orders-pagination">
            <span>
              {result?.totalElements
                ? `Trang ${page + 1} / ${totalPages}`
                : 'Không có kết quả'}
            </span>
            <div>
              <button
                className="btn btn-sm btn-outline-secondary"
                type="button"
                onClick={() => changePage(Math.max(0, page - 1))}
                disabled={loading || page === 0 || totalPages === 0}
              >
                Trước
              </button>
              <button
                className="btn btn-sm btn-outline-secondary"
                type="button"
                onClick={() => changePage(page + 1)}
                disabled={loading || page + 1 >= totalPages}
              >
                Sau
              </button>
            </div>
          </footer>
        </section>
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

function OrderDetailsDialog({ orderId, onClose }) {
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    getSalesOrderDetails(orderId)
      .then((details) => {
        if (active) setOrder(details)
      })
      .catch((requestError) => {
        if (active) setError(requestError.message || 'Không tải được chi tiết đơn hàng.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [orderId])

  return (
    <div className="order-dialog-backdrop" onClick={onClose}>
      <section
        className="order-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="order-dialog-title"
        onClick={(event) => event.stopPropagation()}
        onKeyDown={(event) => {
          if (event.key === 'Escape') onClose()
        }}
      >
        <header className="order-dialog-header">
          <div>
            <p className="orders-eyebrow">CHI TIẾT ĐƠN HÀNG</p>
            <h2 id="order-dialog-title">{order?.orderNumber || 'Đơn hàng'}</h2>
          </div>
          <button className="order-dialog-close" type="button" onClick={onClose} aria-label="Đóng chi tiết" autoFocus>
            ×
          </button>
        </header>
        {loading && <p role="status">Đang tải chi tiết đơn hàng…</p>}
        {error && <div className="alert alert-danger" role="alert">{error}</div>}
        {order && (
          <div className="order-details-body">
            <div className="order-details-summary">
              <div>
                <span>Ngày đặt</span>
                <strong>{formatDate(order.placedAt)}</strong>
              </div>
              <div>
                <span>Kênh</span>
                <strong>{formatLabel(channelLabels, order.channel)}</strong>
              </div>
              <div>
                <span>Trạng thái</span>
                <strong>{formatLabel(orderStatusLabels, order.status)}</strong>
              </div>
              <div>
                <span>Thanh toán</span>
                <strong>{formatLabel(paymentStatusLabels, order.paymentStatus)}</strong>
              </div>
              <div>
                <span>Phương thức</span>
                <strong>{formatLabel(paymentMethodLabels, order.paymentMethod)}</strong>
              </div>
            </div>
            <section className="order-detail-section">
              <h3>Thông tin khách hàng</h3>
              <p><strong>{order.customerName}</strong></p>
              {order.customerPhone && <p>{order.customerPhone}</p>}
              {order.customerEmail && <p>{order.customerEmail}</p>}
              {order.shippingAddress && <p>{order.shippingAddress}</p>}
            </section>
            <section className="order-detail-section">
              <h3>Sản phẩm</h3>
              {order.items.length === 0 ? (
                <p>Đơn hàng chưa có sản phẩm.</p>
              ) : (
                <div className="order-line-list">
                  {order.items.map((item) => (
                    <div className="order-line" key={item.id}>
                      <div>
                        <strong>{item.productName}</strong>
                        <span>{item.productSku} · {item.quantity} × {formatCurrency(item.unitPrice)}</span>
                      </div>
                      <strong>{formatCurrency(item.lineTotal)}</strong>
                    </div>
                  ))}
                </div>
              )}
            </section>
            <section className="order-detail-section order-totals">
              <p><span>Tạm tính</span><strong>{formatCurrency(order.subtotal)}</strong></p>
              <p><span>Giảm giá</span><strong>−{formatCurrency(order.discountAmount)}</strong></p>
              <p><span>Phí giao hàng</span><strong>{formatCurrency(order.shippingFee)}</strong></p>
              <p className="order-grand-total"><span>Tổng thanh toán</span><strong>{formatCurrency(order.totalAmount)}</strong></p>
            </section>
          </div>
        )}
      </section>
    </div>
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
