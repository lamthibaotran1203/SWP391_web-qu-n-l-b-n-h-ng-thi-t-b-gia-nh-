import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { getProductDetail } from '../api/productApi'
import { formatVnd } from '../features/catalog/api'
import { useAuth } from '../auth/useAuth'

// UC-10 supplies this callback when its real cart service is integrated.
// It receives IDs/quantity only; prices and stock must be rechecked by UC-10.
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

  if (!current) return <main className="container py-5" role="status">Đang tải chi tiết sản phẩm...</main>
  if (current.error) return <main className="container py-5">
    <Link to="/products">← Danh sách sản phẩm</Link>
    <div className="alert alert-danger mt-4" role="alert">{current.error.status === 404
      ? 'Sản phẩm không tồn tại hoặc hiện không được công khai.'
      : current.error.message || 'Không tải được chi tiết sản phẩm.'}</div>
    {current.error.status !== 404 && <button className="btn btn-dark" onClick={() => setRetry((value) => value + 1)}>Thử lại</button>}
  </main>

  return <ProductDetailContent key={requestKey + ':' + searchParams.get('sku')}
    product={current.product} initialSku={searchParams.get('sku')} onAddToCart={onAddToCart} />
}

function ProductDetailContent({ product, initialSku, onAddToCart }) {
  const { user } = useAuth()
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
    } catch (error) {
      setMessage(error.message || 'Không thể thêm sản phẩm vào giỏ hàng. Vui lòng thử lại.')
    } finally { setSubmitting(false) }
  }

  return <main className="catalog-page min-vh-100">
    <nav className="catalog-nav container"><Link to="/" className="brand-link">Gia Dụng Việt</Link><Link to="/products">← Danh sách</Link></nav>
    <section className="container py-5 product-detail">
      <div>
        <div className="detail-image">{image && failedImage !== image
          ? <img src={image} alt={product.name} onError={() => setFailedImage(image)} style={{ objectFit: 'contain' }} />
          : <span>Chưa có hình ảnh</span>}</div>
        {product.images.length > 1 && <div className="d-flex flex-wrap gap-2 mt-3" aria-label="Hình ảnh sản phẩm">
          {product.images.map((url, index) => <button type="button" className="btn btn-outline-secondary" key={url}
            aria-label={`Xem ảnh ${index + 1}`} aria-pressed={image === url} onClick={() => setImageChoice(url)}>
            <span>Ảnh {index + 1}</span>
          </button>)}
        </div>}
      </div>
      <div>
        <p className="eyebrow">{product.categoryName}</p>
        <h1>{product.name}</h1>
        <p>Thương hiệu: <strong>{product.brandName}</strong></p>
        <div style={{ whiteSpace: 'pre-wrap' }}>{product.description?.trim() || 'Chưa có mô tả sản phẩm.'}</div>
        <form className="mt-4" onSubmit={addToCart}>
          <label className="form-label" htmlFor="detail-sku">Phiên bản / quy cách</label>
          <select id="detail-sku" className="form-select" value={selectedId} disabled={submitting}
            onChange={(event) => chooseSku(event.target.value)}>
            <option value="" disabled>Chọn phiên bản sản phẩm</option>
            {product.skus.map((sku) => <option value={sku.skuId} key={sku.skuId}>
              {sku.skuName} · {sku.skuCode}{sku.purchasable ? '' : ' · Hết hàng'}
            </option>)}
          </select>
          <div className="mt-3" aria-live="polite">
            {selected ? <><p>SKU: {selected.skuCode}</p><p className="detail-price">{formatVnd(selected.sellingPrice)}</p>
              <p className="stock-note">{selected.purchasable ? `Còn ${selected.availableQuantity} sản phẩm` : 'Hết hàng'}</p></>
              : <p>Vui lòng chọn phiên bản để xem giá và tình trạng còn hàng.</p>}
          </div>
          <label className="form-label" htmlFor="detail-quantity">Số lượng</label>
          <input id="detail-quantity" className="form-control mb-3" type="number" min="1" step="1"
            max={selected?.availableQuantity || undefined} value={quantity}
            disabled={!selected?.purchasable || submitting} onChange={(event) => setQuantity(event.target.value)} />
          {selected?.purchasable && !validQuantity && <p className="text-danger" role="alert">Số lượng phải là số nguyên từ 1 đến {selected.availableQuantity}.</p>}
          <button className="btn btn-dark btn-lg" type="submit" disabled={!canAdd}>{submitting ? 'Đang chuyển đến giỏ hàng...' : 'Thêm vào giỏ hàng'}</button>
          {!user.authenticated ? <p className="mt-3"><Link to="/login" state={{ from: `/products/${product.productId}${selected ? `?sku=${selected.skuId}` : ''}` }}>Đăng nhập</Link> bằng tài khoản khách hàng để mua hàng.</p>
            : !customer ? <p className="text-secondary mt-3">Chức năng mua hàng dành cho tài khoản khách hàng.</p>
              : !onAddToCart && <p className="text-secondary mt-3">Giỏ hàng hiện chưa sẵn sàng. Vui lòng quay lại sau.</p>}
          {message && <div className="alert alert-danger mt-3" role="alert">{message}</div>}
        </form>
      </div>
    </section>
  </main>
}
