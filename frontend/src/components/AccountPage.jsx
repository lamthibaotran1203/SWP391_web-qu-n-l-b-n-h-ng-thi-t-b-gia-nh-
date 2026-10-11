import { Link } from 'react-router-dom'
import { AppHeader } from './AppShell'
import '../styles/account.css'

export function AccountPage({ title, parent, children }) {
  return <div className="account-page"><AppHeader />
    <main className="container account-content">
      <nav aria-label="Đường dẫn"><Link to="/">Trang chủ</Link>
        {parent && <><span aria-hidden="true"> / </span><Link to={parent.to}>{parent.label}</Link></>}
        <span aria-hidden="true"> / </span><span aria-current="page">{title}</span>
      </nav>
      <h1>{title}</h1>{children}
    </main>
  </div>
}
