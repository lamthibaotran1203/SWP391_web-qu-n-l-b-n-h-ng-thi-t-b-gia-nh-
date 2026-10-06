import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  getActiveSuppliers,
  getAvailableSkus,
  createPurchaseOrder,
  formatVnd,
} from '../features/purchasing/api'
import '../features/purchasing/purchasing.css'

export function CreatePurchaseOrderPage() {
  // Data sources
  const [suppliers, setSuppliers] = useState([])
  const [availableSkus, setAvailableSkus] = useState([])
  const [loadingInitial, setLoadingInitial] = useState(true)
  const [initialError, setInitialError] = useState(null)

  // Form state
  const [supplierId, setSupplierId] = useState('')
  const [expectedDate, setExpectedDate] = useState('')
  const [note, setNote] = useState('')
  const [items, setItems] = useState([
    { skuId: '', orderedQuantity: 1, unitCost: 0 },
  ])

  // UI state
  const [submitting, setSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState(null)
  const [successPo, setSuccessPo] = useState(null)

  useEffect(() => {
    let active = true

    Promise.all([getActiveSuppliers(), getAvailableSkus()])
      .then(([suppliersRes, skusRes]) => {
        if (active) {
          const suppList = suppliersRes?.data || suppliersRes || []
          setSuppliers(suppList)
          if (suppList.length > 0) {
            setSupplierId(String(suppList[0].id))
          }

          const skuData = skusRes?.data || skusRes || {}
          const skusList = skuData.content || skuData.items || []
          setAvailableSkus(skusList)

          if (skusList.length > 0) {
            setItems([
              {
                skuId: String(skusList[0].skuId || skusList[0].id),
                orderedQuantity: 1,
                unitCost: Math.round(Number(skusList[0].sellingPrice || 0) * 0.7),
              },
            ])
          }
        }
      })
      .catch((err) => {
        if (active) {
          setInitialError(err.message || 'Không thể tải dữ liệu khởi tạo nhà cung cấp và danh mục SKU.')
        }
      })
      .finally(() => {
        if (active) setLoadingInitial(false)
      })

    return () => {
      active = false
    }
  }, [])

  const selectedSupplier = suppliers.find((s) => String(s.id) === String(supplierId))

  function handleAddItem() {
    const defaultSku = availableSkus[0]
    setItems((prev) => [
      ...prev,
      {
        skuId: defaultSku ? String(defaultSku.skuId || defaultSku.id) : '',
        orderedQuantity: 1,
        unitCost: defaultSku ? Math.round(Number(defaultSku.sellingPrice || 0) * 0.7) : 0,
      },
    ])
  }

  function handleRemoveItem(index) {
    if (items.length <= 1) {
      alert('Đơn đặt hàng phải có ít nhất 1 mặt hàng (PO Item).')
      return
    }
    setItems((prev) => prev.filter((_, idx) => idx !== index))
  }

  function handleItemChange(index, field, value) {
    setItems((prev) => {
      const updated = [...prev]
      const current = { ...updated[index], [field]: value }

      if (field === 'skuId') {
        const foundSku = availableSkus.find(
          (s) => String(s.skuId || s.id) === String(value),
        )
        if (foundSku) {
          current.unitCost = Math.round(Number(foundSku.sellingPrice || 0) * 0.7)
        }
      }

      updated[index] = current
      return updated
    })
  }

  // Calculate totals
  const totalCost = items.reduce((sum, item) => {
    const qty = Number(item.orderedQuantity) || 0
    const cost = Number(item.unitCost) || 0
    return sum + qty * cost
  }, 0)

  const totalQuantity = items.reduce((sum, item) => sum + (Number(item.orderedQuantity) || 0), 0)

  async function handleSubmit(isDraft) {
    setErrorMessage(null)

    if (!supplierId) {
      setErrorMessage('Vui lòng chọn một nhà cung cấp hợp lệ.')
      return
    }

    if (items.length === 0) {
      setErrorMessage('Đơn đặt hàng phải có ít nhất 1 dòng sản phẩm.')
      return
    }

    // Check duplicate SKUs
    const seen = new Set()
    for (const item of items) {
      if (!item.skuId) {
        setErrorMessage('Vui lòng chọn mặt hàng SKU cho tất cả các dòng.')
        return
      }
      if (seen.has(item.skuId)) {
        setErrorMessage('Đơn đặt hàng không được có các dòng sản phẩm (SKU) trùng lặp.')
        return
      }
      seen.add(item.skuId)

      if (Number(item.orderedQuantity) <= 0) {
        setErrorMessage('Số lượng đặt hàng cho từng sản phẩm phải lớn hơn 0.')
        return
      }
      if (Number(item.unitCost) < 0) {
        setErrorMessage('Đơn giá nhập không được âm.')
        return
      }
    }

    setSubmitting(true)

    const payload = {
      supplierId: Number(supplierId),
      expectedDate: expectedDate || null,
      note: note.trim() || null,
      isDraft,
      items: items.map((i) => ({
        skuId: Number(i.skuId),
        orderedQuantity: Number(i.orderedQuantity),
        unitCost: Number(i.unitCost),
      })),
    }

    try {
      const response = await createPurchaseOrder(payload)
      const poData = response?.data || response
      setSuccessPo(poData)
    } catch (err) {
      setErrorMessage(err.message || 'Không thể tạo đơn đặt hàng PO.')
    } finally {
      setSubmitting(false)
    }
  }

  if (loadingInitial) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Đang tải...</span>
        </div>
        <p className="mt-2 text-muted">Đang tải thông tin nhà cung cấp và danh mục SKU...</p>
      </div>
    )
  }

  if (initialError) {
    return (
      <div className="container py-5">
        <div className="alert alert-danger" role="alert">
          <h4 className="alert-heading">Không thể khởi tạo đơn hàng</h4>
          <p>{initialError}</p>
          <hr />
          <Link to="/manager/suppliers" className="btn btn-outline-danger btn-sm">
            Quay lại quản lý nhà cung cấp
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="container purchasing-page">
      {/* Header */}
      <div className="purchasing-header">
        <div className="purchasing-title">
          <div className="mb-2">
            <Link to="/manager/suppliers" className="text-decoration-none small text-muted">
              &larr; Quay lại danh sách nhà cung cấp
            </Link>
          </div>
          <h1>Lập đơn mua hàng (Purchase Order)</h1>
          <p>Tạo đơn đặt hàng PO gửi tới nhà cung cấp để chuẩn bị nhập kho (UC-28)</p>
        </div>
      </div>

      {/* Success Modal / State */}
      {successPo && (
        <div className="alert alert-success p-4 mb-4 rounded-3 shadow-sm" role="alert">
          <h4 className="alert-heading fw-bold">Tạo đơn đặt hàng thành công!</h4>
          <p>
            Mã đơn mua hàng: <strong className="fs-5 text-primary">{successPo.poCode}</strong>
          </p>
          <p className="mb-2 text-muted">
            Trạng thái ban đầu: <strong>{successPo.status === 'DRAFT' ? 'Lưu nháp (DRAFT)' : 'Đã đặt hàng (ORDERED)'}</strong>
            {' · '}
            Tổng giá trị đơn: <strong>{formatVnd(successPo.totalCost)}</strong>
          </p>
          <p className="small text-secondary mb-4">
            <em>Lưu ý:</em> Theo quy tắc bất biến (POST-5), tồn kho vật lý <strong>chưa tăng</strong> tại bước tạo đơn này.
            Số lượng sẽ chỉ được nhập kho khi thực hiện tiếp nhận hàng hóa.
          </p>
          <div className="d-flex gap-2">
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => {
                setSuccessPo(null)
                setItems([
                  {
                    skuId: availableSkus[0] ? String(availableSkus[0].skuId || availableSkus[0].id) : '',
                    orderedQuantity: 1,
                    unitCost: availableSkus[0] ? Math.round(Number(availableSkus[0].sellingPrice || 0) * 0.7) : 0,
                  },
                ])
                setNote('')
                setExpectedDate('')
              }}
            >
              Tạo thêm đơn PO khác
            </button>
            <Link to="/manager/suppliers" className="btn btn-outline-secondary">
              Về trang Quản lý nhà cung cấp
            </Link>
          </div>
        </div>
      )}

      {errorMessage && (
        <div className="alert alert-danger alert-dismissible fade show" role="alert">
          {errorMessage}
          <button
            type="button"
            className="btn-close"
            onClick={() => setErrorMessage(null)}
            aria-label="Close"
          />
        </div>
      )}

      {!successPo && (
        <div className="row g-4">
          {/* Main Form (Left 8 cols) */}
          <div className="col-lg-8">
            {/* Supplier Information Card */}
            <div className="card shadow-sm border-0 mb-4 rounded-3">
              <div className="card-header bg-light py-3">
                <h5 className="card-title m-0 fw-bold text-dark">1. Thông tin nhà cung cấp & Đơn hàng</h5>
              </div>
              <div className="card-body p-4">
                <div className="row g-3">
                  <div className="col-md-6">
                    <label htmlFor="po-supplier" className="form-label fw-semibold">
                      Chọn nhà cung cấp <span className="text-danger">*</span>
                    </label>
                    <select
                      id="po-supplier"
                      className="form-select"
                      value={supplierId}
                      onChange={(e) => setSupplierId(e.target.value)}
                      required
                    >
                      {suppliers.length === 0 && <option value="">-- Chưa có nhà cung cấp nào --</option>}
                      {suppliers.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.name} ({s.code})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="col-md-6">
                    <label htmlFor="po-expected-date" className="form-label fw-semibold">
                      Ngày dự kiến nhận hàng
                    </label>
                    <input
                      type="date"
                      id="po-expected-date"
                      className="form-control"
                      value={expectedDate}
                      onChange={(e) => setExpectedDate(e.target.value)}
                    />
                  </div>

                  {selectedSupplier && (
                    <div className="col-12 mt-2">
                      <div className="p-3 bg-light rounded-3 small">
                        <div className="row">
                          <div className="col-sm-6">
                            <strong>Mã số thuế:</strong> {selectedSupplier.taxCode}
                          </div>
                          <div className="col-sm-6">
                            <strong>Người liên hệ:</strong> {selectedSupplier.contactName || '—'} ({selectedSupplier.phone || '—'})
                          </div>
                          <div className="col-12 mt-1">
                            <strong>Địa chỉ kho/văn phòng:</strong> {selectedSupplier.address || '—'}
                          </div>
                        </div>
                      </div>
                    </div>
                  )}

                  <div className="col-12">
                    <label htmlFor="po-note" className="form-label fw-semibold">
                      Ghi chú đơn đặt hàng
                    </label>
                    <textarea
                      id="po-note"
                      rows="2"
                      className="form-control"
                      placeholder="Ghi chú về điều kiện giao hàng, hóa đơn, thời gian chuyển..."
                      value={note}
                      onChange={(e) => setNote(e.target.value)}
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* PO Line Items Card */}
            <div className="card shadow-sm border-0 mb-4 rounded-3">
              <div className="card-header bg-light py-3 d-flex justify-content-between align-items-center">
                <h5 className="card-title m-0 fw-bold text-dark">2. Danh sách mặt hàng đặt (PO Items)</h5>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-primary"
                  onClick={handleAddItem}
                >
                  + Thêm dòng sản phẩm
                </button>
              </div>
              <div className="card-body p-0">
                <div className="table-responsive">
                  <table className="table align-middle mb-0">
                    <thead className="table-light">
                      <tr>
                        <th style={{ width: '40%' }}>Mặt hàng (SKU)</th>
                        <th style={{ width: '20%' }}>Số lượng đặt</th>
                        <th style={{ width: '25%' }}>Đơn giá nhập (VNĐ)</th>
                        <th style={{ width: '15%' }} className="text-end">Thành tiền</th>
                        <th style={{ width: '50px' }}></th>
                      </tr>
                    </thead>
                    <tbody>
                      {items.map((item, idx) => {
                        const lineSubtotal = (Number(item.orderedQuantity) || 0) * (Number(item.unitCost) || 0)
                        return (
                          <tr key={idx}>
                            <td>
                              <select
                                className="form-select form-select-sm"
                                value={item.skuId}
                                onChange={(e) => handleItemChange(idx, 'skuId', e.target.value)}
                                required
                              >
                                {availableSkus.map((sku) => (
                                  <option key={sku.skuId || sku.id} value={sku.skuId || sku.id}>
                                    {sku.skuCode} - {sku.skuName || sku.productName}
                                  </option>
                                ))}
                              </select>
                            </td>
                            <td>
                              <input
                                type="number"
                                min="1"
                                className="form-control form-control-sm"
                                value={item.orderedQuantity}
                                onChange={(e) => handleItemChange(idx, 'orderedQuantity', e.target.value)}
                                required
                              />
                            </td>
                            <td>
                              <input
                                type="number"
                                min="0"
                                step="1000"
                                className="form-control form-control-sm"
                                value={item.unitCost}
                                onChange={(e) => handleItemChange(idx, 'unitCost', e.target.value)}
                                required
                              />
                            </td>
                            <td className="text-end fw-semibold">
                              {formatVnd(lineSubtotal)}
                            </td>
                            <td className="text-center">
                              {items.length > 1 && (
                                <button
                                  type="button"
                                  className="btn btn-sm btn-link text-danger p-0"
                                  title="Xóa dòng"
                                  onClick={() => handleRemoveItem(idx)}
                                >
                                  ✕
                                </button>
                              )}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>

          {/* Sidebar Summary Card (Right 4 cols) */}
          <div className="col-lg-4">
            <div className="card shadow-sm border-0 rounded-3 sticky-top" style={{ top: '2rem' }}>
              <div className="card-header bg-dark text-white py-3">
                <h5 className="card-title m-0 fw-bold">Tổng kết đơn mua hàng</h5>
              </div>
              <div className="card-body p-4">
                <div className="d-flex justify-content-between mb-2">
                  <span className="text-muted">Số loại sản phẩm:</span>
                  <strong>{items.length} SKU</strong>
                </div>
                <div className="d-flex justify-content-between mb-2">
                  <span className="text-muted">Tổng số lượng đặt:</span>
                  <strong>{totalQuantity} chiếc/bộ</strong>
                </div>
                <hr />
                <div className="d-flex justify-content-between mb-4">
                  <span className="fw-bold fs-5 text-dark">Tổng chi phí:</span>
                  <span className="fw-bold fs-5 text-primary">{formatVnd(totalCost)}</span>
                </div>

                <div className="d-grid gap-2">
                  <button
                    type="button"
                    className="btn btn-primary btn-lg fw-semibold"
                    disabled={submitting}
                    onClick={() => handleSubmit(false)}
                  >
                    {submitting ? 'Đang tạo đơn…' : 'Xác nhận tạo đơn PO'}
                  </button>

                  <button
                    type="button"
                    className="btn btn-outline-secondary"
                    disabled={submitting}
                    onClick={() => handleSubmit(true)}
                  >
                    Lưu nháp (Draft)
                  </button>

                  <Link to="/manager/suppliers" className="btn btn-link text-muted text-decoration-none text-center">
                    Hủy bỏ
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
