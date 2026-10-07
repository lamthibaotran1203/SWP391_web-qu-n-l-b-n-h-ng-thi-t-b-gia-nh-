import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { verifyEmail } from '../api/http'

export function VerifyEmailPage() {
    const [searchParams] = useSearchParams()
    const token = searchParams.get('token')

    const verificationTokenRef = useRef(null)

    const [status, setStatus] = useState(() =>
        token ? 'loading' : 'error',
    )

    const [message, setMessage] = useState(() =>
        token
            ? 'Đang xác minh email...'
            : 'Liên kết xác minh không hợp lệ.',
    )

    useEffect(() => {
        if (!token) {
            return
        }

        if (verificationTokenRef.current === token) {
            return
        }

        verificationTokenRef.current = token

        let active = true

        async function executeVerification() {
            try {
                const response = await verifyEmail(token)

                if (!active) {
                    return
                }

                setStatus('success')
                setMessage(
                    response || 'Xác minh email thành công.',
                )
            } catch (requestError) {
                if (!active) {
                    return
                }

                setStatus('error')
                setMessage(
                    requestError.message
                    || 'Không thể xác minh email.',
                )
            }
        }

        executeVerification()

        return () => {
            active = false
        }
    }, [token])

    return (
        <main className="login-page">
            <section
                className="login-shell"
                aria-labelledby="verify-title"
            >
                <p className="eyebrow">
                    Gia Dụng Việt
                </p>

                <h1 id="verify-title">
                    Xác minh email
                </h1>

                {status === 'loading' && (
                    <div
                        className="alert alert-info"
                        role="status"
                    >
                        {message}
                    </div>
                )}

                {status === 'success' && (
                    <>
                        <div
                            className="alert alert-success"
                            role="alert"
                        >
                            {message}
                        </div>

                        <Link
                            className="btn btn-dark btn-lg w-100"
                            to="/login"
                        >
                            Đăng nhập
                        </Link>
                    </>
                )}

                {status === 'error' && (
                    <>
                        <div
                            className="alert alert-danger"
                            role="alert"
                        >
                            {message}
                        </div>

                        <Link
                            className="btn btn-outline-dark w-100"
                            to="/"
                        >
                            Quay lại trang chủ
                        </Link>
                    </>
                )}
            </section>
        </main>
    )
}