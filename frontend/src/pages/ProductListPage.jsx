import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getProducts } from '../api/productApi'
import { AppFooter, AppHeader, WorkspaceHeader } from '../components/AppShell'
import { formatVnd, getCatalogBrands, getCatalogCategories } from '../features/catalog/api'

const defaultCriteria = { search: '', brand: '', category: '', minPrice: '', maxPrice: '', sort: 'newest' }

export function ProductListPage() {
  const [draft, setDraft] = useState(defaultCriteria)
  const [criteria, setCriteria] = useState(defaultCriteria)
  const [page, setPage] = useState(0)
  const [result, setResult] = useState({ content: [], totalPages: 0, totalElements: 0 })
  const [brands, setBrands] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    Promise.all([getCatalogBrands(), getCatalogCategories()])
      .then(([brandOptions, categoryOptions]) => {
        setBrands(brandOptions)
        setCategories(categoryOptions)
      })
      .catch(() => {})
  }, [])

  const loadProducts = useCallback(() => {
    setLoading(true)
    setError('')
    getProducts({ ...criteria, page })
      .then((data) => {
        setResult(data)
        if (typeof data.number === 'number' && data.number !== page) setPage(data.number)
      })
      .catch((requestError) => setError(requestError.message))
      .finally(() => setLoading(false))
  }, [criteria, page])

  useEffect(() => {
    const timer = setTimeout(loadProducts, 0)
    return () => clearTimeout(timer)
  }, [loadProducts])

  function submit(event) {
    event.preventDefault()
    setCriteria({ ...draft, search: draft.search.replace(/\s+/g, ' ').trim() })
    setPage(0)
  }

  function clearCriteria() {
    setDraft(defaultCriteria)
    setCriteria(defaultCriteria)
    setPage(0)
  }

  const hasCriteria = Object.entries(criteria).some(([key, value]) => key === 'sort' ? value !== 'newest' : value)

  return (
    <main className="catalog-page">
      <AppHeader />
      <section className="container catalog-content">
        <WorkspaceHeader eyebrow="Danh mục sản phẩm" title="Đồ gia dụng cho ngôi nhà hiện đại." description="Lựa chọn sản phẩm phù hợp theo thương hiệu, danh mục và ngân sách.">
          <span className="result-count"><strong>{result.totalElements || 0}</strong><small>sản phẩm</small></span>
        </WorkspaceHeader>
        <form className="catalog-filters" onSubmit={submit}>
          <input className="form-control" value={draft.search} onChange={(event) => setDraft({ ...draft, search: event.target.value })} placeholder="Tên, thương hiệu hoặc SKU" aria-label="Tìm sản phẩm" />
          <select className="form-select" value={draft.brand} onChange={(event) => setDraft({ ...draft, brand: event.target.value })} aria-label="Lọc thương hiệu"><option value="">Thương hiệu</option>{brands.map((brand) => <option value={brand.name} key={brand.id}>{brand.name}</option>)}</select>
          <select className="form-select" value={draft.category} onChange={(event) => setDraft({ ...draft, category: event.target.value })} aria-label="Lọc danh mục"><option value="">Danh mục</option>{categories.map((category) => <option value={category.name} key={category.id}>{category.name}</option>)}</select>
          <input className="form-control" type="number" min="0" value={draft.minPrice} onChange={(event) => setDraft({ ...draft, minPrice: event.target.value })} placeholder="Giá từ" aria-label="Giá từ" />
          <input className="form-control" type="number" min="0" value={draft.maxPrice} onChange={(event) => setDraft({ ...draft, maxPrice: event.target.value })} placeholder="Giá đến" aria-label="Giá đến" />
          <select className="form-select" value={draft.sort} onChange={(event) => { const sort = event.target.value; setDraft({ ...draft, sort }); setCriteria({ ...criteria, sort }); setPage(0) }} aria-label="Sắp xếp"><option value="newest">Mới nhất</option><option value="oldest">Cũ nhất</option><option value="name_asc">Tên A-Z</option><option value="name_desc">Tên Z-A</option><option value="price_asc">Giá thấp đến cao</option><option value="price_desc">Giá cao đến thấp</option></select>
          <button className="btn btn-dark" type="submit">Tìm kiếm</button>
          {hasCriteria && <button className="btn btn-outline-secondary" type="button" onClick={clearCriteria}>Xóa tiêu chí</button>}
        </form>
        {error && <div className="alert alert-danger d-flex justify-content-between align-items-center"><span>{error}</span><button className="btn btn-sm btn-danger" onClick={loadProducts}>Thử lại</button></div>}
        {loading ? <div className="catalog-loading" role="status"><span className="loading-spinner" />Đang tải sản phẩm...</div> : result.content.length === 0 ? <div className="empty-results"><span className="empty-state-icon" aria-hidden="true">⌕</span><h2>Không tìm thấy sản phẩm</h2><p>Hãy thử sửa tiêu chí tìm kiếm hoặc xóa bộ lọc.</p><button className="btn btn-outline-dark" onClick={clearCriteria}>Xóa tiêu chí</button></div> : <>
          <div className="product-grid">{result.content.map((product) => <Link to={`/products/${product.id}`} className="product-card" key={product.id}><div className="product-image">{product.imageUrl ? <img src={product.imageUrl} alt={product.name} /> : <span>Gia dụng</span>}</div><div className="p-3"><small className="text-secondary">{product.brand || product.category || 'Sản phẩm gia dụng'}</small><h2>{product.name}</h2><strong>{formatVnd(product.price)}</strong><p className="stock-note">SKU: {product.sku} · Còn {product.stockQuantity}</p></div></Link>)}</div>
          {result.totalPages > 1 && <nav className="catalog-pagination" aria-label="Phân trang"><button className="btn btn-outline-dark" disabled={page === 0} onClick={() => setPage(page - 1)}>Trước</button><span>Trang {page + 1} / {result.totalPages}</span><button className="btn btn-outline-dark" disabled={page + 1 >= result.totalPages} onClick={() => setPage(page + 1)}>Sau</button></nav>}
        </>}
      </section>
      <AppFooter />
    </main>
  )
}
