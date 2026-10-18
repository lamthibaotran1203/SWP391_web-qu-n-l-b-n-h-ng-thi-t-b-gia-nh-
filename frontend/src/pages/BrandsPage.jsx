import { useEffect, useState } from 'react'
import { Link, useLocation, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import {
  getBrands,
  getBrandById,
  createBrand,
  updateBrand,
  deactivateBrand,
  reactivateBrand,
} from '../features/brands/api'
import '../features/brands/brands.css'

export function BrandsPage() {
  const { user, connection, reload: reloadAuth } = useAuth()
  const location = useLocation()

  const [brands, setBrands] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)


  // Tu dong an thong bao thanh cong sau 5 giay
  useEffect(() => {
    if (!successMessage) return

    const timer = setTimeout(() => {
      setSuccessMessage(null)
    }, 5000)

    return () => clearTimeout(timer)
  }, [successMessage])

  // Filters & Pagination (zero-based for Spring Data)
  const [searchTerm, setSearchTerm] = useState('')
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [refreshCounter, setRefreshCounter] = useState(0)

  // Modals state
  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('create') // 'create' | 'edit' | 'view' | 'deactivate' | 'reactivate'
  const [selectedBrand, setSelectedBrand] = useState(null)

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    country: '',
    description: '',
    logoUrl: '',
  })
  const [formErrors, setFormErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  // Load brands on filters/page/refresh changes
  useEffect(() => {
    let active = true

    getBrands({
      keyword,
      status: statusFilter,
      page: currentPage,
      size: 10,
    })
      .then((response) => {
        if (active) {
          // Response is the Page<BrandResponse> object returned by apiRequest
          const pageData = response || {}
          setBrands(pageData.content || [])
          setTotalPages(pageData.totalPages || 0)
          setTotalElements(pageData.totalElements || 0)
          setError(null)
        }
      })
      .catch((err) => {
        if (active) {
          setError(err.message || 'Không thể tải danh sách thương hiệu.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [currentPage, statusFilter, keyword, refreshCounter])

  // Auth guard checks
  if (connection.status === 'loading') {
    return <p className="container py-4" role="status">Đang kiểm tra phiên đăng nhập…</p>
  }
  if (connection.status === 'error') {
    return (
      <div className="container py-4" role="alert">
        <p>{connection.message || 'Chưa kết nối được máy chủ.'}</p>
        <button className="btn btn-outline-dark" onClick={reloadAuth}>Kết nối lại</button>
      </div>
    )
  }
  if (!user.authenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />
  }
  if (user.role !== 'ADMIN') {
    return (
      <div className="container py-4">
        <h1>Không có quyền quản lý thương hiệu</h1>
        <p>Chỉ Quản trị viên (ADMIN) mới có quyền truy cập trang này.</p>
        <Link to="/" className="btn btn-primary">Về trang chủ</Link>
      </div>
    )
  }

  function handleSearch(e) {
    e.preventDefault()
    setLoading(true)
    setCurrentPage(0)
    setKeyword(searchTerm)
  }

  function handleReset() {
    setLoading(true)
    setSearchTerm('')
    setKeyword('')
    setStatusFilter('')
    setCurrentPage(0)
  }

  function openCreateModal() {
    setModalMode('create')
    setSelectedBrand(null)
    setFormData({
      name: '',
      country: '',
      description: '',
      logoUrl: '',
    })
    setFormErrors({})
    setShowModal(true)
  }

  function openEditModal(brand) {
    setModalMode('edit')
    setSelectedBrand(brand)
    setFormData({
      name: brand.name || '',
      country: brand.country || '',
      description: brand.description || '',
      logoUrl: brand.logoUrl || '',
    })
    setFormErrors({})
    setShowModal(true)
  }

  async function openViewModal(brand) {
    setModalMode('view')
    setSelectedBrand(brand)
    setShowModal(true)
    try {
      const detailed = await getBrandById(brand.id)
      setSelectedBrand(detailed)
    } catch {
      // If fetching detail fails, retain the row data
    }
  }

  function openDeactivateModal(brand) {
    setModalMode('deactivate')
    setSelectedBrand(brand)
    setFormErrors({})
    setShowModal(true)
  }

  function openReactivateModal(brand) {
    setModalMode('reactivate')
    setSelectedBrand(brand)
    setFormErrors({})
    setShowModal(true)
  }

  function closeModal() {
    setShowModal(false)
    setSelectedBrand(null)
    setFormErrors({})
  }

  function validateForm() {
    const errors = {}
    if (!formData.name || !formData.name.trim()) {
      errors.name = 'Tên thương hiệu không được để trống'
    } else if (formData.name.trim().length > 255) {
      errors.name = 'Tên thương hiệu không quá 255 ký tự'
    }
    if (formData.country && formData.country.trim().length > 100) {
      errors.country = 'Quốc gia không quá 100 ký tự'
    }
    if (formData.description && formData.description.trim().length > 500) {
      errors.description = 'Mô tả không quá 500 ký tự'
    }
    if (formData.logoUrl && formData.logoUrl.trim().length > 512) {
      errors.logoUrl = 'Đường dẫn logo không quá 512 ký tự'
    }
    return errors
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const clientErrors = validateForm()
    if (Object.keys(clientErrors).length > 0) {
      setFormErrors(clientErrors)
      return
    }

    setSubmitting(true)
    setFormErrors({})
    setError(null)
    setSuccessMessage(null)

    const payload = {
      name: formData.name.trim(),
      country: formData.country?.trim() || null,
      description: formData.description?.trim() || null,
      logoUrl: formData.logoUrl?.trim() || null,
    }

    try {
      if (modalMode === 'create') {
        await createBrand(payload)
        setSuccessMessage(`Đã thêm thương hiệu "${payload.name}" thành công!`)
      } else if (modalMode === 'edit') {
        await updateBrand(selectedBrand.id, payload)
        setSuccessMessage(`Đã cập nhật thương hiệu "${payload.name}" thành công!`)
      }
      closeModal()
      setRefreshCounter((c) => c + 1)
    } catch (err) {
      if (err.fieldErrors && Object.keys(err.fieldErrors).length > 0) {
        setFormErrors(err.fieldErrors)
      } else {
        setFormErrors({ general: err.message || 'Không thể lưu thương hiệu. Vui lòng thử lại.' })
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleConfirmDeactivate() {
    if (!selectedBrand) return

    setSubmitting(true)
    setFormErrors({})
    setError(null)
    setSuccessMessage(null)

    try {
      await deactivateBrand(selectedBrand.id)
      setSuccessMessage(`Đã vô hiệu hóa thương hiệu "${selectedBrand.name}" thành công!`)
      closeModal()
      setRefreshCounter((c) => c + 1)
    } catch (err) {
      if (err.code === 'BRAND_HAS_ACTIVE_PRODUCTS') {
        setFormErrors({
          general: err.message || 'Không thể vô hiệu hóa thương hiệu vì vẫn còn sản phẩm đang hoạt động thuộc thương hiệu này.',
        })
      } else {
        setFormErrors({ general: err.message || 'Không thể vô hiệu hóa thương hiệu.' })
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleConfirmReactivate() {
    if (!selectedBrand || submitting) return

    setSubmitting(true)
    setFormErrors({})
    setError(null)
    setSuccessMessage(null)

    try {
      await reactivateBrand(selectedBrand.id)
      setSuccessMessage(`Đã khôi phục thương hiệu "${selectedBrand.name}" thành công!`)
      closeModal()
      setRefreshCounter((c) => c + 1)
    } catch (err) {
      setFormErrors({ general: err.message || 'Không thể khôi phục thương hiệu. Vui lòng thử lại.' })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="workspace-page">
      <div className="container brands-page">
        {/* Header */}
        <div className="brands-header">
          <div className="brands-title">
            <h1>Quản lý thương hiệu</h1>
            <p>Thêm mới, tra cứu, chỉnh sửa và quản trị danh mục thương hiệu sản phẩm</p>
          </div>
          <div>
            <button
              type="button"
              className="btn btn-primary d-flex align-items-center gap-2"
              onClick={openCreateModal}
            >
              Thêm thương hiệu
            </button>
          </div>
        </div>

        {/* Alerts */}
        {successMessage && (
          <div className="alert alert-success alert-dismissible fade show" role="alert">
            {successMessage}
            <button
              type="button"
              className="btn-close"
              onClick={() => setSuccessMessage(null)}
              aria-label="Đóng"
            />
          </div>
        )}
        {error && (
          <div className="alert alert-danger alert-dismissible fade show" role="alert">
            <span>{error}</span>
            <button
              type="button"
              className="btn-close"
              onClick={() => setError(null)}
              aria-label="Đóng"
            />
          </div>
        )}

        {/* Filters */}
        <div className="brands-filter-card">
          <form onSubmit={handleSearch} className="row g-3 align-items-end">
            <div className="col-md-5">
              <label htmlFor="brand-search" className="form-label small fw-semibold text-secondary">
                Tìm kiếm thương hiệu
              </label>
              <input
                id="brand-search"
                type="text"
                className="form-control"
                placeholder="Nhập tên thương hiệu hoặc quốc gia..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <div className="col-md-3">
              <label htmlFor="brand-status-filter" className="form-label small fw-semibold text-secondary">
                Trạng thái
              </label>
              <select
                id="brand-status-filter"
                className="form-select"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value)
                  setCurrentPage(0)
                }}
              >
                <option value="">Tất cả trạng thái</option>
                <option value="ACTIVE">Đang hoạt động</option>
                <option value="INACTIVE">Ngừng hoạt động</option>
              </select>
            </div>
            <div className="col-md-4 d-flex gap-2">
              <button type="submit" className="btn btn-dark flex-grow-1">
                Tìm kiếm
              </button>
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={handleReset}
              >
                Đặt lại
              </button>
            </div>
          </form>
        </div>

        {/* Table / Results */}
        <div className="brands-table-container">
          {loading ? (
            <div className="p-5 text-center text-muted" role="status">
              <div className="spinner-border spinner-border-sm text-primary me-2" role="status" />
              Đang tải danh sách thương hiệu...
            </div>
          ) : brands.length === 0 ? (
            <div className="p-5 text-center text-muted">
              <p className="fs-5 mb-1">Không tìm thấy thương hiệu nào.</p>
              <p className="small text-secondary mb-3">
                {keyword || statusFilter
                  ? 'Thử điều chỉnh lại bộ lọc hoặc từ khóa tìm kiếm.'
                  : 'Chưa có thương hiệu nào trong hệ thống.'}
              </p>
              {(keyword || statusFilter) && (
                <button type="button" className="btn btn-outline-dark btn-sm" onClick={handleReset}>
                  Xóa bộ lọc
                </button>
              )}
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table brands-table align-middle">
                <thead>
                  <tr>
                    <th scope="col" style={{ width: '70px' }}>Logo</th>
                    <th scope="col">Tên thương hiệu</th>
                    <th scope="col">Quốc gia</th>
                    <th scope="col">Mô tả</th>
                    <th scope="col">Trạng thái</th>
                    <th scope="col">Ngày tạo</th>
                    <th scope="col" className="text-end" style={{ width: '190px' }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {brands.map((brand) => (
                    <tr key={brand.id}>
                      <td>
                        {brand.logoUrl ? (
                          <img
                            src={brand.logoUrl}
                            alt={brand.name}
                            className="brand-logo-thumb"
                            onError={(e) => {
                              e.currentTarget.style.display = 'none'
                              if (e.currentTarget.nextSibling) {
                                e.currentTarget.nextSibling.style.display = 'flex'
                              }
                            }}
                          />
                        ) : null}
                        <div
                          className="brand-logo-placeholder"
                          style={{ display: brand.logoUrl ? 'none' : 'flex' }}
                        >
                          {brand.name ? brand.name.charAt(0).toUpperCase() : 'B'}
                        </div>
                      </td>
                      <td>
                        <strong className="d-block text-dark">{brand.name}</strong>
                      </td>
                      <td>{brand.country || <span className="text-muted">—</span>}</td>
                      <td>
                        <span
                          className="d-inline-block text-truncate"
                          style={{ maxWidth: '280px' }}
                          title={brand.description}
                        >
                          {brand.description || <span className="text-muted">—</span>}
                        </span>
                      </td>
                      <td>
                        {brand.isDeleted ? (
                          <span className="brand-badge brand-badge-inactive">Ngừng hoạt động</span>
                        ) : (
                          <span className="brand-badge brand-badge-active">Hoạt động</span>
                        )}
                      </td>
                      <td className="small text-secondary">
                        {brand.createdAt ? new Date(brand.createdAt).toLocaleDateString('vi-VN') : '—'}
                      </td>
                      <td className="text-end">
                        <div className="btn-group btn-group-sm">
                          <button
                            type="button"
                            className="btn btn-outline-secondary"
                            onClick={() => openViewModal(brand)}
                            title="Xem chi tiết"
                          >
                            Xem
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-primary"
                            onClick={() => openEditModal(brand)}
                            disabled={brand.isDeleted}
                            title={brand.isDeleted ? 'Không thể sửa thương hiệu đã ngừng hoạt động' : 'Chỉnh sửa'}
                          >
                            Sửa
                          </button>
                          {brand.isDeleted ? (
                            <button
                              type="button"
                              className="btn btn-outline-success"
                              onClick={() => openReactivateModal(brand)}
                              title="Khôi phục thương hiệu"
                            >
                              Khôi phục
                            </button>
                          ) : (
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              onClick={() => openDeactivateModal(brand)}
                              title="Vô hiệu hóa"
                            >
                              Vô hiệu hóa
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination */}
          {!loading && totalPages > 1 && (
            <div className="d-flex justify-content-between align-items-center p-3 border-top">
              <span className="small text-muted">
                Hiển thị {brands.length} trên tổng số {totalElements} thương hiệu
              </span>
              <nav aria-label="Phân trang thương hiệu">
                <ul className="pagination pagination-sm mb-0">
                  <li className={`page-item ${currentPage === 0 ? 'disabled' : ''}`}>
                    <button
                      className="page-link"
                      onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                      disabled={currentPage === 0}
                    >
                      Trước
                    </button>
                  </li>
                  <li className="page-item disabled">
                    <span className="page-link">
                      Trang {currentPage + 1} / {totalPages}
                    </span>
                  </li>
                  <li className={`page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}`}>
                    <button
                      className="page-link"
                      onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                      disabled={currentPage >= totalPages - 1}
                    >
                      Sau
                    </button>
                  </li>
                </ul>
              </nav>
            </div>
          )}
        </div>
      </div>

      {/* Modal Dialog */}
      {showModal && (
        <div className="brand-modal-backdrop" onClick={closeModal}>
          <div className="brand-modal-dialog" onClick={(e) => e.stopPropagation()}>
            {/* Modal Header */}
            <div className="brand-modal-header">
              <h2>
                {modalMode === 'create' && 'Thêm thương hiệu mới'}
                {modalMode === 'edit' && 'Cập nhật thương hiệu'}
                {modalMode === 'view' && 'Chi tiết thương hiệu'}
                {modalMode === 'deactivate' && 'Xác nhận vô hiệu hóa'}
                {modalMode === 'reactivate' && 'Xác nhận khôi phục'}
              </h2>
              <button
                type="button"
                className="btn-close"
                onClick={closeModal}
                aria-label="Đóng"
              />
            </div>

            {/* Modal Body */}
            {modalMode === 'view' && selectedBrand && (
              <div className="brand-modal-body">
                <div className="text-center mb-4">
                  {selectedBrand.logoUrl ? (
                    <img
                      src={selectedBrand.logoUrl}
                      alt={selectedBrand.name}
                      style={{ maxWidth: '120px', maxHeight: '80px', objectFit: 'contain' }}
                    />
                  ) : (
                    <div
                      className="brand-logo-placeholder mx-auto"
                      style={{ width: '64px', height: '64px', fontSize: '1.5rem' }}
                    >
                      {selectedBrand.name ? selectedBrand.name.charAt(0).toUpperCase() : 'B'}
                    </div>
                  )}
                  <h3 className="mt-2 mb-1">{selectedBrand.name}</h3>
                  <div>
                    {selectedBrand.isDeleted ? (
                      <span className="brand-badge brand-badge-inactive">Ngừng hoạt động</span>
                    ) : (
                      <span className="brand-badge brand-badge-active">Đang hoạt động</span>
                    )}
                  </div>
                </div>

                <div className="list-group list-group-flush small">
                  <div className="list-group-item d-flex justify-content-between px-0">
                    <span className="text-muted">Mã ID:</span>
                    <strong>#{selectedBrand.id}</strong>
                  </div>
                  <div className="list-group-item d-flex justify-content-between px-0">
                    <span className="text-muted">Quốc gia / Xuất xứ:</span>
                    <span>{selectedBrand.country || '—'}</span>
                  </div>
                  <div className="list-group-item d-flex justify-content-between px-0">
                    <span className="text-muted">Mô tả:</span>
                    <span className="text-end" style={{ maxWidth: '65%' }}>
                      {selectedBrand.description || '—'}
                    </span>
                  </div>
                  <div className="list-group-item d-flex justify-content-between px-0">
                    <span className="text-muted">Người tạo:</span>
                    <span>{selectedBrand.createdBy || '—'}</span>
                  </div>
                  <div className="list-group-item d-flex justify-content-between px-0">
                    <span className="text-muted">Ngày tạo:</span>
                    <span>{selectedBrand.createdAt ? new Date(selectedBrand.createdAt).toLocaleString('vi-VN') : '—'}</span>
                  </div>
                  <div className="list-group-item d-flex justify-content-between px-0">
                    <span className="text-muted">Người cập nhật:</span>
                    <span>{selectedBrand.updatedBy || '—'}</span>
                  </div>
                  <div className="list-group-item d-flex justify-content-between px-0">
                    <span className="text-muted">Cập nhật gần nhất:</span>
                    <span>{selectedBrand.updatedAt ? new Date(selectedBrand.updatedAt).toLocaleString('vi-VN') : '—'}</span>
                  </div>
                </div>
              </div>
            )}

            {modalMode === 'deactivate' && selectedBrand && (
              <div className="brand-modal-body">
                {formErrors.general && (
                  <div className="alert alert-danger" role="alert">
                    {formErrors.general}
                  </div>
                )}
                <p className="mb-3">
                  Bạn có chắc chắn muốn vô hiệu hóa thương hiệu{' '}
                  <strong className="text-dark">"{selectedBrand.name}"</strong> không?
                </p>
                <div className="alert alert-warning small mb-0">
                  <p className="mb-1 fw-bold">Lưu ý quan trọng:</p>
                  <ul className="mb-0 ps-3">
                    <li>Đây là thao tác soft-delete (ngừng hoạt động). Thương hiệu không bị xóa vĩnh viễn khỏi CSDL.</li>
                    <li>Sẽ không thể chọn thương hiệu này khi tạo sản phẩm mới.</li>
                    <li>Nếu vẫn còn sản phẩm đang hoạt động thuộc thương hiệu này, hệ thống sẽ ngăn chặn thao tác.</li>
                  </ul>
                </div>
              </div>
            )}

            {modalMode === 'reactivate' && selectedBrand && (
              <div className="brand-modal-body">
                {formErrors.general && (
                  <div className="alert alert-danger" role="alert">
                    {formErrors.general}
                  </div>
                )}
                <p className="mb-3">
                  Bạn có chắc chắn muốn khôi phục thương hiệu{' '}
                  <strong className="text-dark">"{selectedBrand.name}"</strong> không?
                </p>
                <div className="alert alert-info small mb-0">
                  Thương hiệu sẽ chuyển sang trạng thái Hoạt động và có thể được sử dụng trở lại.
                </div>
              </div>
            )}

            {(modalMode === 'create' || modalMode === 'edit') && (
              <form onSubmit={handleSubmit} id="brand-form" noValidate>
                <div className="brand-modal-body">
                  {formErrors.general && (
                    <div className="alert alert-danger" role="alert">
                      {formErrors.general}
                    </div>
                  )}

                  <div className="mb-3">
                    <label htmlFor="brand-name-input" className="form-label fw-semibold">
                      Tên thương hiệu <span className="text-danger">*</span>
                    </label>
                    <input
                      id="brand-name-input"
                      type="text"
                      className={`form-control ${formErrors.name ? 'is-invalid' : ''}`}
                      placeholder="Ví dụ: Sunhouse, Lock&Lock, Philips..."
                      value={formData.name}
                      maxLength={255}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      required
                    />
                    {formErrors.name && (
                      <div className="invalid-feedback">{formErrors.name}</div>
                    )}
                  </div>

                  <div className="mb-3">
                    <label htmlFor="brand-country-input" className="form-label fw-semibold">
                      Quốc gia / Xuất xứ
                    </label>
                    <input
                      id="brand-country-input"
                      type="text"
                      className={`form-control ${formErrors.country ? 'is-invalid' : ''}`}
                      placeholder="Ví dụ: Việt Nam, Hàn Quốc, Nhật Bản..."
                      value={formData.country}
                      maxLength={100}
                      onChange={(e) => setFormData({ ...formData, country: e.target.value })}
                    />
                    {formErrors.country && (
                      <div className="invalid-feedback">{formErrors.country}</div>
                    )}
                  </div>

                  <div className="mb-3">
                    <label htmlFor="brand-description-input" className="form-label fw-semibold">
                      Mô tả thương hiệu
                    </label>
                    <textarea
                      id="brand-description-input"
                      className={`form-control ${formErrors.description ? 'is-invalid' : ''}`}
                      rows={3}
                      placeholder="Thông tin giới thiệu ngắn về thương hiệu..."
                      value={formData.description}
                      maxLength={500}
                      onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                    />
                    {formErrors.description && (
                      <div className="invalid-feedback">{formErrors.description}</div>
                    )}
                  </div>

                  <div className="mb-3">
                    <label htmlFor="brand-logo-input" className="form-label fw-semibold">
                      Đường dẫn ảnh Logo (URL)
                    </label>
                    <input
                      id="brand-logo-input"
                      type="url"
                      className={`form-control ${formErrors.logoUrl ? 'is-invalid' : ''}`}
                      placeholder="https://example.com/logo.png"
                      value={formData.logoUrl}
                      maxLength={512}
                      onChange={(e) => setFormData({ ...formData, logoUrl: e.target.value })}
                    />
                    {formErrors.logoUrl && (
                      <div className="invalid-feedback">{formErrors.logoUrl}</div>
                    )}
                    {formData.logoUrl && (
                      <div className="mt-2 p-2 border rounded bg-light d-flex align-items-center gap-2">
                        <small className="text-muted">Xem trước:</small>
                        <img
                          src={formData.logoUrl}
                          alt="Xem trước logo"
                          style={{ height: '32px', maxWidth: '80px', objectFit: 'contain' }}
                          onError={(e) => { e.currentTarget.style.display = 'none' }}
                        />
                      </div>
                    )}
                  </div>
                </div>
              </form>
            )}

            {/* Modal Footer */}
            <div className="brand-modal-footer">
              <button
                type="button"
                className="btn btn-outline-secondary"
                onClick={closeModal}
                disabled={submitting}
              >
                {modalMode === 'view' ? 'Đóng' : 'Hủy'}
              </button>

              {(modalMode === 'create' || modalMode === 'edit') && (
                <button
                  type="submit"
                  form="brand-form"
                  className="btn btn-primary"
                  disabled={submitting}
                >
                  {submitting ? 'Đang lưu...' : modalMode === 'create' ? 'Tạo mới' : 'Lưu thay đổi'}
                </button>
              )}

              {modalMode === 'deactivate' && (
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={handleConfirmDeactivate}
                  disabled={submitting}
                >
                  {submitting ? 'Đang xử lý...' : 'Xác nhận vô hiệu hóa'}
                </button>
              )}

              {modalMode === 'reactivate' && (
                <button
                  type="button"
                  className="btn btn-success"
                  onClick={handleConfirmReactivate}
                  disabled={submitting}
                >
                  {submitting ? 'Đang khôi phục...' : 'Xác nhận khôi phục'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </main>
  )
}
