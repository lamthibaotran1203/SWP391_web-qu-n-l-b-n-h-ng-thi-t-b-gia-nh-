import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { changeOwnPassword } from '../api/http'
import { AccountPage } from '../components/AccountPage'

const emptyForm = { currentPassword: '', newPassword: '', confirmation: '' }

export function ChangePasswordPage() {
  const { user, connection, reload } = useAuth()
  const [form, setForm] = useState(emptyForm)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState('')
  const [showPasswords, setShowPasswords] = useState(false)

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    if (submitting) return
    setError('')
    setFieldErrors({})
    if (form.newPassword !== form.confirmation) {
      setFieldErrors({ confirmation: 'Xác nhận mật khẩu mới chưa khớp.' })
      return
    }
    setSubmitting(true)
    try {
      const result = await changeOwnPassword(form.currentPassword, form.newPassword, form.confirmation)
      setForm(emptyForm)
      setShowPasswords(false)
      setSuccess(result.message)
      await reload()
    } catch (failure) {
      setError(failure.message || 'Chưa thể đổi mật khẩu. Vui lòng thử lại.')
      setFieldErrors(failure.fieldErrors || {})
      if (failure.status === 401) await reload()
    } finally {
      setSubmitting(false)
    }
  }

  if (!success && connection.status === 'loading') return <AccountPage title="Đổi mật khẩu">
    <p role="status">Đang kiểm tra phiên đăng nhập…</p>
  </AccountPage>
  if (!success && connection.status === 'error') return <AccountPage title="Đổi mật khẩu">
    <div className="alert alert-danger" role="alert"><p>{connection.message || 'Chưa kết nối được máy chủ.'}</p>
      <button className="btn btn-outline-secondary" onClick={reload}>Kết nối lại</button>
    </div>
  </AccountPage>
  if (!user.authenticated && !success) return <Navigate to="/login" replace state={{ from: '/account/password' }} />
  const back = user.role === 'CUSTOMER' ? '/account/profile' : '/'

  return (
    <AccountPage title="Đổi mật khẩu" parent={user.role === 'CUSTOMER' ? { to: back, label: 'Thông tin tài khoản' } : null}>
      <section className="account-form" aria-label="Thay đổi mật khẩu">
        {success ? <section aria-live="polite">
          <div className="alert alert-success" role="status">{success}</div>
          <Link className="btn btn-primary" to="/login">Đăng nhập lại</Link>
        </section> : <>
          <p className="mb-4">Mật khẩu mới từ 8–64 ký tự. Sau khi đổi, bạn cần đăng nhập lại.</p>
          {error && <div className="alert alert-danger" role="alert">{error}</div>}
          <form onSubmit={handleSubmit} aria-busy={submitting}>
            {[
              ['currentPassword', 'Mật khẩu hiện tại', 'current-password'],
              ['newPassword', 'Mật khẩu mới', 'new-password'],
              ['confirmation', 'Xác nhận mật khẩu mới', 'new-password'],
            ].map(([name, label, autoComplete]) => <div className="mb-3" key={name}>
              <label className="form-label" htmlFor={name}>{label}</label>
              <input id={name} name={name} type={showPasswords ? 'text' : 'password'} className={`form-control${fieldErrors[name] ? ' is-invalid' : ''}`}
                autoComplete={autoComplete} value={form[name]} onChange={updateField} required maxLength={144}
                disabled={submitting} aria-invalid={Boolean(fieldErrors[name])}
                aria-describedby={fieldErrors[name] ? `${name}-error` : undefined} />
              {fieldErrors[name] && <div id={`${name}-error`} className="invalid-feedback">{fieldErrors[name]}</div>}
            </div>)}
            <div className="form-check mb-4">
              <input className="form-check-input" type="checkbox" id="show-passwords" checked={showPasswords}
                onChange={(event) => setShowPasswords(event.target.checked)} disabled={submitting} />
              <label className="form-check-label" htmlFor="show-passwords">Hiện mật khẩu</label>
            </div>
            <div className="account-actions">
              <button className="btn btn-primary" type="submit" disabled={submitting}>
                {submitting ? 'Đang đổi mật khẩu…' : 'Đổi mật khẩu'}
              </button>
              <Link to={back} className="btn btn-outline-secondary">Hủy</Link>
            </div>
          </form>
        </>}
      </section>
    </AccountPage>
  )
}
