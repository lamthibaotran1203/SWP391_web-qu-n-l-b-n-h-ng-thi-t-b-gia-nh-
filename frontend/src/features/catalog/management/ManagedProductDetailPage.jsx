import { Link, useLocation, useParams } from 'react-router-dom'
import { formatVnd } from '../api'
import { CatalogLoadError, CatalogPageHeading } from './CatalogWorkspace'
import { ProductStatus } from './ProductStatus'
import { ProductThumbnail } from './ProductThumbnail'
import { useCatalogReferences, useManagedProduct } from './useCatalogData'

export function ManagedProductDetailPage() {
  const { productId } = useParams()
  const { state } = useLocation()
  const { product, error, loading, reload } = useManagedProduct(productId)
  const references = useCatalogReferences()
  const path = `/staff/products/${productId}`

  return <>
    <CatalogPageHeading title="Chi tiết sản phẩm" detail>
      {product && <Link className="btn btn-primary" to={`${path}/edit`}>Chỉnh sửa sản phẩm</Link>}
    </CatalogPageHeading>
    {state?.notice && <p className="alert alert-success" role="status">{state.notice}</p>}
    {error && <CatalogLoadError error={error} onRetry={reload} />}
    {loading && <p className="management-loading" role="status">Đang tải thông tin sản phẩm…</p>}
    {product && <>
      <section className="management-overview" aria-label="Thông tin sản phẩm">
        <div className="management-product-image"><ProductThumbnail src={product.imageUrl} large name={product.name} /></div>
        <div>
          <ProductStatus status={product.status} />
          <h2>{product.name}</h2>
          <dl className="management-metadata">
            <div><dt>Danh mục</dt><dd>{references.loading ? 'Đang tải…' : references.categories.find((item) => item.id === product.categoryId)?.name || 'Chưa có thông tin'}</dd></div>
            <div><dt>Thương hiệu</dt><dd>{references.loading ? 'Đang tải…' : references.brands.find((item) => item.id === product.brandId)?.name || 'Chưa có thông tin'}</dd></div>
          </dl>
          {references.error && <CatalogLoadError error={references.error} onRetry={references.reload} />}
          <h3 className="management-section-label">Mô tả sản phẩm</h3>
          <p className="management-description">{product.description?.trim() || 'Chưa có mô tả sản phẩm.'}</p>
        </div>
      </section>
      <section className="management-list" aria-labelledby="sku-list-title">
        <div className="management-list-heading"><div><h2 id="sku-list-title">Phân loại và giá bán</h2>
          <p>Mỗi phân loại tương ứng một mẫu, dung tích hoặc quy cách bán.</p></div>
          <Link className="btn btn-outline-dark" to={`${path}/skus/new`}>Thêm phân loại</Link>
        </div>
        <div className="table-responsive"><table className="table management-table align-middle">
          <caption className="visually-hidden">Phân loại của {product.name}</caption>
          <thead><tr><th scope="col">Phân loại</th><th scope="col">Giá bán</th><th scope="col">Số lượng có thể bán</th><th scope="col">Trạng thái</th><th scope="col">Tùy chọn</th></tr></thead>
          <tbody>{product.skus.map((sku) => <tr key={sku.skuId}>
            <td><div className="management-product-link"><ProductThumbnail src={sku.imageUrl || product.imageUrl} />
              <div><strong>{sku.skuName}</strong><small>Mã hàng: {sku.skuCode}</small><small>Mã vạch: {sku.barcode}</small>
                {Object.entries(sku.attributes).map(([key, value]) => <small key={key}>{key}: {value}</small>)}
              </div></div></td>
            <td className="management-price">{formatVnd(sku.sellingPrice)}</td>
            <td><strong>{sku.availableQuantity}</strong>
              {sku.availableQuantity === 0 ? <span className="management-stock-note is-empty">Hết hàng</span>
                : sku.lowStock && <span className="management-stock-note">Sắp hết hàng</span>}</td>
            <td><ProductStatus status={sku.status} /></td>
            <td><Link to={`${path}/skus/${sku.skuId}/edit`} aria-label={`Chỉnh sửa ${sku.skuName}`}>Chỉnh sửa</Link></td>
          </tr>)}</tbody>
        </table></div>
        {!product.skus.length && <p className="management-empty">Sản phẩm chưa có phân loại.</p>}
      </section>
      <div className="management-bottom-actions"><Link to="/staff/products">Về danh sách sản phẩm</Link>
        <button className="btn btn-outline-secondary btn-sm" onClick={reload}>Làm mới thông tin</button></div>
    </>}
  </>
}
