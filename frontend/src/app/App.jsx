import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from 'react-router-dom'

import { AuthProvider } from '../auth/AuthProvider'
import { CatalogWorkspace } from '../features/catalog/management/CatalogWorkspace'
import { ManagedProductListPage } from '../features/catalog/management/ManagedProductListPage'
import { ManagedProductDetailPage } from '../features/catalog/management/ManagedProductDetailPage'
import { ManagedProductCreatePage, ManagedProductEditPage } from '../features/catalog/management/ManagedProductFormPage'
import { ManagedSkuFormPage } from '../features/catalog/management/ManagedSkuFormPage'
import { PersonalProfilePage } from '../features/profile/PersonalProfilePage'
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
          <Route path="/manager/catalog" element={<Navigate to="/staff/products" replace />} />
          <Route path="/staff/products" element={<CatalogWorkspace />}>
            <Route index element={<ManagedProductListPage />} />
            <Route path="new" element={<ManagedProductCreatePage />} />
            <Route path=":productId" element={<ManagedProductDetailPage />} />
            <Route path=":productId/edit" element={<ManagedProductEditPage />} />
            <Route path=":productId/skus/new" element={<ManagedSkuFormPage />} />
            <Route path=":productId/skus/:skuId/edit" element={<ManagedSkuFormPage />} />
          </Route>
          <Route path="/account/password" element={<ChangePasswordPage />} />
          <Route path="/account/profile" element={<PersonalProfilePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/products" element={<ProductListPage />} />
          <Route path="/products/:id" element={<ProductDetailPage />} />
          <Route path="/staff/orders" element={<StaffOrdersPage />} />
          <Route path="/staff/suppliers" element={<SuppliersPage />} />
          <Route path="/staff/purchase-orders/create" element={<CreatePurchaseOrderPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
