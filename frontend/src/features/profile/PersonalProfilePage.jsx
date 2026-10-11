import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'
import { AccountPage } from '../../components/AccountPage'
import { getPersonalProfile } from './api'
import { PersonalProfileForm } from './PersonalProfileForm'

export function PersonalProfilePage() {
  const { user, connection, reload } = useAuth()
  if (connection.status === 'loading') return <p className="container py-4" role="status">Đang kiểm tra phiên đăng nhập…</p>
  if (connection.status === 'error') return <div className="container py-4" role="alert">
    <p>{connection.message || 'Chưa kết nối được máy chủ.'}</p>
    <button className="btn btn-outline-dark" onClick={reload}>Kết nối lại</button>
  </div>
  if (!user.authenticated) return <Navigate to="/login" replace state={{ from: '/account/profile' }} />
  if (user.role !== 'CUSTOMER') return <main className="container py-4">
    <h1>Trang hồ sơ dành cho khách hàng</h1><Link to="/">Về trang chủ</Link>
  </main>
  return <AccountPage title="Thông tin tài khoản"><PersonalProfileContent />
    <Link className="account-link" to="/account/password">Đổi mật khẩu</Link>
  </AccountPage>
}

function PersonalProfileContent() {
  const { saveProfile, reload } = useAuth()
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState(null)
  const current = result?.revision === revision ? result : null

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    getPersonalProfile({ signal: controller.signal })
      .then((profile) => { if (active) setResult({ revision, profile }) })
      .catch((error) => { if (active) setResult({ revision, error }) })
    return () => { active = false; controller.abort() }
  }, [revision])

  async function save(fields) {
    try {
      const profile = await saveProfile(fields)
      setResult({ revision, profile })
      return profile
    } catch (error) {
      if (error.status === 401) await reload()
      throw error
    }
  }

  if (!current) return <p role="status">Đang tải thông tin tài khoản…</p>
  if (current.error) return <div className="alert alert-danger" role="alert"><p>{current.error.message}</p>
    {current.error.status === 401 ? <Link to="/login">Đăng nhập lại</Link>
      : <button className="btn btn-outline-dark" onClick={() => setRevision((value) => value + 1)}>Thử tải lại</button>}
  </div>
  return <section className="account-form" aria-label="Hồ sơ cá nhân">
    <PersonalProfileForm profile={current.profile} onSave={save} />
  </section>
}
