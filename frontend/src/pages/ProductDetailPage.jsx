import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { getProductDetail } from '../api/productApi'
import { AppFooter, AppHeader } from '../components/AppShell'
import { formatVnd } from '../features/catalog/api'
import { useAuth } from '../auth/useAuth'

// Cart API receives IDs/quantity only and checks current prices and availability.
export function ProductDetailPage({ onAddToCart }) {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const [response, setResponse] = useState(null)
  const [retry, setRetry] = useState(0)
  const requestKey = `${id}:${retry}`
  const current = response?.key === requestKey ? response : null

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    getProductDetail(id, { signal: controller.signal })
      .then((product) => { if (active) setResponse({ key: requestKey, product }) })
      .catch((error) => { if (active) setResponse({ key: requestKey, error }) })
    return () => { active = false; controller.abort() }
  }, [id, requestKey])

  if (!current) return <main className="catalog-page min-vh-100"><AppHeader /><div className="catalog-loading" role="status"><span className="loading-spinner" />Đang tải chi tiết sản phẩm...</div><AppFooter /></main>
  if (current.error) return <main className="catalog-page min-vh-100">
    <AppHeader />
    <div className="container py-5">
      <Link className="back-link" to="/products">← Danh sách sản phẩm</Link>
      <div className="alert alert-danger mt-4" role="alert">{current.error.status === 404
        ? 'Sản phẩm không tồn tại hoặc hiện không được công khai.'
        : current.error.message || 'Không tải được chi tiết sản phẩm.'}</div>
      {current.error.status !== 404 && <button className="btn btn-dark" onClick={() => setRetry((value) => value + 1)}>Thử lại</button>}
    </div>
    <AppFooter />
  </main>

  return <ProductDetailContent key={requestKey + ':' + searchParams.get('sku')}
    product={current.product} initialSku={searchParams.get('sku')} onAddToCart={onAddToCart} />
}

function ProductDetailContent({ product, initialSku, onAddToCart }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const eligibleInitial = product.skus.find((sku) => String(sku.skuId) === initialSku)
  const [selectedId, setSelectedId] = useState(eligibleInitial?.skuId ?? (product.skus.length === 1 ? product.skus[0].skuId : ''))
  const [quantity, setQuantity] = useState('1')
  const [imageChoice, setImageChoice] = useState(null)
  const [failedImage, setFailedImage] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const selected = product.skus.find((sku) => String(sku.skuId) === String(selectedId))
  const image = imageChoice || selected?.imageUrl || product.images[0]
  const amount = Number(quantity)
  const validQuantity = /^\d+$/.test(quantity) && Number.isSafeInteger(amount) && amount >= 1 && amount <= (selected?.availableQuantity ?? 0)
  const customer = user.authenticated && user.role === 'CUSTOMER'
  const canAdd = customer && selected?.purchasable && validQuantity && !!onAddToCart && !submitting

  function chooseSku(value) {
    if (submitting) return
    setSelectedId(value)
    setQuantity('1')
    setImageChoice(null)
    setMessage('')
  }

  async function addToCart(event) {
    event.preventDefault()
    if (!canAdd) return
    setSubmitting(true)
    setMessage('')
    try {
      await onAddToCart({ skuId: selected.skuId, quantity: amount })
      setMessage('Đã thêm sản phẩm vào giỏ hàng.')
    } catch (error) {
      if (error.status === 401) {
        navigate('/login', { state: { from: `/products/${product.productId}?sku=${selected.skuId}` } })
        return
      }
      setMessage(error.message || 'Không thể thêm sản phẩm vào giỏ hàng. Vui lòng thử lại.')
    } finally { setSubmitting(false) }
  }

  return <main className="catalog-page min-vh-100">
    <AppHeader />
    <div className="container product-detail-breadcrumb"><Link to="/products"><span aria-hidden="true">←</span> Danh sách sản phẩm</Link></div>
    <section className="container product-detail">
      <div className="product-gallery">
        <div className="detail-image">{image && failedImage !== image
          ? <img src={image} alt={product.name} onError={() => setFailedImage(image)} />
          : <div className="detail-image-placeholder"><span aria-hidden="true">▧</span><p>Chưa có hình ảnh</p></div>}</div>
        {product.images.length > 1 && <div className="product-thumbnails" aria-label="Hình ảnh sản phẩm">
          {product.images.map((url, index) => <button type="button" className="product-thumbnail" key={url}
            aria-label={`Xem ảnh ${index + 1}`} aria-pressed={image === url} onClick={() => setImageChoice(url)}>
            <img src={url} alt="" onError={(event) => { event.currentTarget.style.display = 'none' }} />
            <span>Ảnh {index + 1}</span>
          </button>)}
        </div>}
      </div>
      <div className="product-detail-copy">
        <div className="product-detail-meta"><span>{product.categoryName}</span><span>{product.brandName}</span></div>
        <h1>{product.name}</h1>
        <div className="product-description"><h2>Thông tin sản phẩm</h2><p>{product.description?.trim() || 'Chưa có mô tả sản phẩm.'}</p></div>
        <form className="product-purchase-card" onSubmit={addToCart}>
          <div className="purchase-section-heading"><div><span className="purchase-step">1</span><strong>Chọn phiên bản</strong></div><small>{product.skus.length} lựa chọn</small></div>
          <label className="form-label visually-hidden" htmlFor="detail-sku">Phiên bản / quy cách</label>
          <select id="detail-sku" className="form-select" value={selectedId} disabled={submitting}
            onChange={(event) => chooseSku(event.target.value)}>
            <option value="" disabled>Chọn phiên bản sản phẩm</option>
            {product.skus.map((sku) => <option value={sku.skuId} key={sku.skuId}>
              {sku.skuName} · {sku.skuCode}{sku.purchasable ? '' : ' · Hết hàng'}
            </option>)}
          </select>
          <div className="product-selection-summary" aria-live="polite">
            {selected ? <><div><small>Mã sản phẩm</small><strong>{selected.skuCode}</strong></div>
              <div className="product-price-row"><div><small>Giá bán</small><p className="detail-price">{formatVnd(selected.sellingPrice)}</p></div>
                <span className={`stock-pill ${selected.purchasable ? 'in-stock' : 'out-of-stock'}`}>{selected.purchasable ? `Còn ${selected.availableQuantity} sản phẩm` : 'Hết hàng'}</span></div></>
              : <p className="selection-prompt">Vui lòng chọn phiên bản để xem giá và tình trạng còn hàng.</p>}
          </div>
          <div className="purchase-section-heading quantity-heading"><div><span className="purchase-step">2</span><label htmlFor="detail-quantity">Số lượng</label></div></div>
          <div className="quantity-control">
            <button type="button" aria-label="Giảm số lượng" disabled={!selected?.purchasable || submitting || amount <= 1}
              onClick={() => setQuantity(String(Math.max(1, amount - 1)))}>−</button>
            <input id="detail-quantity" className="form-control" type="number" min="1" step="1"
              max={selected?.availableQuantity || undefined} value={quantity}
              disabled={!selected?.purchasable || submitting} onChange={(event) => setQuantity(event.target.value)} />
            <button type="button" aria-label="Tăng số lượng" disabled={!selected?.purchasable || submitting || amount >= selected.availableQuantity}
              onClick={() => setQuantity(String(Math.min(selected.availableQuantity, amount + 1)))}>+</button>
          </div>
          {selected?.purchasable && !validQuantity && <p className="text-danger" role="alert">Số lượng phải là số nguyên từ 1 đến {selected.availableQuantity}.</p>}
          <button className="btn btn-dark btn-lg product-add-button" type="submit" disabled={!canAdd}><span aria-hidden="true">＋</span>{submitting ? 'Đang chuyển đến giỏ hàng...' : 'Thêm vào giỏ hàng'}</button>
          {!user.authenticated ? <p className="purchase-help"><Link to="/login" state={{ from: `/products/${product.productId}${selected ? `?sku=${selected.skuId}` : ''}` }}>Đăng nhập</Link> bằng tài khoản khách hàng để mua hàng.</p>
            : !customer ? <p className="purchase-help">Chức năng mua hàng dành cho tài khoản khách hàng.</p>
              : !onAddToCart && <p className="purchase-help">Giỏ hàng hiện chưa sẵn sàng. Vui lòng quay lại sau.</p>}
          {message && <div className={`alert ${message === 'Đã thêm sản phẩm vào giỏ hàng.' ? 'alert-success' : 'alert-danger'} mt-3`} role="alert">{message} {message === 'Đã thêm sản phẩm vào giỏ hàng.' && <Link to="/cart">Xem giỏ hàng</Link>}</div>}
        </form>
      </div>
    </section>
    <AppFooter />
  </main>
}
