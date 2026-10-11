import { Link, useNavigate, useParams } from 'react-router-dom'
import { CatalogLoadError, CatalogPageHeading } from './CatalogWorkspace'
import { SkuEditor } from './SkuEditor'
import { addManagedSku, editManagedSku } from './api'
import { useManagedProduct } from './useCatalogData'

export function ManagedSkuFormPage() {
  const { productId, skuId } = useParams()
  const navigate = useNavigate()
  const { product, error, loading, reload } = useManagedProduct(productId)
  const sku = product?.skus.find((item) => String(item.skuId) === skuId)
  const missingSku = product && skuId && !sku
  const path = `/manager/products/${productId}`

  async function save(fields) {
    if (skuId) await editManagedSku(product.productId, sku.skuId, product.productVersion, sku.skuVersion, fields)
    else await addManagedSku(product.productId, product.productVersion, fields)
    navigate(path, { replace: true, state: { notice: skuId ? 'Đã lưu phân loại.' : 'Đã thêm phân loại.' } })
  }

  return <>
    <CatalogPageHeading title={skuId ? 'Chỉnh sửa phân loại' : 'Thêm phân loại'} detail>
      <Link className="btn btn-outline-dark" to={path}>Chi tiết sản phẩm</Link>
    </CatalogPageHeading>
    {error && <CatalogLoadError error={error} onRetry={reload} />}
    {loading && <p role="status">Đang tải dữ liệu phân loại…</p>}
    {missingSku && <p className="alert alert-danger" role="alert">Phân loại này không thuộc sản phẩm hoặc không còn tồn tại.</p>}
    {product && !missingSku && <div className="management-form">
      <h2 className="h5 mb-4">{product.name}</h2>
      <SkuEditor key={`${product.productVersion}:${sku?.skuVersion ?? 'new'}`} sku={sku} onSave={save} onCancel={() => navigate(path)} onReload={reload} />
    </div>}
  </>
}
