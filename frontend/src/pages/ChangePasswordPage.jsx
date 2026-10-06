import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { changeOwnPassword } from '../api/http'

const emptyForm = { currentPassword: '', newPassword: '', confirmation: '' }

export function ChangePasswordPage() {
  const { user, connection, reload } = useAuth()
  const [form, setForm] = useState(emptyForm)
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState('')

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

  if (connection.status === 'loading') return <main className="plain-page">Đang kiểm tra phiên đăng nhập…</main>
  if (!user.authenticated && !success) return <Navigate to="/login" replace />

  return (
    <main className="plain-page">
      <div className="plain-container plain-form">
        <Link to="/">← Trang chủ</Link>
        <h1 className="mt-4">Đổi mật khẩu</h1>
        {success ? <section aria-live="polite">
          <div className="alert alert-success" role="status">{success}</div>
          <Link className="btn btn-dark" to="/login">Đăng nhập lại</Link>
        </section> : <>
          <p>Mật khẩu mới từ 8–64 ký tự. Sau khi đổi, bạn cần đăng nhập lại.</p>
          {error && <div className="alert alert-danger" role="alert">{error}</div>}
          <form onSubmit={handleSubmit} aria-busy={submitting}>
            {[
              ['currentPassword', 'Mật khẩu hiện tại', 'current-password'],
              ['newPassword', 'Mật khẩu mới', 'new-password'],
              ['confirmation', 'Xác nhận mật khẩu mới', 'new-password'],
            ].map(([name, label, autoComplete]) => <div className="mb-3" key={name}>
              <label className="form-label" htmlFor={name}>{label}</label>
              <input id={name} name={name} type="password" className={`form-control${fieldErrors[name] ? ' is-invalid' : ''}`}
                autoComplete={autoComplete} value={form[name]} onChange={updateField} required maxLength={144}
                disabled={submitting} aria-invalid={Boolean(fieldErrors[name])}
                aria-describedby={fieldErrors[name] ? `${name}-error` : undefined} />
              {fieldErrors[name] && <div id={`${name}-error`} className="invalid-feedback">{fieldErrors[name]}</div>}
            </div>)}
            <button className="btn btn-dark" type="submit" disabled={submitting}>
              {submitting ? 'Đang đổi mật khẩu…' : 'Đổi mật khẩu'}
            </button>
            <Link to="/" className="btn btn-outline-secondary ms-2">Hủy</Link>
          </form>
        </>}
      </div>
    </main>
  )
}
