import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getProducts } from '../api/productApi'
import { AppFooter, AppHeader, WorkspaceHeader } from '../components/AppShell'
import { formatVnd, getCatalogBrands, getPublicCategories } from '../features/catalog/api'
import '../features/catalog/categories.css'

const defaults = { search: '', brandId: '', categoryId: '', minPrice: '', maxPrice: '', availability: 'any', sort: 'newest' }

export function ProductListPage() {
  const [params, setParams] = useSearchParams()
  const criteria = useMemo(() => Object.fromEntries(Object.entries(defaults).map(([key, value]) => [key, params.get(key) ?? value])), [params])
  const page = params.get('page') ?? '0'
  function apply(next, nextPage = 0) {
    const query = new URLSearchParams()
    Object.entries(next).forEach(([key, value]) => { if (String(value).trim() && value !== defaults[key]) query.set(key, String(value).trim()) })
    if (Number(nextPage) !== 0) query.set('page', String(nextPage))
    setParams(query)
  }
  return <CatalogueResults criteria={criteria} page={page} apply={apply} />
}

function ProductPrice({ product }) {
  return <>{formatVnd(product.price)}{product.maxPrice && product.maxPrice !== product.price && <> – {formatVnd(product.maxPrice)}</>}</>
}

function CatalogueResults({ criteria, page, apply }) {
  const criteriaKey = JSON.stringify(criteria)
  const [form, setForm] = useState({ key: criteriaKey, draft: criteria })
  const draft = form.key === criteriaKey ? form.draft : criteria
  function setDraft(next) { setForm({ key: criteriaKey, draft: next }) }
  const [response, setResponse] = useState(null)
  const [retry, setRetry] = useState(0)
  const requestKey = `${criteriaKey}:${page}:${retry}`
  const [references, setReferences] = useState({ brands: [], categories: [], error: '' })
  const [referenceRetry, setReferenceRetry] = useState(0)
  const [suggestions, setSuggestions] = useState({ query: '', items: [], error: '', loading: false })
  const [showSuggestions, setShowSuggestions] = useState(false)
  const suggestionQuery = draft.search.replace(/\s+/g, ' ').trim()
  const searching = ['search','brandId','categoryId','minPrice','maxPrice'].some((key) => criteria[key]) || criteria.availability !== 'any'
  const hasCriteria = searching || criteria.sort !== 'newest'
  useEffect(() => {
    const controller = new AbortController()
    Promise.all([getCatalogBrands({ signal: controller.signal }), getPublicCategories({}, { signal: controller.signal })])
      .then(([brands, categories]) => { if (!controller.signal.aborted) setReferences({ brands, categories, error: '' }) })
      .catch((error) => { if (!controller.signal.aborted) setReferences({ brands: [], categories: [], error: error.message }) })
    return () => controller.abort()
  }, [referenceRetry])
  useEffect(() => {
    const controller = new AbortController()
    getProducts({ ...criteria, page: Number(page) }, { signal: controller.signal })
      .then((data) => { if (!controller.signal.aborted) setResponse({ key: requestKey, data }) })
      .catch((error) => { if (!controller.signal.aborted) setResponse({ key: requestKey, error }) })
    return () => controller.abort()
  }, [criteria, page, requestKey])
  useEffect(() => {
    if (suggestionQuery.length < 2 || !showSuggestions) return
    const controller = new AbortController()
    const timer = setTimeout(() => {
      setSuggestions({ query: suggestionQuery, items: [], loading: true, error: '' })
      getProducts({ search: suggestionQuery, page: 0, size: 5, sort: 'name_asc' }, { signal: controller.signal })
        .then((data) => { if (!controller.signal.aborted) setSuggestions({ query: suggestionQuery, items: data.content, loading: false, error: '' }) })
        .catch((error) => { if (!controller.signal.aborted) setSuggestions({ query: suggestionQuery, items: [], loading: false, error: error.message }) })
    }, 300)
    return () => { clearTimeout(timer); controller.abort() }
  }, [suggestionQuery, showSuggestions])
  const loading = response?.key !== requestKey
  const result = loading ? null : response?.data
  const error = loading ? null : response?.error
  const currentPage = result?.number ?? Number(page)
  function submit(event) {
    event.preventDefault()
    setShowSuggestions(false)
    const next = { ...draft, search: suggestionQuery }
    if (JSON.stringify(next) === JSON.stringify(criteria) && Number(page) === 0) setRetry((value) => value + 1)
    else apply(next)
  }
  function clear() { if (!hasCriteria && Number(page) === 0) setRetry((value) => value + 1); else apply(defaults) }
  function filter(key, value) { setShowSuggestions(false); apply({ ...draft, search: suggestionQuery, [key]: value }) }
  return <main className="catalog-page"><AppHeader /><section className="container catalog-content">
    <WorkspaceHeader eyebrow="Danh mục sản phẩm" title="Đồ gia dụng cho ngôi nhà hiện đại." description="Khám phá sản phẩm phù hợp với nhu cầu và ngân sách của bạn.">
      <span className="result-count"><strong>{result?.totalElements ?? 0}</strong><small>sản phẩm</small></span>
    </WorkspaceHeader>
    <div className="catalogue-intro"><Link to="/categories">Khám phá theo danh mục →</Link><small>Giá bán đã bao gồm VAT · Đơn vị VND</small></div>
    <form className="catalog-filters" onSubmit={submit}>
      <div className="catalog-search-field"><input className="form-control" value={draft.search} maxLength={100} placeholder="Tìm kiếm" aria-label="Tìm sản phẩm" autoComplete="off" onFocus={() => setShowSuggestions(true)} onChange={(event) => { setDraft({ ...draft, search: event.target.value }); setShowSuggestions(true) }} />
        {showSuggestions && suggestionQuery.length >= 2 && suggestions.query === suggestionQuery && <div className="catalog-suggestions" aria-label="Gợi ý sản phẩm">
          {suggestions.loading ? <span className="catalog-suggestion-status">Đang tìm gợi ý...</span> : suggestions.error ? <span className="catalog-suggestion-status text-danger">{suggestions.error}</span>
            : suggestions.items.length === 0 ? <span className="catalog-suggestion-status">Không có gợi ý phù hợp.</span>
              : suggestions.items.map((product) => <Link key={product.productId} className="catalog-suggestion" to={`/products/${product.productId}`}><span><strong>{product.name}</strong><small>{product.brand}</small></span><span><ProductPrice product={product} /></span></Link>)}
        </div>}
      </div>
      <select className="form-select" value={draft.brandId} aria-label="Lọc thương hiệu" onChange={(event) => filter('brandId', event.target.value)}><option value="">Thương hiệu</option>{draft.brandId && !references.brands.some((b) => String(b.id) === draft.brandId) && <option value={draft.brandId}>Thương hiệu #{draft.brandId}</option>}{references.brands.map((brand) => <option value={brand.id} key={brand.id}>{brand.name}</option>)}</select>
      <select className="form-select" value={draft.categoryId} aria-label="Lọc danh mục" onChange={(event) => filter('categoryId', event.target.value)}><option value="">Danh mục</option>{draft.categoryId && !references.categories.some((c) => String(c.id) === draft.categoryId) && <option value={draft.categoryId}>Danh mục #{draft.categoryId}</option>}{references.categories.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}</select>
      <input className="form-control" type="number" min="0" step="1" max="999999999999999999" value={draft.minPrice} aria-label="Giá từ" placeholder="Giá từ" onChange={(event) => setDraft({ ...draft, minPrice: event.target.value })} onBlur={() => { if (draft.minPrice !== criteria.minPrice) filter('minPrice', draft.minPrice) }} />
      <input className="form-control" type="number" min="0" step="1" max="999999999999999999" value={draft.maxPrice} aria-label="Giá đến" placeholder="Giá đến" onChange={(event) => setDraft({ ...draft, maxPrice: event.target.value })} onBlur={() => { if (draft.maxPrice !== criteria.maxPrice) filter('maxPrice', draft.maxPrice) }} />
      <select className="form-select" value={draft.availability} aria-label="Tình trạng còn hàng" onChange={(event) => filter('availability', event.target.value)}><option value="any">Tất cả tồn kho</option><option value="in_stock">Còn hàng</option><option value="out_of_stock">Hết hàng</option></select>
      <select className="form-select" value={draft.sort} aria-label="Sắp xếp" onChange={(event) => apply({ ...criteria, sort: event.target.value })}><option value="newest">Mới nhất</option><option value="oldest">Cũ nhất</option><option value="name_asc">Tên A-Z</option><option value="name_desc">Tên Z-A</option><option value="price_asc">Giá thấp đến cao</option><option value="price_desc">Giá cao đến thấp</option></select>
      <button className="btn btn-dark" type="submit">Tìm kiếm</button>{hasCriteria && <button className="btn btn-outline-secondary" type="button" onClick={clear}>Xóa</button>}
    </form>
    {references.error && <div className="alert alert-warning" role="alert">{references.error} <button className="btn btn-sm btn-outline-dark" onClick={() => setReferenceRetry((value) => value + 1)}>Tải lại bộ lọc</button></div>}
    {error ? <div className="alert alert-danger" role="alert">{error.message} <button className="btn btn-sm btn-danger" onClick={() => setRetry((value) => value + 1)}>Thử lại</button></div>
      : loading ? <div className="catalog-loading" role="status">Đang tải sản phẩm...</div>
        : result.content.length === 0 ? <div className="empty-results"><h2>{searching ? 'Không tìm thấy sản phẩm' : 'Chưa có sản phẩm công khai'}</h2><p>{searching ? 'Hãy thử sửa tiêu chí tìm kiếm hoặc xóa bộ lọc.' : 'Cửa hàng đang cập nhật danh mục. Bạn vui lòng quay lại sau.'}</p><button className="btn btn-outline-dark" onClick={clear}>{searching ? 'Xóa' : 'Tải lại'}</button></div>
          : <><div className="product-grid">{result.content.map((product) => <Link key={product.productId} to={`/products/${product.productId}`} className="product-card"><div className="product-image">{product.imageUrl ? <img src={product.imageUrl} alt={product.name} onError={(event) => { event.currentTarget.style.visibility = 'hidden' }} /> : <span>Gia dụng</span>}</div><div className="p-3"><small className="text-secondary">{product.brand} · {product.category}</small><h2>{product.name}</h2><strong><ProductPrice product={product} /></strong><p className="stock-note">{product.stockQuantity > 0 ? `Còn ${product.stockQuantity} sản phẩm` : 'Hết hàng'} · {product.skuCount} phiên bản</p></div></Link>)}</div>
            {result.totalPages > 1 && <nav className="catalog-pagination" aria-label="Phân trang"><button className="btn btn-outline-dark" disabled={currentPage === 0} onClick={() => apply(criteria, currentPage - 1)}>Trước</button><span>Trang {currentPage + 1} / {result.totalPages}</span><button className="btn btn-outline-dark" disabled={currentPage + 1 >= result.totalPages} onClick={() => apply(criteria, currentPage + 1)}>Sau</button></nav>}</>}
  </section><AppFooter /></main>
}
