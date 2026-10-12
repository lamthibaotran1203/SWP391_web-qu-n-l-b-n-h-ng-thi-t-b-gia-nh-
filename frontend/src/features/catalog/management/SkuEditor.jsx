import { useState } from 'react'
import { SkuFields } from './SkuFields'
import { skuForm, skuPayload } from './formValues'

export function SkuEditor({ sku, onSave, onCancel, onBusy, onReload }) {
  const [fields, setFields] = useState(() => skuForm(sku))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)
  const [confirmed, setConfirmed] = useState(false)
  const deactivating = sku?.status === 'ACTIVE' && fields.status === 'INACTIVE'
  async function submit(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true); onBusy?.(true); setError(null)
    try {
      if (deactivating && !confirmed) throw new Error('Xác nhận ngừng bán trước khi lưu.')
      await onSave(skuPayload(fields))
    } catch (failure) { setError(failure) }
    finally { setBusy(false); onBusy?.(false) }
  }
  return <form onSubmit={submit} aria-busy={busy}>
    {error && <div role="alert" className="alert alert-danger">{error.message}
      {error.status === 409 && onReload && <button type="button" className="btn btn-outline-dark ms-3" onClick={onReload}>Làm mới dữ liệu</button>}
    </div>}
    <fieldset disabled={busy}><SkuFields value={fields} onChange={setFields} />
      {deactivating && <div className="form-check mt-3">
        <input id="sku-deactivate-confirm" className="form-check-input" type="checkbox" checked={confirmed}
          onChange={(event) => setConfirmed(event.target.checked)} />
        <label className="form-check-label" htmlFor="sku-deactivate-confirm">Tôi đồng ý ngừng bán phân loại này.</label>
      </div>}
      <div className="form-actions"><button className="btn btn-primary" type="submit">{busy ? 'Đang lưu…' : sku ? 'Lưu thay đổi' : 'Thêm phân loại'}</button>
        <button className="btn btn-outline-secondary" type="button" onClick={onCancel}>Hủy</button></div>
    </fieldset>
  </form>
}
