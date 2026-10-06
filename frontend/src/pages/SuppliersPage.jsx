import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getSuppliers,
  createSupplier,
  updateSupplier,
  deleteSupplier,
} from '../features/purchasing/api'
import '../features/purchasing/purchasing.css'

export function SuppliersPage() {
  const [suppliers, setSuppliers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [successMessage, setSuccessMessage] = useState(null)

  // Filters & Pagination
  const [searchTerm, setSearchTerm] = useState('')
  const [keyword, setKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [refreshCounter, setRefreshCounter] = useState(0)

  // Modals state
  const [showModal, setShowModal] = useState(false)
  const [modalMode, setModalMode] = useState('create') // 'create' | 'edit' | 'view'
  const [selectedSupplier, setSelectedSupplier] = useState(null)

  // Form state
  const [formData, setFormData] = useState({
    code: '',
    taxCode: '',
    name: '',
    contactName: '',
    phone: '',
    email: '',
    address: '',
    status: 'ACTIVE',
  })
  const [formErrors, setFormErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let active = true

    getSuppliers({
      keyword,
      status: statusFilter,
      page: currentPage,
      size: 10,
    })
      .then((response) => {
        if (active) {
          const pageData = response?.data || response || {}
          setSuppliers(pageData.content || [])
          setTotalPages(pageData.totalPages || 0)
          setTotalElements(pageData.totalElements || 0)
          setError(null)
        }
      })
      .catch((err) => {
        if (active) {
          setError(err.message || 'Không thể tải danh sách nhà cung cấp.')
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [currentPage, statusFilter, keyword, refreshCounter])

  function handleSearch(e) {
    e.preventDefault()
    setLoading(true)
    setCurrentPage(0)
    setKeyword(searchTerm)
  }

  function handleReset() {
    setSearchTerm('')
    setKeyword('')
    setStatusFilter('')
    setCurrentPage(0)
    setLoading(true)
  }

  function openCreateModal() {
    setModalMode('create')
    setFormData({
      code: '',
      taxCode: '',
      name: '',
      contactName: '',
      phone: '',
      email: '',
      address: '',
      status: 'ACTIVE',
    })
    setFormErrors({})
    setShowModal(true)
  }

  function openEditModal(supplier) {
    setModalMode('edit')
    setSelectedSupplier(supplier)
    setFormData({
      code: supplier.code,
      taxCode: supplier.taxCode,
      name: supplier.name,
      contactName: supplier.contactName || '',
      phone: supplier.phone || '',
      email: supplier.email || '',
      address: supplier.address || '',
      status: supplier.status,
    })
    setFormErrors({})
    setShowModal(true)
  }

  function openViewModal(supplier) {
    setModalMode('view')
    setSelectedSupplier(supplier)
    setFormData({
      code: supplier.code,
      taxCode: supplier.taxCode,
      name: supplier.name,
      contactName: supplier.contactName || '',
      phone: supplier.phone || '',
      email: supplier.email || '',
      address: supplier.address || '',
      status: supplier.status,
    })
    setFormErrors({})
    setShowModal(true)
  }

  function closeModal() {
    setShowModal(false)
    setSelectedSupplier(null)
    setFormErrors({})
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setSubmitting(true)
    setFormErrors({})
    setError(null)
    setSuccessMessage(null)

    try {
      if (modalMode === 'create') {
        await createSupplier(formData)
        setSuccessMessage('Thêm nhà cung cấp mới thành công!')
      } else if (modalMode === 'edit') {
        await updateSupplier(selectedSupplier.id, {
          name: formData.name,
          contactName: formData.contactName,
          phone: formData.phone,
          email: formData.email,
          address: formData.address,
          status: formData.status,
        })
        setSuccessMessage('Cập nhật thông tin nhà cung cấp thành công!')
      }
      closeModal()
      setRefreshCounter((c) => c + 1)
    } catch (err) {
      if (err.fieldErrors) {
        setFormErrors(err.fieldErrors)
      } else {
        setFormErrors({ general: err.message || 'Đã có lỗi xảy ra.' })
      }
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(supplier) {
    if (
      !window.confirm(
        `Bạn có chắc chắn muốn vô hiệu hóa nhà cung cấp "${supplier.name}" (${supplier.code})?`,
      )
    ) {
      return
    }

    try {
      await deleteSupplier(supplier.id)
      setSuccessMessage(`Đã vô hiệu hóa nhà cung cấp "${supplier.name}" thành công!`)
      setRefreshCounter((c) => c + 1)
    } catch (err) {
      setError(err.message || 'Không thể vô hiệu hóa nhà cung cấp.')
    }
  }

  return (
    <div className="container purchasing-page">
      {/* Header */}
      <div className="purchasing-header">
        <div className="purchasing-title">
          <h1>Quản lý nhà cung cấp</h1>
          <p>Đăng ký, tra cứu và duy trì thông tin nhà cung cấp thiết bị gia dụng</p>
        </div>
        <div className="d-flex gap-2">
          <Link
            to="/manager/purchase-orders/create"
            className="btn btn-success d-flex align-items-center gap-2"
          >
            <span>📋</span> Lập đơn mua hàng (PO)
          </Link>
          <button
            type="button"
            className="btn btn-primary d-flex align-items-center gap-2"
            onClick={openCreateModal}
          >
            <span className="fs-5">+</span> Thêm nhà cung cấp
          </button>
        </div>
      </div>

      {/* Notifications */}
      {successMessage && (
        <div className="alert alert-success alert-dismissible fade show" role="alert">
          {successMessage}
          <button
            type="button"
            className="btn-close"
            onClick={() => setSuccessMessage(null)}
            aria-label="Close"
          />
        </div>
      )}
      {error && (
        <div className="alert alert-danger alert-dismissible fade show" role="alert">
          {error}
          <button
            type="button"
            className="btn-close"
            onClick={() => setError(null)}
            aria-label="Close"
          />
        </div>
      )}

      {/* Filter Card */}
      <div className="purchasing-filter-card">
        <form onSubmit={handleSearch} className="row g-3 align-items-end">
          <div className="col-md-6 col-lg-5">
            <label htmlFor="search-keyword" className="form-label fw-semibold text-muted small">
              Tìm kiếm theo từ khóa
            </label>
            <input
              type="text"
              id="search-keyword"
              className="form-control"
              placeholder="Nhập mã, tên NCC, MST hoặc SĐT..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          <div className="col-md-4 col-lg-3">
            <label htmlFor="filter-status" className="form-label fw-semibold text-muted small">
              Trạng thái
            </label>
            <select
              id="filter-status"
              className="form-select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
            >
              <option value="">-- Tất cả trạng thái --</option>
              <option value="ACTIVE">Đang hoạt động (ACTIVE)</option>
              <option value="INACTIVE">Tạm ngừng (INACTIVE)</option>
            </select>
          </div>
          <div className="col-md-2 col-lg-4 d-flex gap-2">
            <button type="submit" className="btn btn-dark px-4">
              Tìm kiếm
            </button>
            <button type="button" className="btn btn-outline-secondary" onClick={handleReset}>
              Làm mới
            </button>
          </div>
        </form>
      </div>

      {/* Table container */}
      <div className="purchasing-table-container">
        {loading ? (
          <div className="text-center py-5">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Đang tải...</span>
            </div>
            <p className="mt-2 text-muted">Đang tải danh sách nhà cung cấp...</p>
          </div>
        ) : suppliers.length === 0 ? (
          <div className="text-center py-5">
            <p className="text-muted mb-3">Không tìm thấy nhà cung cấp nào phù hợp.</p>
            <button type="button" className="btn btn-sm btn-outline-primary" onClick={openCreateModal}>
              Thêm nhà cung cấp mới
            </button>
          </div>
        ) : (
          <>
            <div className="table-responsive">
              <table className="table purchasing-table">
                <thead>
                  <tr>
                    <th>Mã NCC</th>
                    <th>Tên nhà cung cấp</th>
                    <th>Mã số thuế</th>
                    <th>Người liên hệ</th>
                    <th>Số điện thoại</th>
                    <th>Trạng thái</th>
                    <th className="text-end">Hành động</th>
                  </tr>
                </thead>
                <tbody>
                  {suppliers.map((s) => (
                    <tr key={s.id}>
                      <td className="fw-bold text-primary">{s.code}</td>
                      <td>
                        <strong>{s.name}</strong>
                        {s.address && (
                          <div className="text-muted small text-truncate" style={{ maxWidth: 220 }}>
                            {s.address}
                          </div>
                        )}
                      </td>
                      <td>{s.taxCode}</td>
                      <td>{s.contactName || '—'}</td>
                      <td>{s.phone || '—'}</td>
                      <td>
                        <span className={s.status === 'ACTIVE' ? 'badge-active' : 'badge-inactive'}>
                          {s.status === 'ACTIVE' ? 'Hoạt động' : 'Tạm ngừng'}
                        </span>
                      </td>
                      <td className="text-end">
                        <div className="btn-group btn-group-sm">
                          <button
                            type="button"
                            className="btn btn-outline-secondary"
                            title="Xem chi tiết"
                            onClick={() => openViewModal(s)}
                          >
                            Chi tiết
                          </button>
                          <button
                            type="button"
                            className="btn btn-outline-primary"
                            title="Chỉnh sửa"
                            onClick={() => openEditModal(s)}
                          >
                            Sửa
                          </button>
                          {s.status === 'ACTIVE' && (
                            <button
                              type="button"
                              className="btn btn-outline-danger"
                              title="Vô hiệu hóa"
                              onClick={() => handleDelete(s)}
                            >
                              Ngừng
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div className="d-flex justify-content-between align-items-center p-3 border-top">
              <span className="small text-muted">
                Tổng số: <strong>{totalElements}</strong> nhà cung cấp
              </span>
              <div className="btn-group btn-group-sm">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  disabled={currentPage <= 0}
                  onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                >
                  &laquo; Trang trước
                </button>
                <span className="btn btn-outline-secondary disabled text-dark">
                  Trang {currentPage + 1} / {totalPages || 1}
                </span>
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  disabled={currentPage >= totalPages - 1}
                  onClick={() => setCurrentPage((p) => p + 1)}
                >
                  Trang sau &raquo;
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Supplier Modal (Create / Edit / View) */}
      {showModal && (
        <div className="supplier-modal-backdrop" onClick={closeModal}>
          <div className="supplier-modal-dialog" onClick={(e) => e.stopPropagation()}>
            <div className="supplier-modal-header">
              <h2>
                {modalMode === 'create' && 'Đăng ký nhà cung cấp mới'}
                {modalMode === 'edit' && `Chỉnh sửa: ${selectedSupplier?.name}`}
                {modalMode === 'view' && `Chi tiết nhà cung cấp: ${selectedSupplier?.code}`}
              </h2>
              <button
                type="button"
                className="btn-close"
                onClick={closeModal}
                aria-label="Đóng"
              />
            </div>

            <form onSubmit={handleSubmit}>
              <div className="supplier-modal-body">
                {formErrors.general && (
                  <div className="alert alert-danger py-2 small">{formErrors.general}</div>
                )}

                <div className="row g-3">
                  {/* Mã NCC */}
                  <div className="col-md-6">
                    <label htmlFor="code" className="form-label small fw-semibold">
                      Mã nhà cung cấp <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      id="code"
                      className={`form-control ${formErrors.code ? 'is-invalid' : ''}`}
                      placeholder="VD: SUP-SUNHOUSE, NCC001"
                      disabled={modalMode !== 'create'}
                      value={formData.code}
                      onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                      required
                    />
                    {formErrors.code && (
                      <div className="invalid-feedback">{formErrors.code}</div>
                    )}
                  </div>

                  {/* Mã số thuế */}
                  <div className="col-md-6">
                    <label htmlFor="taxCode" className="form-label small fw-semibold">
                      Mã số thuế <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      id="taxCode"
                      className={`form-control ${formErrors.taxCode ? 'is-invalid' : ''}`}
                      placeholder="VD: 0101234567"
                      disabled={modalMode === 'view'}
                      value={formData.taxCode}
                      onChange={(e) => setFormData({ ...formData, taxCode: e.target.value })}
                      required
                    />
                    {formErrors.taxCode && (
                      <div className="invalid-feedback">{formErrors.taxCode}</div>
                    )}
                  </div>

                  {/* Tên nhà cung cấp */}
                  <div className="col-12">
                    <label htmlFor="name" className="form-label small fw-semibold">
                      Tên nhà cung cấp <span className="text-danger">*</span>
                    </label>
                    <input
                      type="text"
                      id="name"
                      className={`form-control ${formErrors.name ? 'is-invalid' : ''}`}
                      placeholder="VD: Công ty Cổ phần Tập đoàn Sunhouse"
                      disabled={modalMode === 'view'}
                      value={formData.name}
                      onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                      required
                    />
                    {formErrors.name && (
                      <div className="invalid-feedback">{formErrors.name}</div>
                    )}
                  </div>

                  {/* Người liên hệ */}
                  <div className="col-md-6">
                    <label htmlFor="contactName" className="form-label small fw-semibold">
                      Người liên hệ
                    </label>
                    <input
                      type="text"
                      id="contactName"
                      className="form-control"
                      placeholder="VD: Nguyễn Văn A"
                      disabled={modalMode === 'view'}
                      value={formData.contactName}
                      onChange={(e) => setFormData({ ...formData, contactName: e.target.value })}
                    />
                  </div>

                  {/* Số điện thoại */}
                  <div className="col-md-6">
                    <label htmlFor="phone" className="form-label small fw-semibold">
                      Số điện thoại
                    </label>
                    <input
                      type="text"
                      id="phone"
                      className="form-control"
                      placeholder="VD: 0901234567"
                      disabled={modalMode === 'view'}
                      value={formData.phone}
                      onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    />
                  </div>

                  {/* Email */}
                  <div className="col-md-6">
                    <label htmlFor="email" className="form-label small fw-semibold">
                      Email
                    </label>
                    <input
                      type="email"
                      id="email"
                      className="form-control"
                      placeholder="VD: contact@sunhouse.com.vn"
                      disabled={modalMode === 'view'}
                      value={formData.email}
                      onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    />
                  </div>

                  {/* Trạng thái */}
                  <div className="col-md-6">
                    <label htmlFor="status" className="form-label small fw-semibold">
                      Trạng thái hoạt động
                    </label>
                    <select
                      id="status"
                      className="form-select"
                      disabled={modalMode === 'view'}
                      value={formData.status}
                      onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                    >
                      <option value="ACTIVE">Đang hoạt động (ACTIVE)</option>
                      <option value="INACTIVE">Tạm ngừng (INACTIVE)</option>
                    </select>
                  </div>

                  {/* Địa chỉ */}
                  <div className="col-12">
                    <label htmlFor="address" className="form-label small fw-semibold">
                      Địa chỉ văn phòng / kho hàng
                    </label>
                    <textarea
                      id="address"
                      rows="2"
                      className="form-control"
                      placeholder="VD: Khu Công nghiệp Thạch Thất, Quốc Oai, Hà Nội"
                      disabled={modalMode === 'view'}
                      value={formData.address}
                      onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                    />
                  </div>
                </div>
              </div>

              <div className="supplier-modal-footer">
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={closeModal}
                >
                  {modalMode === 'view' ? 'Đóng' : 'Hủy bỏ'}
                </button>
                {modalMode !== 'view' && (
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={submitting}
                  >
                    {submitting ? 'Đang lưu…' : modalMode === 'create' ? 'Đăng ký NCC' : 'Cập nhật'}
                  </button>
                )}
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
