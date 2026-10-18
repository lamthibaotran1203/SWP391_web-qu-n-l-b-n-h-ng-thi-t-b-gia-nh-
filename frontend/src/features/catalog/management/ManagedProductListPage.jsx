import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CatalogLoadError, CatalogPageHeading } from './CatalogWorkspace'
import { ProductThumbnail } from './ProductThumbnail'
import { ProductStatus } from './ProductStatus'
import { getManagedProducts } from './api'
import { useCatalogReferences } from './useCatalogData'

export function ManagedProductListPage() {
  const [params, setParams] = useSearchParams()
  const search = params.get('search') || ''
  const status = ['ACTIVE', 'INACTIVE'].includes(params.get('status')) ? params.get('status') : ''
  const page = Math.max(0, Number.parseInt(params.get('page'), 10) || 0)
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState(null)
  const references = useCatalogReferences()
  const requestKey = JSON.stringify({ search, status, page, revision })
  const current = result?.key === requestKey ? result : null

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    getManagedProducts({ search, status, page }, { signal: controller.signal })
      .then((data) => { if (active) setResult({ key: requestKey, data }) })
      .catch((error) => { if (active) setResult({ key: requestKey, error }) })
    return () => { active = false; controller.abort() }
  }, [search, status, page, requestKey])

  function updateQuery(next) {
    setParams({ search, status, page: String(page), ...next })
  }

  return <>
    <CatalogPageHeading title="Quản lý sản phẩm">
      <Link className="btn btn-primary" to="/staff/products/new">Thêm sản phẩm</Link>
    </CatalogPageHeading>
    <form className="management-filters" onSubmit={(event) => {
      event.preventDefault()
      updateQuery({ search: new FormData(event.currentTarget).get('search').trim(), page: '0' })
    }}>
      <div><label htmlFor="management-search" className="form-label">Tên sản phẩm</label>
        <input key={search} id="management-search" name="search" className="form-control" defaultValue={search} maxLength={100}
          placeholder="Ví dụ: nồi cơm, ấm siêu tốc" /></div>
      <div><label htmlFor="management-status" className="form-label">Trạng thái</label>
        <select id="management-status" className="form-select" value={status} onChange={(event) => updateQuery({ status: event.target.value, page: '0' })}>
          <option value="">Tất cả trạng thái</option><option value="ACTIVE">Đang bán</option><option value="INACTIVE">Ngừng bán</option>
        </select></div>
      <button className="btn btn-outline-dark" type="submit">Tìm kiếm</button>
      {(search || status) && <button className="btn btn-link" type="button" onClick={() => setParams({})}>Xóa bộ lọc</button>}
    </form>
    {references.error && <CatalogLoadError error={references.error} onRetry={references.reload} />}
    {current?.error && <CatalogLoadError error={current.error} onRetry={() => setRevision((value) => value + 1)} />}
    {!current && <p className="management-loading" role="status">Đang tải danh sách sản phẩm…</p>}
    {current?.data && <section className="management-list" aria-label="Danh sách sản phẩm">
      <div className="management-list-heading"><h2>Sản phẩm của cửa hàng</h2><span>{current.data.totalElements} sản phẩm</span></div>
      {current.data.items.length ? <div className="table-responsive"><table className="table management-table align-middle">
        <caption className="visually-hidden">Danh sách sản phẩm của cửa hàng</caption>
        <thead><tr><th scope="col">Sản phẩm</th><th scope="col">Danh mục</th><th scope="col">Thương hiệu</th><th scope="col">Trạng thái</th><th scope="col">Tùy chọn</th></tr></thead>
        <tbody>{current.data.items.map((product) => <tr key={product.productId}>
                    <td><Link className="management-product-link" to={`/staff/products/${product.productId}`}>
            <ProductThumbnail src={product.imageUrl} /><strong>{product.name}</strong>
          </Link></td>
          <td>{references.categories.find((item) => item.id === product.categoryId)?.name || '—'}</td>
          <td>{references.brands.find((item) => item.id === product.brandId)?.name || '—'}</td>
          <td><ProductStatus status={product.status} /></td>
          <td><div className="management-row-actions">
                        <Link to={`/staff/products/${product.productId}`} aria-label={`Chi tiết ${product.name}`}>Chi tiết</Link>
                        <Link to={`/staff/products/${product.productId}/edit`} aria-label={`Chỉnh sửa ${product.name}`}>Chỉnh sửa</Link>
          </div></td>
        </tr>)}</tbody>
      </table></div> : <div className="management-empty"><h3>Không tìm thấy sản phẩm</h3>
        <p>{search || status ? 'Thử từ khóa khác hoặc xóa bộ lọc.' : 'Thêm sản phẩm đầu tiên để bắt đầu quản lý.'}</p>
      </div>}
      <nav className="management-pagination" aria-label="Phân trang quản lý">
        <span>Trang {page + 1} · {current.data.totalElements} sản phẩm</span>
        <div><button className="btn btn-outline-secondary btn-sm" disabled={page === 0} onClick={() => updateQuery({ page: String(page - 1) })}>Trước</button>
          <button className="btn btn-outline-secondary btn-sm" disabled={(page + 1) * current.data.size >= current.data.totalElements}
            onClick={() => updateQuery({ page: String(page + 1) })}>Sau</button></div>
      </nav>
    </section>}
  </>
}
