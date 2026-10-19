import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from 'react-router-dom'

import { AuthProvider } from '../auth/AuthProvider'
import { OperationsLayout, RequireRole } from '../components/OperationsLayout'
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
import { ProductCategoriesPage } from '../pages/ProductCategoriesPage'
import { CartPage } from '../features/cart/CartPage'
import { CheckoutHandoffPage } from '../features/cart/CheckoutHandoffPage'
import { addCartItem } from '../features/cart/api'
import { StaffOrdersPage } from '../pages/StaffOrdersPage'
import { SuppliersPage } from '../pages/SuppliersPage'
import { BrandsPage } from '../pages/BrandsPage'
import { CategoriesPage } from '../pages/CategoriesPage'
import { CreatePurchaseOrderPage } from '../pages/CreatePurchaseOrderPage'
import { PosPage } from '../features/pos/PosPage'

export function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/account/password" element={<ChangePasswordPage />} />
          <Route path="/account/profile" element={<PersonalProfilePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/verify-email" element={<VerifyEmailPage />} />
          <Route path="/products" element={<ProductListPage />} />
          <Route path="/categories" element={<ProductCategoriesPage />} />
          <Route path="/products/:id" element={<ProductDetailPage onAddToCart={addCartItem} />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutHandoffPage />} />
          <Route element={<OperationsLayout />}>
            <Route path="/staff" element={<Navigate to="/staff/orders" replace />} />
            <Route path="/staff/orders" element={<StaffOrdersPage />} />
            <Route path="/staff/pos" element={<PosPage />} />
            <Route element={<RequireRole roles={['ADMIN']} />}>
              <Route path="/admin" element={<Navigate to="/admin/products" replace />} />
              <Route path="/admin/catalog" element={<Navigate to="/admin/products" replace />} />
              <Route path="/admin/products" element={<CatalogWorkspace />}>
                <Route index element={<ManagedProductListPage />} />
                <Route path="new" element={<ManagedProductCreatePage />} />
                <Route path=":productId" element={<ManagedProductDetailPage />} />
                <Route path=":productId/edit" element={<ManagedProductEditPage />} />
                <Route path=":productId/skus/new" element={<ManagedSkuFormPage />} />
                <Route path=":productId/skus/:skuId/edit" element={<ManagedSkuFormPage />} />
              </Route>
              <Route path="/admin/categories" element={<CategoriesPage />} />
              <Route path="/admin/brands" element={<BrandsPage />} />
              <Route path="/admin/suppliers" element={<SuppliersPage />} />
              <Route path="/admin/purchase-orders/create" element={<CreatePurchaseOrderPage />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
