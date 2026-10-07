import { useEffect, useState } from 'react'
import { getSalesOrderDetails } from '../../api/http'
import {
  channelLabels,
  formatCurrency,
  formatDate,
  formatLabel,
  orderStatusLabels,
  paymentMethodLabels,
  paymentStatusLabels,
} from './order-formatters'

export function OrderDetailsDialog({ orderId, onClose }) {
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
            {order && (
              <div className="order-dialog-badges">
                <span className={`order-badge order-badge--${order.status.toLowerCase()}`}>
                  {formatLabel(orderStatusLabels, order.status)}
                </span>
                <span className={`order-payment-badge order-payment-badge--${order.paymentStatus.toLowerCase()}`}>
                  {formatLabel(paymentStatusLabels, order.paymentStatus)}
                </span>
              </div>
            )}
          </div>
          <button className="order-dialog-close" type="button" onClick={onClose} aria-label="Đóng chi tiết" autoFocus>
            ×
          </button>
        </header>
        {loading && <p className="order-dialog-loading" role="status"><span className="loading-spinner" />Đang tải chi tiết đơn hàng…</p>}
        {error && <div className="alert alert-danger order-dialog-error" role="alert">{error}</div>}
        {order && (
          <div className="order-details-body">
            <div className="order-details-summary">
              <div className="order-summary-item">
                <span>Ngày đặt</span>
                <strong>{formatDate(order.placedAt)}</strong>
              </div>
              <div className="order-summary-item">
                <span>Kênh</span>
                <strong>{formatLabel(channelLabels, order.channel)}</strong>
              </div>
              <div className="order-summary-item">
                <span>Phương thức</span>
                <strong>{formatLabel(paymentMethodLabels, order.paymentMethod)}</strong>
              </div>
            </div>
            <section className="order-detail-section order-customer-section">
              <div className="order-detail-section-heading">
                <span className="order-section-icon" aria-hidden="true">KH</span>
                <div>
                  <h3>Thông tin khách hàng</h3>
                  <p>Thông tin liên hệ và nhận hàng</p>
                </div>
              </div>
              <div className="order-customer-card">
                <div className="order-customer-name">
                  <span className="order-customer-avatar" aria-hidden="true">
                    {order.customerName?.charAt(0)?.toUpperCase() || '?'}
                  </span>
                  <strong>{order.customerName || 'Khách hàng'}</strong>
                </div>
                <div className="order-customer-details">
                  {order.customerPhone && (
                    <p><span>Số điện thoại</span><strong>{order.customerPhone}</strong></p>
                  )}
                  {order.customerEmail && (
                    <p><span>Email</span><strong>{order.customerEmail}</strong></p>
                  )}
                  {order.shippingAddress && (
                    <p className="order-customer-address"><span>Địa chỉ giao hàng</span><strong>{order.shippingAddress}</strong></p>
                  )}
                </div>
              </div>
            </section>
            <section className="order-detail-section">
              <div className="order-detail-section-heading">
                <span className="order-section-icon" aria-hidden="true">SP</span>
                <div>
                  <h3>Sản phẩm</h3>
                  <p>{order.items.length} mặt hàng trong đơn</p>
                </div>
              </div>
              {order.items.length === 0 ? (
                <p className="order-empty-items">Đơn hàng chưa có sản phẩm.</p>
              ) : (
                <div className="order-line-list">
                  {order.items.map((item) => (
                    <div className="order-line" key={item.id}>
                      <span className="order-product-mark" aria-hidden="true">G</span>
                      <div className="order-line-info">
                        <strong>{item.productName}</strong>
                        <span>Mã SKU: {item.productSku}</span>
                        <span>{formatCurrency(item.unitPrice)} / sản phẩm</span>
                      </div>
                      <div className="order-line-quantity">x {item.quantity}</div>
                      <strong className="order-line-total">{formatCurrency(item.lineTotal)}</strong>
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
