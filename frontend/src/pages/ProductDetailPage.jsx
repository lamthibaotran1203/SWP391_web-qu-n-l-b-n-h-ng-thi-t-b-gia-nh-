import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProductById } from '../api/productApi'
import { AppFooter, AppHeader } from '../components/AppShell'
import { formatVnd } from '../features/catalog/api'

export function ProductDetailPage() {
  const { id } = useParams()
  const [product, setProduct] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getProductById(id).then(setProduct).catch((requestError) => setError(requestError.message))
  }, [id])

  if (error) return <main className="catalog-page min-vh-100"><AppHeader /><div className="container py-5"><Link className="back-link" to="/products">← Danh sách sản phẩm</Link><div className="alert alert-danger mt-4">{error}</div></div></main>
  if (!product) return <main className="catalog-page min-vh-100"><AppHeader /><div className="catalog-loading" role="status"><span className="loading-spinner" />Đang tải sản phẩm...</div></main>

  return (
    <main className="catalog-page min-vh-100">
      <AppHeader />
      <div className="container product-detail-breadcrumb"><Link to="/products">← Quay lại danh sách sản phẩm</Link></div>
      <section className="container product-detail">
        <div className="detail-image">{product.imageUrl ? <img src={product.imageUrl} alt={product.name} /> : <span>Gia dụng</span>}</div>
        <div className="product-detail-copy">
          <p className="eyebrow">{product.category || 'Sản phẩm gia dụng'}</p>
          <h1>{product.name}</h1>
          <p className="detail-price">{formatVnd(product.price)}</p>
          <p>{product.description || 'Sản phẩm chất lượng cho không gian sống tiện nghi.'}</p>
          <p className={`stock-pill ${product.stockQuantity ? 'in-stock' : 'out-of-stock'}`}>{product.stockQuantity ? `Còn ${product.stockQuantity} sản phẩm trong kho` : 'Sản phẩm đang tạm hết hàng'}</p>
          <div className="product-detail-actions"><button className="btn btn-primary btn-lg" type="button" disabled={!product.stockQuantity}>Thêm vào giỏ hàng</button><Link className="btn btn-outline-secondary btn-lg" to="/products">Tiếp tục xem</Link></div>
        </div>
      </section>
      <AppFooter />
    </main>
  )
}
