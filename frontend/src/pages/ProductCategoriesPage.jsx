import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { AppHeader, AppFooter, WorkspaceHeader } from '../components/AppShell'
import { getPublicCategories } from '../features/catalog/api'
import '../features/catalog/categories.css'

export function ProductCategoriesPage() {
  const [response, setResponse] = useState(null)
  const [retry, setRetry] = useState(0)
  useEffect(() => {
    const controller = new AbortController()
    getPublicCategories({ roots: true }, { signal: controller.signal })
      .then((items) => { if (!controller.signal.aborted) setResponse({ retry, items }) })
      .catch((error) => { if (!controller.signal.aborted) setResponse({ retry, error }) })
    return () => controller.abort()
  }, [retry])
  const loading = response?.retry !== retry
  return <main className="catalog-page"><AppHeader /><section className="container catalog-content">
    <WorkspaceHeader eyebrow="Khám phá" title="Danh mục sản phẩm" description="Chọn danh mục để tìm sản phẩm hoặc mở rộng để xem các nhóm nhỏ hơn." />
    <Link to="/products">Xem tất cả sản phẩm →</Link>
    {loading ? <div className="catalog-loading" role="status">Đang tải danh mục...</div>
      : response.error ? <div className="alert alert-danger mt-4" role="alert">{response.error.message} <button className="btn btn-outline-danger btn-sm" onClick={() => setRetry((value) => value + 1)}>Thử lại</button></div>
        : response.items.length === 0 ? <div className="empty-results mt-4"><h2>Chưa có danh mục công khai</h2><p>Cửa hàng đang cập nhật danh mục. Bạn vui lòng quay lại sau.</p><button className="btn btn-outline-dark" onClick={() => setRetry((value) => value + 1)}>Tải lại</button></div>
          : <ul className="category-tree">{response.items.map((category) => <CategoryBranch category={category} key={category.id} />)}</ul>}
  </section><AppFooter /></main>
}

function CategoryBranch({ category }) {
  const [expanded, setExpanded] = useState(false)
  const [response, setResponse] = useState(null)
  const [loading, setLoading] = useState(false)
  const request = useRef(null)
  useEffect(() => () => request.current?.abort(), [])
  async function load() {
    request.current?.abort()
    const controller = new AbortController()
    request.current = controller
    setLoading(true)
    try {
      const items = await getPublicCategories({ parentId: category.id }, { signal: controller.signal })
      if (!controller.signal.aborted) setResponse({ items })
    } catch (error) { if (!controller.signal.aborted) setResponse({ error }) }
    finally { if (!controller.signal.aborted) setLoading(false) }
  }
  function toggle() {
    setExpanded(!expanded)
    if (!expanded && !response) load()
  }
  return <li className="category-branch"><div className="category-card">
    <span className="category-cover">{category.imageUrl ? <img src={category.imageUrl} alt="" onError={(event) => { event.currentTarget.style.display = 'none' }} /> : <span aria-hidden="true">{category.name.slice(0, 1)}</span>}</span>
    <div className="category-copy"><h2><Link to={`/products?categoryId=${category.id}`}>{category.name}</Link></h2>{category.description && <p>{category.description}</p>}<Link to={`/products?categoryId=${category.id}`}>Xem sản phẩm →</Link></div>
    {category.hasChildren && <button className="btn btn-outline-secondary btn-sm" type="button" aria-expanded={expanded} aria-controls={`category-children-${category.id}`} aria-label={`${expanded ? 'Thu gọn' : 'Mở rộng'} ${category.name}`} onClick={toggle}>{expanded ? 'Thu gọn −' : 'Danh mục con +'}</button>}
  </div>{expanded && <div id={`category-children-${category.id}`} className="category-children">
    {loading ? <p role="status">Đang tải danh mục con...</p> : response?.error ? <div className="alert alert-warning" role="alert">{response.error.message} <button className="btn btn-outline-dark btn-sm" onClick={load}>Thử lại</button></div>
      : response?.items.length === 0 ? <p>Chưa có danh mục con công khai.</p>
        : <ul className="category-tree">{response?.items.map((child) => <CategoryBranch key={child.id} category={child} />)}</ul>}
  </div>}</li>
}
