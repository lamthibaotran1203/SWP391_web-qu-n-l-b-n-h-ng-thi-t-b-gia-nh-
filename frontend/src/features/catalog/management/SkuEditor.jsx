import { useState } from 'react'
import { SkuFields } from './SkuFields'
import { skuForm, skuPayload } from './formValues'

export function SkuEditor({ sku, onSave, onCancel, onBusy }) {
  const [fields, setFields] = useState(() => skuForm(sku))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const deactivating = sku?.status === 'ACTIVE' && fields.status === 'INACTIVE'
  async function submit(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true); onBusy?.(true); setError('')
    try {
      if (deactivating && !confirmed) throw new Error('Xác nhận ngừng bán trước khi lưu.')
      await onSave(skuPayload(fields))
    } catch (failure) { setError(failure.message) }
    finally { setBusy(false); onBusy?.(false) }
  }
  return <form onSubmit={submit} aria-busy={busy}>
    {error && <div role="alert" className="alert alert-danger">{error}</div>}
    <fieldset disabled={busy}><SkuFields value={fields} onChange={setFields} />
      {deactivating && <div className="form-check mt-3">
        <input id="sku-deactivate-confirm" className="form-check-input" type="checkbox" checked={confirmed}
          onChange={(event) => setConfirmed(event.target.checked)} />
        <label className="form-check-label" htmlFor="sku-deactivate-confirm">Tôi đồng ý ngừng bán phân loại này.</label>
      </div>}
      <div className="d-flex gap-2 my-4"><button className="btn btn-dark" type="submit">{busy ? 'Đang lưu…' : sku ? 'Lưu thay đổi' : 'Thêm phân loại'}</button>
        <button className="btn btn-outline-secondary" type="button" onClick={onCancel}>Hủy</button></div>
    </fieldset>
  </form>
}
