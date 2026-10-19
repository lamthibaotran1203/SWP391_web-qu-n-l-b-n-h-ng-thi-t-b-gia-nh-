import { useEffect, useState } from 'react'
import { formatVnd } from '../catalog/api'
import { formatDate } from '../../pages/orders/order-formatters'
import { Link } from 'react-router-dom'
import { getPosCatalog, processPosTransaction } from './api'
import './pos.css'

const pageSize = 12

export function PosPage() {
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [catalog, setCatalog] = useState(null)
  const [loadingCatalog, setLoadingCatalog] = useState(true)
  const [catalogError, setCatalogError] = useState('')
  const [cart, setCart] = useState([])
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [processing, setProcessing] = useState(false)
  const [saleError, setSaleError] = useState('')
  const [submissionKey, setSubmissionKey] = useState(null)
  const [completedOrder, setCompletedOrder] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    getPosCatalog({ search, page, size: pageSize }, { signal: controller.signal })
      .then((result) => {
        if (!controller.signal.aborted) {
          setCatalog(result)
          setCatalogError('')
        }
      })
      .catch((error) => {
        if (!controller.signal.aborted) {
          setCatalogError(error.message || 'Không tải được danh sách sản phẩm.')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoadingCatalog(false)
      })
    return () => controller.abort()
  }, [search, page, reloadKey])

  function addItem(sku) {
    if (sku.availableQuantity < 1) return
    const existing = cart.find((item) => item.skuId === sku.skuId)
    if (existing && existing.quantity >= sku.availableQuantity) {
      setSaleError(`"${sku.productName} - ${sku.skuName}" không còn đủ tồn kho.`)
      return
    }
    setSaleError('')
    setCompletedOrder(null)
    setSubmissionKey(null)
    setCart((current) => {
      const currentItem = current.find((item) => item.skuId === sku.skuId)
      if (!currentItem) return [...current, { ...sku, quantity: 1 }]
      return current.map((item) => (
        item.skuId === sku.skuId ? { ...item, quantity: item.quantity + 1 } : item
      ))
    })
  }

  function changeQuantity(skuId, quantity) {
    const item = cart.find((line) => line.skuId === skuId)
    if (!item) return
    setSaleError('')
    setCompletedOrder(null)
    setSubmissionKey(null)
    if (quantity < 1) {
      setCart((current) => current.filter((line) => line.skuId !== skuId))
    } else if (quantity <= item.availableQuantity) {
      setCart((current) => current.map((line) => (
        line.skuId === skuId ? { ...line, quantity } : line
      )))
    } else {
      setSaleError(`"${item.productName} - ${item.skuName}" chỉ còn ${item.availableQuantity} sản phẩm.`)
    }
  }

  async function submitSale(event) {
    event.preventDefault()
    if (processing || cart.length === 0) return
    const requestKey = submissionKey || globalThis.crypto.randomUUID()
    setSubmissionKey(requestKey)
    setProcessing(true)
    setSaleError('')
    try {
      const order = await processPosTransaction(
        cart.map(({ skuId, quantity }) => ({ skuId, quantity })),
        paymentMethod,
        requestKey,
      )
      setCompletedOrder(order)
      setCart([])
      setSubmissionKey(null)
      setLoadingCatalog(true)
      setReloadKey((value) => value + 1)
    } catch (error) {
      setSaleError(error.message || 'Không thể hoàn tất giao dịch. Vui lòng thử lại.')
    } finally {
      setProcessing(false)
    }
  }

  function startNextSale() {
    setCompletedOrder(null)
    setSaleError('')
    setSubmissionKey(null)
  }

  const subtotal = cart.reduce(
    (sum, item) => sum + BigInt(item.sellingPrice) * BigInt(item.quantity),
    0n,
  ).toString()
  const totalPages = catalog ? Math.ceil(catalog.totalElements / pageSize) : 0

  return (
    <main className="pos-page">
      <header className="pos-heading">
        <div>
          <p className="pos-eyebrow">BÁN HÀNG TẠI QUẦY</p>
          <h1>Giao dịch POS</h1>
          <p>Tìm sản phẩm trong danh mục thật, kiểm tra tồn kho và ghi nhận giao dịch tại quầy.</p>
        </div>
        <Link className="btn btn-outline-secondary" to="/staff/orders">Tra cứu đơn hàng</Link>
      </header>

      {completedOrder ? (
        <section className="pos-receipt" aria-labelledby="pos-receipt-title" role="status">
          <p className="pos-eyebrow">GIAO DỊCH THÀNH CÔNG</p>
          <h2 id="pos-receipt-title">Đã tạo đơn {completedOrder.orderNumber}</h2>
          <p>Thanh toán: {formatVnd(completedOrder.totalAmount)} · Đã ghi nhận thanh toán thành công.</p>
          <p>Thời gian: {formatDate(completedOrder.placedAt)}</p>
          <div className="pos-receipt-actions">
            <Link className="btn btn-success" to="/staff/orders">Xem đơn hàng thật</Link>
            <button className="btn btn-outline-secondary" type="button" onClick={startNextSale}>Bắt đầu giao dịch mới</button>
          </div>
        </section>
      ) : (
        <div className="pos-workspace">
          <section className="pos-catalog-panel" aria-labelledby="pos-catalog-title">
            <div className="pos-panel-heading">
              <div>
                <h2 id="pos-catalog-title">Sản phẩm đang kinh doanh</h2>
                <p>Giá và tồn kho lấy trực tiếp từ hệ thống.</p>
              </div>
            </div>
            <form
              className="pos-search"
              onSubmit={(event) => {
                event.preventDefault()
                setLoadingCatalog(true)
                setCatalogError('')
                setSearch(searchDraft.trim())
                setPage(0)
              }}
            >
              <label className="visually-hidden" htmlFor="pos-search">Tìm sản phẩm hoặc mã SKU</label>
              <input
                className="form-control"
                id="pos-search"
                maxLength={100}
                placeholder="Tên sản phẩm hoặc mã SKU"
                value={searchDraft}
                onChange={(event) => setSearchDraft(event.target.value)}
              />
              <button className="btn btn-outline-success" type="submit">Tìm</button>
            </form>

            {catalogError && (
              <div className="alert alert-danger" role="alert">
                {catalogError}
                <button className="btn btn-sm btn-outline-danger ms-2" type="button" onClick={() => {
                  setLoadingCatalog(true)
                  setCatalogError('')
                  setReloadKey((value) => value + 1)
                }}>Thử lại</button>
              </div>
            )}
            {loadingCatalog && <p className="pos-loading" role="status">Đang tải sản phẩm…</p>}
            {!loadingCatalog && !catalogError && catalog?.items.length === 0 && (
              <p className="pos-empty">Không tìm thấy sản phẩm phù hợp.</p>
            )}
            <div className="pos-product-grid">
              {!loadingCatalog && catalog?.items.map((sku) => (
                <article className="pos-product-card" key={sku.skuId}>
                  <div>
                    <h3>{sku.productName}</h3>
                    <p>{sku.skuName} · {sku.skuCode}</p>
                  </div>
                  <strong className="pos-product-price">{formatVnd(sku.sellingPrice)}</strong>
                  <span className={sku.availableQuantity > 0 ? 'pos-stock' : 'pos-stock pos-stock--empty'}>
                    Tồn kho: {sku.availableQuantity}
                  </span>
                  <button
                    className="btn btn-sm btn-success"
                    type="button"
                    onClick={() => addItem(sku)}
                    disabled={sku.availableQuantity < 1 || processing}
                  >
                    Thêm vào đơn
                  </button>
                </article>
              ))}
            </div>
            <footer className="pos-pagination">
              <span>{catalog?.totalElements || 0} phiên bản sản phẩm</span>
              <div>
                <button className="btn btn-sm btn-outline-secondary" type="button" onClick={() => {
                  setLoadingCatalog(true)
                  setPage((current) => Math.max(0, current - 1))
                }} disabled={page === 0 || loadingCatalog}>Trước</button>
                <span>{totalPages ? `${page + 1} / ${totalPages}` : '0 / 0'}</span>
                <button className="btn btn-sm btn-outline-secondary" type="button" onClick={() => {
                  setLoadingCatalog(true)
                  setPage((current) => current + 1)
                }} disabled={loadingCatalog || page + 1 >= totalPages}>Sau</button>
              </div>
            </footer>
          </section>

          <aside className="pos-checkout-panel" aria-labelledby="pos-checkout-title">
            <div className="pos-panel-heading">
              <div>
                <h2 id="pos-checkout-title">Đơn tại quầy</h2>
                <p>{cart.length} mặt hàng</p>
              </div>
            </div>
            {cart.length === 0 ? (
              <p className="pos-empty">Chọn sản phẩm để bắt đầu giao dịch.</p>
            ) : (
              <ul className="pos-cart-items">
                {cart.map((item) => (
                  <li key={item.skuId}>
                    <div className="pos-cart-description">
                      <strong>{item.productName}</strong>
                      <span>{item.skuName} · {formatVnd(item.sellingPrice)}</span>
                    </div>
                    <div className="pos-quantity-control" aria-label={`Số lượng ${item.productName}`}>
                      <button type="button" aria-label={`Giảm ${item.productName}`} onClick={() => changeQuantity(item.skuId, item.quantity - 1)} disabled={processing}>−</button>
                      <span>{item.quantity}</span>
                      <button type="button" aria-label={`Tăng ${item.productName}`} onClick={() => changeQuantity(item.skuId, item.quantity + 1)} disabled={processing || item.quantity >= item.availableQuantity}>+</button>
                    </div>
                    <strong>{formatVnd((BigInt(item.sellingPrice) * BigInt(item.quantity)).toString())}</strong>
                  </li>
                ))}
              </ul>
            )}

            <form className="pos-payment" onSubmit={submitSale}>
              <label className="form-label" htmlFor="pos-payment-method">Phương thức thanh toán</label>
              <select
                className="form-select"
                id="pos-payment-method"
                value={paymentMethod}
                onChange={(event) => {
                  setPaymentMethod(event.target.value)
                  setSubmissionKey(null)
                }}
                disabled={processing}
              >
                <option value="CASH">Tiền mặt</option>
                <option value="CARD">Thẻ (nhân viên xác nhận đã thu)</option>
                <option value="BANK_TRANSFER">Chuyển khoản (nhân viên xác nhận đã nhận)</option>
              </select>
              <p className="pos-payment-note">Chỉ ghi nhận khoản thanh toán đã nhận tại quầy; không kết nối cổng thẻ hoặc ngân hàng.</p>
              <p className="pos-total"><span>Tổng thanh toán</span><strong>{formatVnd(subtotal)}</strong></p>
              {saleError && <p className="alert alert-danger" role="alert">{saleError}</p>}
              <button className="btn btn-success w-100" type="submit" disabled={processing || cart.length === 0}>
                {processing ? 'Đang ghi nhận giao dịch…' : 'Thanh toán và hoàn tất'}
              </button>
            </form>
          </aside>
        </div>
      )}
    </main>
  )
}
