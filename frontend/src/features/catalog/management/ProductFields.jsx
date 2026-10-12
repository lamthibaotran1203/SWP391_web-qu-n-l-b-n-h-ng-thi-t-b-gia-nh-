export function ProductFields({ value, onChange, categories, brands }) {
  function update(event) { onChange({ ...value, [event.target.name]: event.target.value }) }
  return <fieldset className="row g-3">
    <legend className="h5">Thông tin sản phẩm</legend>
    <div className="col-12"><label htmlFor="product-name" className="form-label">Tên sản phẩm</label>
      <input id="product-name" name="name" className="form-control" value={value.name} onChange={update} required minLength={2} maxLength={200} /></div>
    <div className="col-md-6"><label htmlFor="product-category" className="form-label">Danh mục</label>
      <select id="product-category" name="categoryId" className="form-select" value={value.categoryId || ''} onChange={update}>
        <option value="">Chưa chọn</option>
        {value.categoryId && !categories.some((item) => String(item.id) === String(value.categoryId)) && <option value={value.categoryId}>Danh mục cũ đã ngừng dùng</option>}
        {categories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
      </select></div>
    <div className="col-md-6"><label htmlFor="product-brand" className="form-label">Thương hiệu</label>
      <select id="product-brand" name="brandId" className="form-select" value={value.brandId || ''} onChange={update}>
        <option value="">Chưa chọn</option>
        {value.brandId && !brands.some((item) => String(item.id) === String(value.brandId)) && <option value={value.brandId}>Thương hiệu cũ đã ngừng dùng</option>}
        {brands.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
      </select></div>
    <div className="col-12"><label htmlFor="product-image" className="form-label">Ảnh đại diện sản phẩm</label>
      <input id="product-image" name="imageUrl" className="form-control" value={value.imageUrl} onChange={update} maxLength={1000} placeholder="Dán liên kết ảnh https://…" /></div>
    <div className="col-12"><label htmlFor="product-description" className="form-label">Mô tả</label>
      <textarea id="product-description" name="description" className="form-control" value={value.description} onChange={update} maxLength={10000} rows={3} /></div>
    <div className="col-md-6"><label htmlFor="product-status" className="form-label">Trạng thái sản phẩm</label>
      <select id="product-status" name="status" className="form-select" value={value.status} onChange={update}>
        <option value="INACTIVE">Ngừng bán</option><option value="ACTIVE">Đang bán</option>
      </select></div>
    <p className="form-text mb-0">Chọn danh mục, thương hiệu và thêm ảnh trước khi chuyển sang Đang bán.</p>
  </fieldset>
}
