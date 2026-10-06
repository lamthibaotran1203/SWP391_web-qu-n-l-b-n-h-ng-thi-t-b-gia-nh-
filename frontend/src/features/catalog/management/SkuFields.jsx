const fields = [
  ['skuCode', 'Mã hàng (SKU)', 'text', 100], ['barcode', 'Mã vạch nội bộ', 'text', 100],
  ['skuName', 'Tên phân loại', 'text', 255], ['originalPrice', 'Giá gốc (VND)', 'text', 18],
  ['sellingPrice', 'Giá bán (VND)', 'text', 18], ['minStockThreshold', 'Cảnh báo khi số lượng dưới', 'number'],
]
export function SkuFields({ value, onChange, prefix = 'sku' }) {
  function update(event) { onChange({ ...value, [event.target.name]: event.target.value }) }
  function updateAttribute(index, key, content) {
    onChange({ ...value, attributeRows: value.attributeRows.map((row, position) => position === index ? { ...row, [key]: content } : row) })
  }
  return <fieldset className="row g-3">
    <legend className="h5">Thông tin phân loại</legend>
    {fields.map(([name, label, type, maxLength]) => <div className="col-md-6" key={name}>
      <label htmlFor={`${prefix}-${name}`} className="form-label">{label}</label>
      <input id={`${prefix}-${name}`} name={name} className="form-control" value={value[name]} onChange={update}
        type={type} required maxLength={maxLength} min={type === 'number' ? 0 : undefined} step={type === 'number' ? 1 : undefined}
        inputMode={name.includes('Price') ? 'numeric' : undefined} pattern={name.includes('Price') ? '[0-9]+' : undefined} />
    </div>)}
    <div className="col-md-6"><label htmlFor={`${prefix}-image`} className="form-label">Ảnh phân loại (tùy chọn)</label>
      <input id={`${prefix}-image`} name="imageUrl" className="form-control" value={value.imageUrl} onChange={update} maxLength={512} /></div>
    <div className="col-md-6"><label htmlFor={`${prefix}-status`} className="form-label">Trạng thái phân loại</label>
      <select id={`${prefix}-status`} name="status" className="form-select" value={value.status} onChange={update}>
        <option value="INACTIVE">Ngừng bán</option><option value="ACTIVE">Đang bán</option>
      </select></div>
    <div className="col-12"><p>Thông số sản phẩm, ví dụ: Dung tích — 2 lít.</p>
      {value.attributeRows.map((row, index) => <div className="row g-2 mb-2" key={index}>
        <div className="col-sm-4"><label className="form-label" htmlFor={`${prefix}-key-${index}`}>Tên thông số {index + 1}</label>
          <input id={`${prefix}-key-${index}`} className="form-control" value={row.key} required maxLength={50}
            onChange={(event) => updateAttribute(index, 'key', event.target.value)} /></div>
        <div className="col-sm-6"><label className="form-label" htmlFor={`${prefix}-value-${index}`}>Giá trị thông số {index + 1}</label>
          <input id={`${prefix}-value-${index}`} className="form-control" value={row.value} required maxLength={255}
            onChange={(event) => updateAttribute(index, 'value', event.target.value)} /></div>
        <div className="col-sm-2 align-self-end"><button className="btn btn-outline-secondary" type="button"
          aria-label={`Xóa thông số ${index + 1}`} onClick={() => onChange({ ...value, attributeRows: value.attributeRows.filter((_, position) => position !== index) })}>Bỏ</button></div>
      </div>)}
      <button type="button" className="btn btn-outline-secondary" disabled={value.attributeRows.length >= 20}
        onClick={() => onChange({ ...value, attributeRows: [...value.attributeRows, { key: '', value: '' }] })}>Thêm thông số</button>
    </div>
  </fieldset>
}
