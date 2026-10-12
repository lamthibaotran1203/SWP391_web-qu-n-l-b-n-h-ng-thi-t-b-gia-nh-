import { Link, useNavigate, useParams } from 'react-router-dom'
import { CatalogLoadError, CatalogPageHeading } from './CatalogWorkspace'
import { ProductEditor } from './ProductEditor'
import { createManagedProduct, editManagedProduct } from './api'
import { useCatalogReferences, useManagedProduct } from './useCatalogData'

export function ManagedProductCreatePage() {
  const navigate = useNavigate()
  const references = useCatalogReferences()

  async function save(fields, skus) {
    const product = await createManagedProduct(fields, skus)
    navigate(`/manager/products/${product.productId}`, { replace: true, state: { notice: 'Đã thêm sản phẩm.' } })
  }

  return <>
    <CatalogPageHeading title="Thêm sản phẩm" detail />
    {references.error && <CatalogLoadError error={references.error} onRetry={references.reload} />}
    {references.loading && <p role="status">Đang tải danh mục và thương hiệu…</p>}
    {!references.loading && !references.error && <div className="management-form">
      <ProductEditor {...references} onSave={save} onCancel={() => navigate('/manager/products')} />
    </div>}
    {(references.error || references.loading) && <Link to="/manager/products">Về danh sách sản phẩm</Link>}
  </>
}

export function ManagedProductEditPage() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const { product, error, loading, reload } = useManagedProduct(productId)
  const references = useCatalogReferences()
  const path = `/manager/products/${productId}`

  async function save(fields) {
    await editManagedProduct(product.productId, product.productVersion, fields)
    navigate(path, { replace: true, state: { notice: 'Đã lưu thông tin sản phẩm.' } })
  }

  return <>
    <CatalogPageHeading title="Chỉnh sửa sản phẩm" detail>
      <Link className="btn btn-outline-dark" to={path}>Xem chi tiết</Link>
    </CatalogPageHeading>
    {error && <CatalogLoadError error={error} onRetry={reload} />}
    {references.error && <CatalogLoadError error={references.error} onRetry={references.reload} />}
    {(loading || references.loading) && <p role="status">Đang tải dữ liệu chỉnh sửa…</p>}
    {product && !references.loading && !references.error && <div className="management-form">
      <ProductEditor key={`${product.productId}:${product.productVersion}`} product={product} {...references}
        onSave={save} onCancel={() => navigate(path)} onReload={reload} />
    </div>}
  </>
}
