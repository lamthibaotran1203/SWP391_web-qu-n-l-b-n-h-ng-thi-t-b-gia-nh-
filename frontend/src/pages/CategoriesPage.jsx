import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import {
  createCategory,
  deactivateCategory,
  getCategories,
  getCategory,
  getCategoryOptions,
  updateCategory,
} from '../features/categories/api'
import '../features/categories/categories.css'

const emptyForm = { code: '', name: '', description: '', parentId: '' }

export function CategoriesPage() {
  const { user, connection, reload: reloadAuth } = useAuth()
  const location = useLocation()
  const [categories, setCategories] = useState([])
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [notice, setNotice] = useState('')
  const [revision, setRevision] = useState(0)
  const [modal, setModal] = useState(null)
  const [selected, setSelected] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [options, setOptions] = useState([])
  const [formErrors, setFormErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (connection.status !== 'connected' || !user.authenticated || user.role !== 'ADMIN') return undefined
    let active = true
    getCategories({ keyword, status, page, size: 10 })
      .then((result) => {
        if (!active) return
        setCategories(result?.content || [])
        setTotalPages(result?.totalPages || 0)
        setTotalElements(result?.totalElements || 0)
        setLoadError('')
      })
      .catch((error) => active && setLoadError(error.message || 'Không thể tải danh mục.'))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [connection.status, user.authenticated, user.role, keyword, status, page, revision])

  if (connection.status === 'loading') return <p role="status">Đang kiểm tra phiên đăng nhập…</p>
  if (connection.status === 'error') return <div className="alert alert-danger" role="alert">
    <p>{connection.message || 'Chưa kết nối được máy chủ.'}</p>
    <button className="btn btn-outline-danger" type="button" onClick={reloadAuth}>Thử kết nối lại</button>
  </div>
  if (!user.authenticated) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (user.role !== 'ADMIN') return <div role="alert"><h1>Không có quyền quản lý danh mục</h1><Link to="/">Về trang chủ</Link></div>

  function closeModal() {
    if (submitting) return
    setModal(null)
    setSelected(null)
    setFormErrors({})
  }

  async function loadOptions(excludeId) {
    try {
      setOptions(await getCategoryOptions(excludeId) || [])
    } catch (error) {
      setFormErrors({ general: error.message || 'Không thể tải danh mục cha.' })
    }
  }

  function openCreate() {
    setSelected(null)
    setForm(emptyForm)
    setFormErrors({})
    setModal('create')
    loadOptions()
  }

  function openEdit(category) {
    setSelected(category)
    setForm({
      code: category.code,
      name: category.name,
      description: category.description || '',
      parentId: category.parentId ? String(category.parentId) : '',
    })
    setFormErrors({})
    setModal('edit')
    loadOptions(category.id)
  }

  async function openDetails(category) {
    setSelected(category)
    setModal('details')
    setFormErrors({})
    try {
      setSelected(await getCategory(category.id))
    } catch (error) {
      setFormErrors({ general: error.message || 'Không thể tải chi tiết danh mục.' })
    }
  }

  function openDeactivate(category) {
    setSelected(category)
    setFormErrors({})
    setModal('deactivate')
  }

  function validateForm() {
    const errors = {}
    if (!form.code.trim()) errors.code = 'Vui lòng nhập mã danh mục.'
    else if (!/^[A-Za-z0-9][A-Za-z0-9_-]*$/.test(form.code.trim())) errors.code = 'Mã chỉ gồm chữ, số, gạch ngang hoặc gạch dưới.'
    if (!form.name.trim()) errors.name = 'Vui lòng nhập tên danh mục.'
    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function submitForm(event) {
    event.preventDefault()
    if (!validateForm()) return
    setSubmitting(true)
    setFormErrors({})
    const payload = {
      code: form.code.trim(),
      name: form.name.trim(),
      description: form.description.trim() || null,
      parentId: form.parentId ? Number(form.parentId) : null,
    }
    try {
      if (modal === 'create') {
        await createCategory(payload)
        setNotice('Đã tạo danh mục thành công.')
      } else {
        await updateCategory(selected.id, payload)
        setNotice('Đã cập nhật danh mục thành công.')
      }
      setModal(null)
      setPage(0)
      setLoading(true)
      setRevision((value) => value + 1)
    } catch (error) {
      setFormErrors({ ...error.fieldErrors, general: error.message || 'Không thể lưu danh mục.' })
    } finally {
      setSubmitting(false)
    }
  }

  async function confirmDeactivate() {
    setSubmitting(true)
    setFormErrors({})
    try {
      await deactivateCategory(selected.id)
      setNotice('Đã ngừng hoạt động danh mục.')
      setModal(null)
      setLoading(true)
      setRevision((value) => value + 1)
    } catch (error) {
      setFormErrors({ general: error.message || 'Không thể ngừng danh mục.' })
    } finally {
      setSubmitting(false)
    }
  }

  function submitSearch(event) {
    event.preventDefault()
    setLoading(true)
    setPage(0)
    setKeyword(keywordInput.trim())
  }

  function resetFilters() {
    setLoading(true)
    setKeywordInput('')
    setKeyword('')
    setStatus('')
    setPage(0)
  }

  return <main className="workspace-page">
    <div className="container categories-page">
      <div className="categories-header">
        <div><h1>Quản lý danh mục sản phẩm</h1><p>Tạo và duy trì cấu trúc danh mục sử dụng trong catalogue.</p></div>
        <button className="btn btn-primary" type="button" onClick={openCreate}>Thêm danh mục</button>
      </div>

      {notice && <div className="alert alert-success alert-dismissible" role="status">{notice}<button className="btn-close" aria-label="Đóng" onClick={() => setNotice('')} /></div>}
      {loadError && <div className="alert alert-danger" role="alert">{loadError}</div>}

      <form className="categories-panel row g-3 align-items-end" onSubmit={submitSearch}>
        <div className="col-md-6"><label className="form-label" htmlFor="category-search">Tìm kiếm</label><input id="category-search" className="form-control" placeholder="Nhập mã, tên hoặc mô tả..." value={keywordInput} onChange={(event) => setKeywordInput(event.target.value)} /></div>
        <div className="col-md-3"><label className="form-label" htmlFor="category-status">Trạng thái</label><select id="category-status" className="form-select" value={status} onChange={(event) => { setLoading(true); setStatus(event.target.value); setPage(0) }}><option value="">Tất cả</option><option value="ACTIVE">Hoạt động</option><option value="INACTIVE">Ngừng hoạt động</option></select></div>
        <div className="col-md-3 d-flex gap-2"><button className="btn btn-dark flex-grow-1" type="submit">Tìm kiếm</button><button className="btn btn-outline-secondary" type="button" onClick={resetFilters}>Đặt lại</button></div>
      </form>

      <div className="categories-table-wrap">
        {loading ? <div className="p-5 text-center" role="status">Đang tải danh mục…</div> : categories.length === 0 ? <div className="p-5 text-center text-muted">Không tìm thấy danh mục phù hợp.</div> : <div className="table-responsive"><table className="table categories-table">
          <thead><tr><th>Mã</th><th>Tên danh mục</th><th>Danh mục cha</th><th className="category-description">Mô tả</th><th>Trạng thái</th><th className="text-end">Thao tác</th></tr></thead>
          <tbody>{categories.map((category) => <tr key={category.id}>
            <td><span className="category-code">{category.code}</span></td>
            <td><strong>{category.name}</strong></td>
            <td>{category.parentName || '—'}</td>
            <td className="category-description text-muted">{category.description || '—'}</td>
            <td><span className={`category-status category-status--${category.isDeleted ? 'inactive' : 'active'}`}>{category.isDeleted ? 'Ngừng hoạt động' : 'Hoạt động'}</span></td>
            <td className="text-end"><div className="btn-group btn-group-sm"><button className="btn btn-outline-secondary" type="button" onClick={() => openDetails(category)}>Chi tiết</button><button className="btn btn-outline-primary" type="button" disabled={category.isDeleted} onClick={() => openEdit(category)}>Sửa</button><button className="btn btn-outline-danger" type="button" disabled={category.isDeleted} onClick={() => openDeactivate(category)}>Ngừng</button></div></td>
          </tr>)}</tbody>
        </table></div>}
        {!loading && totalElements > 0 && <div className="d-flex justify-content-between align-items-center p-3 border-top"><span className="small text-muted">Tổng số: {totalElements} danh mục</span><div className="btn-group btn-group-sm"><button className="btn btn-outline-secondary" disabled={page === 0} onClick={() => { setLoading(true); setPage((value) => value - 1) }}>Trước</button><span className="btn btn-outline-secondary disabled">Trang {page + 1} / {Math.max(totalPages, 1)}</span><button className="btn btn-outline-secondary" disabled={page + 1 >= totalPages} onClick={() => { setLoading(true); setPage((value) => value + 1) }}>Sau</button></div></div>}
      </div>
    </div>

    {modal && <div className="category-modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && closeModal()}><div className="category-modal" role="dialog" aria-modal="true" aria-labelledby="category-modal-title">
      <div className="category-modal__header"><h2 id="category-modal-title">{modal === 'create' ? 'Thêm danh mục' : modal === 'edit' ? 'Cập nhật danh mục' : modal === 'details' ? 'Chi tiết danh mục' : 'Xác nhận ngừng hoạt động'}</h2><button className="btn-close" type="button" aria-label="Đóng" onClick={closeModal} /></div>
      {formErrors.general && <div className="alert alert-danger m-3 mb-0" role="alert">{formErrors.general}</div>}
      {(modal === 'create' || modal === 'edit') && <form onSubmit={submitForm}>
        <div className="category-modal__body">
          <div className="row g-3">
            <div className="col-md-5"><label className="form-label" htmlFor="category-code">Mã danh mục *</label><input id="category-code" className={`form-control ${formErrors.code ? 'is-invalid' : ''}`} maxLength="50" value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value })} />{formErrors.code && <div className="invalid-feedback">{formErrors.code}</div>}</div>
            <div className="col-md-7"><label className="form-label" htmlFor="category-name">Tên danh mục *</label><input id="category-name" className={`form-control ${formErrors.name ? 'is-invalid' : ''}`} maxLength="255" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />{formErrors.name && <div className="invalid-feedback">{formErrors.name}</div>}</div>
            <div className="col-12"><label className="form-label" htmlFor="category-parent">Danh mục cha</label><select id="category-parent" className="form-select" value={form.parentId} onChange={(event) => setForm({ ...form, parentId: event.target.value })}><option value="">Không có danh mục cha</option>{options.map((option) => <option key={option.id} value={option.id}>{option.code} — {option.name}</option>)}</select></div>
            <div className="col-12"><label className="form-label" htmlFor="category-description">Mô tả</label><textarea id="category-description" className="form-control" rows="3" maxLength="500" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></div>
          </div>
        </div><div className="category-modal__footer"><button className="btn btn-outline-secondary" type="button" onClick={closeModal}>Hủy</button><button className="btn btn-primary" type="submit" disabled={submitting}>{submitting ? 'Đang lưu…' : 'Lưu danh mục'}</button></div>
      </form>}
      {modal === 'details' && selected && <><div className="category-modal__body"><dl className="category-details"><div><dt>Mã danh mục</dt><dd>{selected.code}</dd></div><div><dt>Trạng thái</dt><dd>{selected.isDeleted ? 'Ngừng hoạt động' : 'Hoạt động'}</dd></div><div><dt>Tên danh mục</dt><dd>{selected.name}</dd></div><div><dt>Danh mục cha</dt><dd>{selected.parentName || 'Không có'}</dd></div><div><dt>Người tạo</dt><dd>{selected.createdBy || '—'}</dd></div><div><dt>Ngày tạo</dt><dd>{selected.createdAt ? new Date(selected.createdAt).toLocaleString('vi-VN') : '—'}</dd></div></dl>{selected.description && <p className="mt-3 mb-0">{selected.description}</p>}</div><div className="category-modal__footer"><button className="btn btn-primary" type="button" onClick={closeModal}>Đóng</button></div></>}
      {modal === 'deactivate' && selected && <><div className="category-modal__body"><p>Bạn có chắc muốn ngừng danh mục <strong>{selected.name}</strong>?</p><p className="alert alert-warning mb-0">Danh mục sẽ không còn xuất hiện trong bộ lọc hoặc form tạo sản phẩm. Các sản phẩm và dữ liệu lịch sử vẫn được giữ nguyên.</p></div><div className="category-modal__footer"><button className="btn btn-outline-secondary" type="button" onClick={closeModal}>Hủy</button><button className="btn btn-danger" type="button" disabled={submitting} onClick={confirmDeactivate}>{submitting ? 'Đang xử lý…' : 'Xác nhận ngừng'}</button></div></>}
    </div></div>}
  </main>
}
