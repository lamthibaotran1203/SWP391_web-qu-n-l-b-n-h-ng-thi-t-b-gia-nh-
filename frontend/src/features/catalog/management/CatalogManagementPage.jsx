import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../../auth/useAuth'
import { formatVnd, getCatalogBrands, getCatalogCategories } from '../api'
import { addManagedSku, createManagedProduct, editManagedProduct, editManagedSku, getManagedProduct, getManagedProducts } from './api'
import { ProductEditor } from './ProductEditor'
import { SkuEditor } from './SkuEditor'
import { ProductThumbnail } from './ProductThumbnail'

export function CatalogManagementPage() {
  const { user, connection } = useAuth()
  const [query, setQuery] = useState({ search: '', status: '', page: 0 })
  const [search, setSearch] = useState('')
  const [data, setData] = useState(null)
  const [references, setReferences] = useState({ categories: [], brands: [] })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [selected, setSelected] = useState(null)
  const [creating, setCreating] = useState(false)
  const [editingProduct, setEditingProduct] = useState(false)
  const [editingSku, setEditingSku] = useState(undefined)
  const [revision, setRevision] = useState(0)
  const [notice, setNotice] = useState('')
  const manager = user.authenticated && user.role === 'MANAGER'

  useEffect(() => {
    if (!manager) return
    let active = true
    Promise.all([getManagedProducts(query), getCatalogCategories(), getCatalogBrands()])
      .then(([page, categories, brands]) => { if (active) { setData(page); setReferences({ categories, brands }); setError('') } })
      .catch((failure) => { if (active) setError(failure.message) })
    return () => { active = false }
  }, [manager, query, revision])

  async function openProduct(id) {
    setBusy(true); setError(''); setNotice('')
    try { setSelected(await getManagedProduct(id)); setCreating(false); setEditingProduct(false); setEditingSku(undefined) }
    catch (failure) { setError(failure.message) }
    finally { setBusy(false) }
  }
  function saved(product) {
    setSelected(product); setCreating(false); setEditingProduct(false); setEditingSku(undefined)
    setRevision((current) => current + 1); setNotice('Đã lưu thay đổi.')
  }
  async function saveProduct(fields, skus) {
    const result = creating ? await createManagedProduct(fields, skus)
      : await editManagedProduct(selected.productId, selected.productVersion, fields)
    saved(result)
  }
  async function saveSku(fields) {
    const result = editingSku ? await editManagedSku(selected.productId, editingSku.skuId, selected.productVersion, editingSku.skuVersion, fields)
      : await addManagedSku(selected.productId, selected.productVersion, fields)
    saved(result)
  }

  if (connection.status === 'loading') return <main className="plain-page">Đang kiểm tra phiên…</main>
  if (!user.authenticated) return <Navigate to="/login" replace />
  if (!manager) return <main className="plain-page"><div className="plain-container"><h1>Không có quyền quản lý danh mục</h1><Link to="/">Trang chủ</Link></div></main>

  return <main className="plain-page"><div className="plain-container">
    <Link to="/">Trang chủ</Link><h1 className="mt-3">Quản lý sản phẩm</h1>
    {error && <div role="alert" className="alert alert-danger">{error} <button className="btn btn-link" onClick={() => setRevision((current) => current + 1)}>Thử tải lại</button></div>}
    {notice && <p role="status" className="alert alert-success">{notice}</p>}
    <form className="d-flex flex-wrap gap-2 mb-3" onSubmit={(event) => { event.preventDefault(); setQuery({ ...query, search, page: 0 }) }}>
      <label className="visually-hidden" htmlFor="management-search">Tìm tên sản phẩm</label>
      <input id="management-search" className="form-control w-auto" value={search} onChange={(event) => setSearch(event.target.value)} maxLength={100} placeholder="Tìm tên sản phẩm" />
      <label className="visually-hidden" htmlFor="management-status">Lọc trạng thái</label>
      <select id="management-status" className="form-select w-auto" value={query.status} onChange={(event) => setQuery({ ...query, status: event.target.value, page: 0 })}>
        <option value="">Tất cả trạng thái</option><option value="ACTIVE">Đang bán</option><option value="INACTIVE">Ngừng bán</option>
      </select>
      <button className="btn btn-outline-dark" type="submit">Tìm kiếm</button>
      <button className="btn btn-dark" type="button" disabled={busy} onClick={() => { setSelected(null); setCreating(true); setEditingSku(undefined); setNotice('') }}>Thêm sản phẩm</button>
    </form>
    {!data && !error && <p role="status">Đang tải sản phẩm…</p>}
    {data && <>
      <div className="table-responsive"><table className="table align-middle"><caption className="visually-hidden">Danh sách sản phẩm của cửa hàng</caption>
        <thead><tr><th scope="col">Sản phẩm</th><th scope="col">Trạng thái</th><th scope="col"><span className="visually-hidden">Xem và chỉnh sửa</span></th></tr></thead>
        <tbody>{data.items.map((product) => <tr key={product.productId}><td><div className="management-product"><ProductThumbnail src={product.imageUrl} /><span>{product.name}</span></div></td><td>{product.status === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán'}</td>
          <td><button className="btn btn-outline-dark btn-sm" disabled={busy} onClick={() => openProduct(product.productId)} aria-label={`Chi tiết ${product.name}`}>Chi tiết</button></td></tr>)}</tbody>
      </table></div>
      {data.items.length === 0 && <p>Không tìm thấy sản phẩm. Hãy thử từ khóa hoặc trạng thái khác.</p>}
      <nav className="d-flex gap-3 align-items-center mb-4" aria-label="Phân trang quản lý">
        <button className="btn btn-outline-secondary" disabled={query.page === 0} onClick={() => setQuery({ ...query, page: query.page - 1 })}>Trước</button>
        <span>Trang {query.page + 1} · {data.totalElements} sản phẩm</span>
        <button className="btn btn-outline-secondary" disabled={(query.page + 1) * data.size >= data.totalElements} onClick={() => setQuery({ ...query, page: query.page + 1 })}>Sau</button>
      </nav>
    </>}
    {creating && <section className="border-top pt-4"><h2>Thêm sản phẩm</h2><ProductEditor key="create" {...references} onBusy={setBusy} onSave={saveProduct} onCancel={() => setCreating(false)} /></section>}
    {selected && <section className="border-top pt-4" aria-label="Chi tiết quản lý">
      <div className="management-product mb-3"><ProductThumbnail src={selected.imageUrl} /><h2 className="mb-0">{selected.name}</h2></div>
      <div className="d-flex flex-wrap gap-2 mb-3">
        <button className="btn btn-outline-dark" disabled={busy} onClick={() => { setEditingProduct(true); setEditingSku(undefined) }}>Chỉnh sửa sản phẩm</button>
        <button className="btn btn-outline-secondary" disabled={busy} onClick={() => openProduct(selected.productId)}>Làm mới</button>
        <button className="btn btn-dark" disabled={busy} onClick={() => { setEditingSku(null); setEditingProduct(false) }}>Thêm phân loại</button>
      </div>
      <div className="table-responsive"><table className="table"><caption>Danh sách phân loại</caption>
        <thead><tr><th scope="col">Phân loại / Mã sản phẩm</th><th scope="col">Giá bán</th><th scope="col">Có thể bán</th><th scope="col">Trạng thái</th><th scope="col"><span className="visually-hidden">Xem và chỉnh sửa</span></th></tr></thead>
        <tbody>{selected.skus.map((sku) => <tr key={sku.skuId}><td>{sku.skuName}<br />{sku.skuCode} / {sku.barcode}</td>
          <td>{formatVnd(sku.sellingPrice)}</td><td>{sku.availableQuantity}{sku.availableQuantity === 0 ? <strong className="d-block">Hết hàng</strong> : sku.lowStock && <strong className="d-block">Sắp hết hàng</strong>}</td>
          <td>{sku.status === 'ACTIVE' ? 'Đang bán' : 'Ngừng bán'}</td><td><button className="btn btn-outline-dark btn-sm" aria-label={`Sửa SKU ${sku.skuCode}`} disabled={busy}
            onClick={() => { setEditingSku(sku); setEditingProduct(false) }}>Sửa</button></td></tr>)}</tbody>
      </table></div>
      {editingProduct && <ProductEditor key={`product-${selected.productId}-${selected.productVersion}`} product={selected} {...references} onBusy={setBusy} onSave={saveProduct} onCancel={() => setEditingProduct(false)} />}
      {editingSku !== undefined && <SkuEditor key={`sku-${editingSku?.skuId || 'new'}-${selected.productVersion}`} sku={editingSku} onBusy={setBusy} onSave={saveSku} onCancel={() => setEditingSku(undefined)} />}
    </section>}
  </div></main>
}
