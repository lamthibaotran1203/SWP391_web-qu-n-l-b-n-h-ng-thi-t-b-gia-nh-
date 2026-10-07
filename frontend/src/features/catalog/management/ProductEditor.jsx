import { useState } from 'react'
import { ProductFields } from './ProductFields'
import { SkuFields } from './SkuFields'
import { productForm, productPayload, skuForm, skuPayload } from './formValues'

export function ProductEditor({ product, categories, brands, onSave, onCancel, onBusy }) {
  const [fields, setFields] = useState(() => productForm(product))
  const [skus, setSkus] = useState(() => [skuForm()])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const deactivating = product?.status === 'ACTIVE' && fields.status === 'INACTIVE'
  async function submit(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true); onBusy?.(true); setError('')
    try {
      if (deactivating && !confirmed) throw new Error('Xác nhận ngừng bán trước khi lưu.')
      await onSave(productPayload(fields), product ? [] : skus.map(skuPayload))
    } catch (failure) { setError(failure.message) }
    finally { setBusy(false); onBusy?.(false) }
  }
  return <form onSubmit={submit} aria-busy={busy}>
    {error && <div role="alert" className="alert alert-danger">{error}</div>}
    <fieldset disabled={busy}>
      <ProductFields value={fields} onChange={setFields} categories={categories} brands={brands} />
      {!product && <section className="my-4">
        <h3 className="h5">Phân loại và giá</h3>
        {skus.map((sku, index) => <div className="border-bottom py-3" key={index}>
          <h4 className="h6">Phân loại {index + 1}</h4>
          <SkuFields prefix={`initial-${index}`} value={sku} onChange={(value) => setSkus((current) => current.map((item, position) => position === index ? value : item))} />
          {skus.length > 1 && <button type="button" className="btn btn-outline-secondary mt-2" onClick={() => setSkus((current) => current.filter((_, position) => position !== index))}>Xóa phân loại {index + 1}</button>}
        </div>)}
        <button type="button" className="btn btn-outline-secondary mt-3" disabled={skus.length >= 50} onClick={() => setSkus((current) => [...current, skuForm()])}>Thêm phân loại</button>
      </section>}
      {deactivating && <div className="form-check mt-3">
        <input id="product-deactivate-confirm" className="form-check-input" type="checkbox" checked={confirmed}
          onChange={(event) => setConfirmed(event.target.checked)} />
        <label className="form-check-label" htmlFor="product-deactivate-confirm">Tôi đồng ý ngừng bán sản phẩm này.</label>
      </div>}
      <div className="d-flex gap-2 my-4"><button className="btn btn-dark" type="submit">{busy ? 'Đang lưu…' : 'Lưu sản phẩm'}</button>
        <button className="btn btn-outline-secondary" type="button" onClick={onCancel}>Hủy</button></div>
    </fieldset>
  </form>
}
