import { useEffect, useRef, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { AppHeader, AppFooter } from '../../components/AppShell'
import { formatVnd } from '../catalog/api'
import { getCart, updateCartItem, removeCartItems } from './api'
import './cart.css'

export function CartPage() {
  const { user, connection } = useAuth()
  if (connection?.status === 'loading') return <main className="catalog-page"><AppHeader /><div className="container py-5" role="status">Đang kiểm tra phiên đăng nhập...</div><AppFooter /></main>
  if (!user.authenticated) return <Navigate to="/login" state={{ from: '/cart' }} replace />
  if (user.role !== 'CUSTOMER') return <main className="catalog-page"><AppHeader /><div className="container py-5">Giỏ hàng dành cho tài khoản khách hàng.</div><AppFooter /></main>
  return <CartContent />
}

function CartContent() {
  const navigate = useNavigate()
  const [cart, setCart] = useState(null)
  const [retry, setRetry] = useState(0)
  const [error, setError] = useState('')
  const [expired, setExpired] = useState(false)
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')
  const [refreshRequired, setRefreshRequired] = useState(false)
  const [pendingSkuIds, setPendingSkuIds] = useState([])
  const saving = useRef(false)
  const [selection, setSelection] = useState([])
  useEffect(() => {
    const controller = new AbortController()
    getCart({ signal: controller.signal }).then((data) => {
      if (!controller.signal.aborted) { setCart(data); setError(''); setRefreshRequired(false); setPendingSkuIds([]) }
    }).catch((failure) => {
      if (!controller.signal.aborted) { setError(failure.message); setExpired(failure.status === 401) }
    })
    return () => controller.abort()
  }, [retry])

  async function change(work) {
    if (saving.current) return false
    saving.current = true
    setBusy(true)
    setError('')
    try {
      const result = await work()
      if (result.removed === true) {
        setCart(result.cart)
        setNotice(result.message)
        setRefreshRequired(result.refreshRequired)
      } else {
        setCart(result)
        setNotice('')
      }
      setSelection([])
      return true
    }
    catch (failure) { setError(failure.message); setExpired(failure.status === 401); return false }
    finally { saving.current = false; setBusy(false) }
  }
  function markPending(skuId, pending) {
    setPendingSkuIds((ids) => pending
      ? ids.includes(skuId) ? ids : [...ids, skuId]
      : ids.filter((id) => id !== skuId))
  }
  if (expired) return <Navigate to="/login" state={{ from: '/cart' }} replace />
  return <main className="catalog-page min-vh-100"><AppHeader />
    <section className="container cart-content">
      <Link to="/products" className="back-link">← Tiếp tục mua sắm</Link>
      <h1>Giỏ hàng của bạn</h1>
      <p className="text-secondary">Giá và số lượng còn hàng được kiểm tra theo thông tin hiện tại.</p>
      <p className="text-secondary small">Giá đã bao gồm VAT, hiển thị bằng đồng Việt Nam. Tạm tính chưa gồm phí giao hàng và ưu đãi tại bước thanh toán.</p>
      {notice && <div className="alert alert-success" role="status">{notice}</div>}
      {error && <div className="alert alert-danger" role="alert">{error} <button className="btn btn-sm btn-outline-danger" disabled={busy} onClick={() => setRetry((value) => value + 1)}>Thử lại</button></div>}
      {!cart ? refreshRequired
        ? <button className="btn btn-dark" disabled={busy} onClick={() => setRetry((value) => value + 1)}>Tải lại giỏ hàng</button>
        : !error && <p role="status">Đang tải giỏ hàng...</p>
        : cart.items.length === 0 ? <div className="empty-results"><h2>Giỏ hàng đang trống</h2><p>Khám phá sản phẩm phù hợp với ngôi nhà của bạn.</p><Link className="btn btn-dark" to="/products">Xem sản phẩm</Link></div>
          : <div className="cart-layout"><div>
            <div className="cart-toolbar"><label><input type="checkbox" disabled={busy} checked={selection.length === cart.items.length} onChange={(event) => setSelection(event.target.checked ? cart.items.map((item) => item.skuId) : [])} /> Chọn tất cả</label>
              <button className="btn btn-outline-danger btn-sm" disabled={busy || selection.length === 0} onClick={() => change(() => removeCartItems(selection))}>Xóa đã chọn ({selection.length})</button></div>
            {cart.items.map((item) => <CartItem key={item.skuId + ':' + item.quantity} item={item} busy={busy} selected={selection.includes(item.skuId)}
              toggle={() => setSelection((ids) => ids.includes(item.skuId) ? ids.filter((id) => id !== item.skuId) : [...ids, item.skuId])}
              update={(quantity) => change(() => updateCartItem(item.skuId, quantity, item.version))}
              markPending={(pending) => markPending(item.skuId, pending)}
              remove={() => change(() => removeCartItems([item.skuId]))} />)}
          </div><aside className="cart-summary"><h2>Tóm tắt giỏ hàng</h2><p>{cart.items.length} phiên bản sản phẩm</p><div className="cart-subtotal"><span>Tạm tính</span><strong>{formatVnd(cart.subtotal)}</strong></div>
            {!cart.checkoutEligible && <p className="text-danger">Vui lòng cập nhật hoặc xóa sản phẩm không còn đủ điều kiện mua.</p>}
            <button className="btn btn-dark w-100" disabled={busy || pendingSkuIds.length > 0 || !cart.checkoutEligible} onClick={() => navigate('/checkout')}>Thanh toán</button><p className="text-secondary small mt-2">Tồn kho và giá sẽ được kiểm tra lại khi đặt hàng.</p></aside></div>}
    </section><AppFooter /></main>
}

function CartItem({ item, busy, selected, toggle, update, remove, markPending }) {
  const [quantity, setQuantity] = useState(String(item.quantity))
  const timer = useRef(null)
  useEffect(() => () => clearTimeout(timer.current), [])
  const number = Number(quantity)
  const valid = /^\d+$/.test(quantity) && Number.isSafeInteger(number) && number > 0 && number <= item.availableQuantity
  function editQuantity(value) {
    setQuantity(value)
    markPending(Number(value) !== item.quantity)
    clearTimeout(timer.current)
    const next = Number(value)
    if (!/^\d+$/.test(value) || !Number.isSafeInteger(next) || next < 1 || next > item.availableQuantity || next === item.quantity) return
    timer.current = setTimeout(async () => {
      const saved = await update(next)
      if (!saved) setQuantity(String(item.quantity))
      markPending(false)
    }, 400)
  }
  return <article className="cart-item">
    <input type="checkbox" aria-label={`Chọn ${item.name}`} checked={selected} onChange={toggle} disabled={busy} />
    <div className="cart-item-image">{item.imageUrl ? <img src={item.imageUrl} alt="" /> : <span>Gia dụng</span>}</div>
    <div className="cart-item-copy"><Link to={`/products/${item.productId}?sku=${item.skuId}`}><h2>{item.name}</h2></Link><p>{item.skuName} · {item.skuCode}</p>
      <strong>{formatVnd(item.unitPrice)}</strong><p className={item.available ? 'text-secondary' : 'text-danger'}>{!item.eligible ? 'Sản phẩm không còn được bán' : `Còn ${item.availableQuantity} sản phẩm`}{item.eligible && !item.available && ' — số lượng trong giỏ vượt tồn kho'}</p>
      <div className="cart-item-actions"><label htmlFor={`cart-quantity-${item.skuId}`}>Số lượng</label><input id={`cart-quantity-${item.skuId}`} type="number" min="1" step="1" max={item.availableQuantity || undefined} value={quantity} disabled={busy || !item.eligible} onChange={(event) => editQuantity(event.target.value)} />
      </div>
      {busy && <p className="text-secondary small" role="status">Đang lưu giỏ hàng...</p>}
      {item.eligible && !valid && <p className="text-danger small">Số lượng phải là số nguyên từ 1 đến {item.availableQuantity}.</p>}
    </div><div className="cart-item-end"><strong className="cart-line-total">{formatVnd(item.lineTotal)}</strong>
      <button className="btn btn-sm btn-outline-danger" disabled={busy} onClick={remove}>Xóa</button></div>
  </article>
}
