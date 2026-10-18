import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { AppHeader, AppFooter } from '../../components/AppShell'
import { formatVnd } from '../catalog/api'
import { getCart } from './api'
import './cart.css'

export function CheckoutHandoffPage() {
  const { user, connection } = useAuth()
  if (connection?.status === 'loading') return <p role="status">Đang kiểm tra phiên đăng nhập...</p>
  if (!user.authenticated) return <Navigate to="/login" state={{ from: '/checkout' }} replace />
  if (user.role !== 'CUSTOMER') return <Navigate to="/products" replace />
  return <CheckoutReview />
}

function CheckoutReview() {
  const [cart, setCart] = useState(null)
  const [error, setError] = useState(null)
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    getCart({ signal: controller.signal }).then((data) => {
      if (!controller.signal.aborted) { setCart(data); setError(null) }
    }).catch((failure) => {
      if (!controller.signal.aborted) setError(failure)
    })
    return () => controller.abort()
  }, [retry])

  if (error?.status === 401) return <Navigate to="/login" state={{ from: '/checkout' }} replace />
  return <main className="catalog-page min-vh-100"><AppHeader />
    <section className="container cart-content">
      <Link to="/cart" className="back-link">← Quay lại giỏ hàng</Link>
      <h1>Chuẩn bị thanh toán</h1>
      <p>Đây là bước bàn giao từ UC10 sang UC14. Chưa tạo đơn hàng, giữ hàng hay trừ tồn kho.</p>
      {error ? <div role="alert">{error.message} <button onClick={() => setRetry((value) => value + 1)}>Thử lại</button></div>
        : !cart ? <p role="status">Đang kiểm tra lại giỏ hàng...</p>
          : !cart.checkoutEligible ? <div role="alert">Giỏ hàng trống hoặc có sản phẩm cần cập nhật. Vui lòng quay lại giỏ hàng.</div>
            : <div className="cart-summary">
              <h2>Thông tin giỏ hàng hiện tại</h2>
              {cart.items.map((item) => <p key={item.skuId}>{item.name} × {item.quantity} — {formatVnd(item.lineTotal)}</p>)}
              <p>Tạm tính: <strong>{formatVnd(cart.subtotal)}</strong></p>
              <p>Giá đã gồm VAT. Phí giao hàng, ưu đãi và xác nhận đặt hàng sẽ do UC14 xử lý.</p>
              <p role="status">Bước đặt hàng UC14 đang chờ tích hợp.</p>
            </div>}
    </section><AppFooter /></main>
}
