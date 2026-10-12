import { useState } from 'react'

function fieldsOf(profile) { return { fullName: profile.fullName, phone: profile.phone || '' } }

export function PersonalProfileForm({ profile, onSave }) {
  const [fields, setFields] = useState(() => fieldsOf(profile))
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [saved, setSaved] = useState(false)
  const [busy, setBusy] = useState(false)

  function change(event) {
    setFields((current) => ({ ...current, [event.target.name]: event.target.value }))
    setSaved(false)
  }

  async function submit(event) {
    event.preventDefault()
    if (busy) return
    const name = fields.fullName.trim().replace(/\s+/g, ' ')
    const phone = fields.phone.trim()
    const invalid = {}
    if (name.length < 2 || name.length > 100) invalid.fullName = 'Họ và tên cần từ 2–100 ký tự.'
    if (phone && !/^0[0-9]{9}$/.test(phone)) invalid.phone = 'Số điện thoại cần 10 chữ số, bắt đầu bằng 0.'
    setErrors(invalid); setError(''); setSaved(false)
    if (Object.keys(invalid).length) return
    setBusy(true)
    try {
      const current = await onSave({ fullName: name, phone })
      setFields(fieldsOf(current)); setSaved(true)
    } catch (failure) {
      setError(failure.message || 'Chưa thể lưu thông tin tài khoản. Vui lòng thử lại.')
      setErrors(failure.fieldErrors || {})
    } finally { setBusy(false) }
  }

  return <form onSubmit={submit} aria-busy={busy}>
    {error && <p className="alert alert-danger" role="alert">{error}</p>}
    {saved && <p className="alert alert-success" role="status">Đã lưu thông tin tài khoản.</p>}
    <fieldset disabled={busy}>
      <div className="mb-4"><label className="form-label" htmlFor="profile-email">Email</label>
        <input id="profile-email" className="form-control" type="email" value={profile.email} readOnly />
      </div>
      <div className="mb-4"><label className="form-label" htmlFor="profile-name">Họ và tên</label>
        <input id="profile-name" name="fullName" className={`form-control${errors.fullName ? ' is-invalid' : ''}`}
          autoComplete="name" value={fields.fullName} onChange={change} required maxLength={100}
          aria-invalid={Boolean(errors.fullName)} aria-describedby={errors.fullName ? 'profile-name-error' : undefined} />
        {errors.fullName && <div className="invalid-feedback" id="profile-name-error">{errors.fullName}</div>}
      </div>
      <div className="mb-4"><label className="form-label" htmlFor="profile-phone">Số điện thoại</label>
        <input id="profile-phone" name="phone" className={`form-control${errors.phone ? ' is-invalid' : ''}`}
          type="tel" inputMode="tel" autoComplete="tel" value={fields.phone} onChange={change} maxLength={10}
          aria-invalid={Boolean(errors.phone)} aria-describedby={errors.phone ? 'profile-phone-error' : 'profile-phone-help'} />
        {errors.phone ? <div className="invalid-feedback" id="profile-phone-error">{errors.phone}</div>
          : <p className="form-text" id="profile-phone-help">10 chữ số, bắt đầu bằng 0. Có thể để trống.</p>}
      </div>
      <div className="account-actions">
        <button className="btn btn-primary" type="submit">{busy ? 'Đang lưu…' : 'Lưu thay đổi'}</button>
        <button className="btn btn-outline-secondary" type="button" onClick={() => {
          setFields(fieldsOf(profile)); setErrors({}); setError(''); setSaved(false)
        }}>Hủy thay đổi</button>
      </div>
    </fieldset>
  </form>
}
