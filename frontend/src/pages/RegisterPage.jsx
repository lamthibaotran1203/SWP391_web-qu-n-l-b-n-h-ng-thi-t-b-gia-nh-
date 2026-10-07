import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
    register,
    resendVerification,
} from '../api/http'

export function RegisterPage() {
    const [form, setForm] = useState({
        fullName: '',
        email: '',
        password: '',
        confirmPassword: '',
    })

    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [resending, setResending] = useState(false)
    const [verificationEmail, setVerificationEmail] = useState('')

    function updateField(event) {
        setForm((current) => ({
            ...current,
            [event.target.name]: event.target.value,
        }))
    }

    async function handleSubmit(event) {
        event.preventDefault()

        setError('')
        setSuccess('')

        if (form.password !== form.confirmPassword) {
            setError('Mật khẩu xác nhận không khớp.')
            return
        }

        if (form.password.length < 8) {
            setError('Mật khẩu phải có ít nhất 8 ký tự.')
            return
        }

        setSubmitting(true)

        try {
            const submittedEmail = form.email.trim()

            const response = await register(
                submittedEmail,
                form.password,
                form.fullName,
            )

            setVerificationEmail(submittedEmail)

            setSuccess(
                response?.message
                || 'Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản.',
            )

            setForm({
                fullName: '',
                email: '',
                password: '',
                confirmPassword: '',
            })
        } catch (requestError) {
            setError(
                requestError.message
                || 'Không thể đăng ký tài khoản.',
            )
        } finally {
            setSubmitting(false)
        }
    }

    async function handleResendVerification() {
        if (!verificationEmail) {
            return
        }

        setError('')
        setSuccess('')
        setResending(true)

        try {
            const response =
                await resendVerification(
                    verificationEmail,
                )

            setSuccess(
                response?.message
                || 'Nếu tài khoản cần xác minh, email xác minh mới đã được gửi.',
            )
        } catch (requestError) {
            setError(
                requestError.message
                || 'Không thể gửi lại email xác minh.',
            )
        } finally {
            setResending(false)
        }
    }

    return (
        <main className="login-page">
            <section
                className="login-shell"
                aria-labelledby="register-title"
            >
                <Link className="back-link" to="/">
                    ← Quay lại trang chủ
                </Link>

                <p className="eyebrow">
                    Gia Dụng Việt
                </p>

                <h1 id="register-title">
                    Đăng ký tài khoản
                </h1>

                <p className="text-secondary">
                    Tạo tài khoản khách hàng để sử dụng hệ thống.
                </p>

                {error && (
                    <div
                        className="alert alert-danger"
                        role="alert"
                    >
                        {error}
                    </div>
                )}

                {success && (
                    <div
                        className="alert alert-success"
                        role="alert"
                    >
                        {success}
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="mb-3">
                        <label
                            className="form-label"
                            htmlFor="fullName"
                        >
                            Họ và tên
                        </label>

                        <input
                            className="form-control form-control-lg"
                            id="fullName"
                            name="fullName"
                            type="text"
                            autoComplete="name"
                            maxLength={100}
                            value={form.fullName}
                            onChange={updateField}
                            required
                        />
                    </div>

                    <div className="mb-3">
                        <label
                            className="form-label"
                            htmlFor="email"
                        >
                            Email
                        </label>

                        <input
                            className="form-control form-control-lg"
                            id="email"
                            name="email"
                            type="email"
                            autoComplete="email"
                            maxLength={254}
                            value={form.email}
                            onChange={updateField}
                            required
                        />
                    </div>

                    <div className="mb-3">
                        <label
                            className="form-label"
                            htmlFor="password"
                        >
                            Mật khẩu
                        </label>

                        <input
                            className="form-control form-control-lg"
                            id="password"
                            name="password"
                            type="password"
                            autoComplete="new-password"
                            minLength={8}
                            maxLength={72}
                            value={form.password}
                            onChange={updateField}
                            required
                        />
                    </div>

                    <div className="mb-4">
                        <label
                            className="form-label"
                            htmlFor="confirmPassword"
                        >
                            Xác nhận mật khẩu
                        </label>

                        <input
                            className="form-control form-control-lg"
                            id="confirmPassword"
                            name="confirmPassword"
                            type="password"
                            autoComplete="new-password"
                            minLength={8}
                            maxLength={72}
                            value={form.confirmPassword}
                            onChange={updateField}
                            required
                        />
                    </div>

                    <button
                        className="btn btn-dark btn-lg w-100"
                        type="submit"
                        disabled={submitting}
                    >
                        {submitting
                            ? 'Đang đăng ký…'
                            : 'Đăng ký'}
                    </button>
                </form>

                {verificationEmail && (
                    <div className="mt-3">
                        <p className="text-secondary mb-2">
                            Chưa nhận được email xác minh?
                        </p>

                        <button
                            className="btn btn-outline-dark w-100"
                            type="button"
                            onClick={handleResendVerification}
                            disabled={resending}
                        >
                            {resending
                                ? 'Đang gửi lại…'
                                : 'Gửi lại email xác minh'}
                        </button>
                    </div>
                )}

                <p className="text-secondary mt-4 mb-0">
                    Đã có tài khoản?{' '}
                    <Link to="/login">
                        Đăng nhập
                    </Link>
                </p>
            </section>
        </main>
    )
}