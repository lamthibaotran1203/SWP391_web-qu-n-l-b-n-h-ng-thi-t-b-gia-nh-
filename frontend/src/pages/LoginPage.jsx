import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'

export function LoginPage() {
  const { login } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  const [form, setForm] = useState({
    email: '',
    password: '',
  })

  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function updateField(event) {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }))
  }

  async function handleSubmit(event) {
    event.preventDefault()

    setError('')
    setSubmitting(true)

    try {
      await login(form.email, form.password)
      navigate(location.state?.from || '/')
    } catch (requestError) {
      setError(
        requestError.message
        || 'Không thể đăng nhập.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="plain-page">
      <section className="plain-container plain-form" aria-labelledby="login-title">
        <Link to="/">← Quay lại trang chủ</Link>
        <h1 className="mt-4" id="login-title">Đăng nhập hệ thống</h1>
        <p className="text-secondary">
          Đăng nhập bằng tài khoản của bạn.
        </p>

        {error && (
          <div
            className="alert alert-danger"
            role="alert"
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <label
              className="form-label"
              htmlFor="email"
            >
              Email
            </label>

            <input
              className="form-control"
              id="email"
              name="email"
              type="email"
              autoComplete="username"
              value={form.email}
              onChange={updateField}
              required
            />
          </div>

          <div className="mb-4">
            <label
              className="form-label"
              htmlFor="password"
            >
              Mật khẩu
            </label>

            <input
              className="form-control"
              id="password"
              name="password"
              type="password"
              autoComplete="current-password"
              value={form.password}
              onChange={updateField}
              required
            />
          </div>
          <button className="btn btn-dark" type="submit" disabled={submitting}>
            {submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
          </button>
        </form>

        <p className="text-secondary mt-4 mb-0">
          Chưa có tài khoản?{' '}
          <Link to="/register">
            Đăng ký ngay
          </Link>
        </p>
      </section>
    </main>
  )
}