import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { getRoleLabel } from '../auth/role-labels'
import './account-menu.css'

export function AccountMenu({ user, logout }) {
  const menu = useRef(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    function dismiss(event) {
      if (event.type === 'keydown' && event.key === 'Escape' && menu.current.open) {
        menu.current.open = false
        menu.current.querySelector('summary').focus()
      } else if (event.type === 'pointerdown' && !menu.current.contains(event.target)) {
        menu.current.open = false
      }
    }
    document.addEventListener('pointerdown', dismiss)
    document.addEventListener('keydown', dismiss)
    return () => {
      document.removeEventListener('pointerdown', dismiss)
      document.removeEventListener('keydown', dismiss)
    }
  }, [])

  async function signOut() {
    if (busy) return
    setBusy(true); setError('')
    try { await logout(); if (menu.current) menu.current.open = false }
    catch (failure) { setError(failure.message || 'Chưa thể đăng xuất. Vui lòng thử lại.') }
    finally { setBusy(false) }
  }

  return <details className="account-menu" ref={menu}>
    <summary className="app-user-chip" aria-label="Tài khoản của bạn">
      <span className="app-user-avatar" aria-hidden="true">{user.fullName?.charAt(0)?.toUpperCase() || 'U'}</span>
      <span><strong>{user.fullName}</strong><small>{getRoleLabel(user.role)}</small></span>
    </summary>
    <nav className="account-menu-panel" aria-label="Tài khoản">
      {user.role === 'CUSTOMER' && <Link to="/account/profile" onClick={() => { menu.current.open = false }}>Thông tin tài khoản</Link>}
      <Link to="/account/password" onClick={() => { menu.current.open = false }}>Đổi mật khẩu</Link>
      <button type="button" disabled={busy} onClick={signOut}>{busy ? 'Đang đăng xuất…' : 'Đăng xuất'}</button>
      {error && <p role="alert">{error}</p>}
    </nav>
  </details>
}
