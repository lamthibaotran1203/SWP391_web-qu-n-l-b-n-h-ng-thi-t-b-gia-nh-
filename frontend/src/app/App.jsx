import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from 'react-router-dom'

import { AuthProvider } from '../auth/AuthProvider'
import { CatalogManagementPage } from '../features/catalog/management/CatalogManagementPage'
import { HomePage } from '../pages/HomePage'
import { ChangePasswordPage } from '../pages/ChangePasswordPage'
import { LoginPage } from '../pages/LoginPage'
import { RegisterPage } from '../pages/RegisterPage'
import { VerifyEmailPage } from '../pages/VerifyEmailPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { ProductListPage } from '../pages/ProductListPage'
import { StaffOrdersPage } from '../pages/StaffOrdersPage'
import { SuppliersPage } from '../pages/SuppliersPage'
import { CreatePurchaseOrderPage } from '../pages/CreatePurchaseOrderPage'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/manager/catalog" element={<CatalogManagementPage />} />
          <Route path="/account/password" element={<ChangePasswordPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/products" element={<ProductListPage />} />
          <Route path="/products/:id" element={<ProductDetailPage />} />
          <Route path="/staff/orders" element={<StaffOrdersPage />} />
          <Route path="/manager/suppliers" element={<SuppliersPage />} />
          <Route path="/manager/purchase-orders/create" element={<CreatePurchaseOrderPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}