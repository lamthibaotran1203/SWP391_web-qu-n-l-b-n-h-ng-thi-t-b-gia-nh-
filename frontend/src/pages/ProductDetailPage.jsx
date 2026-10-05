import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getProductById } from '../api/productApi'

const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' })

export function ProductDetailPage() {
  const { id } = useParams()
  const [product, setProduct] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getProductById(id).then(setProduct).catch((requestError) => setError(requestError.message))
  }, [id])

  if (error) return <main className="container py-5"><Link to="/products">← Danh sách sản phẩm</Link><div className="alert alert-danger mt-4">{error}</div></main>
  if (!product) return <main className="container py-5">Đang tải sản phẩm...</main>

  return (
    <main className="catalog-page min-vh-100">
      <nav className="catalog-nav container"><Link to="/" className="brand-link">Gia Dụng Việt</Link><Link to="/products">← Danh sách</Link></nav>
      <section className="container py-5 product-detail">
        <div className="detail-image">{product.imageUrl ? <img src={product.imageUrl} alt={product.name} /> : <span>Gia dụng</span>}</div>
        <div>
          <p className="eyebrow">{product.category || 'Sản phẩm gia dụng'}</p>
          <h1>{product.name}</h1>
          <p className="detail-price">{money.format(product.price)}</p>
          <p>{product.description || 'Sản phẩm chất lượng cho không gian sống tiện nghi.'}</p>
          <p className="stock-note">Còn {product.stockQuantity} sản phẩm trong kho</p>
          <button className="btn btn-dark btn-lg" type="button" disabled={!product.stockQuantity}>Thêm vào giỏ hàng</button>
        </div>
      </section>
    </main>
  )
}
