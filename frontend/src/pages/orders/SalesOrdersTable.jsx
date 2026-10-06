import {
  channelLabels,
  formatCurrency,
  formatDate,
  formatLabel,
  orderStatusLabels,
  paymentStatusLabels,
} from './order-formatters'

export function SalesOrdersTable({
  orders,
  loading,
  page,
  totalPages,
  totalElements,
  showResults,
  onSelectOrder,
  onPageChange,
}) {
  return (
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
            {!loading && showResults && orders.map((order) => (
              <tr key={order.id}>
                <td>
                  <button
                    className="order-number-button"
                    type="button"
                    onClick={() => onSelectOrder(order.id)}
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
            {!loading && showResults && orders.length === 0 && (
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
          {totalElements
            ? `Trang ${page + 1} / ${totalPages}`
            : 'Không có kết quả'}
        </span>
        <div>
          <button
            className="btn btn-sm btn-outline-secondary"
            type="button"
            onClick={() => onPageChange(Math.max(0, page - 1))}
            disabled={loading || page === 0 || totalPages === 0}
          >
            Trước
          </button>
          <button
            className="btn btn-sm btn-outline-secondary"
            type="button"
            onClick={() => onPageChange(page + 1)}
            disabled={loading || page + 1 >= totalPages}
          >
            Sau
          </button>
        </div>
      </footer>
    </section>
  )
}
