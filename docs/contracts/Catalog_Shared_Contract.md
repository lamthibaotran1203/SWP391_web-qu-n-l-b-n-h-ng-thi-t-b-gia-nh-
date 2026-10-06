# Shared catalogue integration contract — 2026-10-05

Approved by the user and recorded in canonical Backlog J14 before implementation.
Applies to AnNP UC-13 prerequisites and teammates' integration. RDS changes are deferred;
personal follow-up notes and intermediate review evidence remain local and are not versioned. Search/detail/cart/stock workflows owned by
other members are not implemented here; UC-13 management API/basic UI have since been implemented locally; see
[the current management contract](UC13_Catalog_Management_Contract.md) for writes and versions.
The dated read contract below records the earlier foundation; teammate filter/sort additions still need documentation reconciliation.

## Data sources

| Concept | Authoritative source | Meaning |
|---|---|---|
| Product | `app.product.product_id` | Shared name/description/category/brand/cover image/publication |
| Sale unit | `app.hm_sku.id` | Manual model/capacity/package SKU; code and internal barcode unique |
| Selling price | `app.hm_sku.selling_price` | Whole VND, nonnegative; original price separate |
| Available stock | `app.hm_inventory.available_quantity` | Generated physical minus reserved; missing row means 0 for reads |
| Purchased price/name | `app.hm_order_item` snapshots | Frozen at order creation; not today's catalogue values |
| Old imported values | `product.legacy_*` | Immutable historical archive; never read/write as current price/stock |

**Breaking change for V2/V3 callers:** product `sku`, `price`, `stock_quantity`, `category`,
`brand` were renamed by V5 to `legacy_*`. Update old code to SKU/inventory and category/brand
FKs. V1–V4 are unchanged. V5 keeps historical values, blocks edits/new archive population and
makes new product default INACTIVE. Never synchronize the archive with current values.

Only ACTIVE undeleted product + ACTIVE undeleted SKU + undeleted category and brand are
publicly visible. Product publication requires category, brand and a nonblank product image.
SKU image overrides the product cover when nonblank. Zero available stock stays visible;
`purchasable=false`. That flag is a current hint, not a stock reservation or purchase guarantee.

## Read-only API

Every success uses existing `{ "data": ... }`; errors use `ApiProblem` with code/detail/traceId.
GET is public, with no browser database key. Other methods remain session/CSRF protected.

| Endpoint | `data` |
|---|---|
| `GET /api/v1/catalog/skus?page=0&size=20` | `{items,page,size,totalElements}` — pages count **SKUs**, not products |
| `GET /api/v1/catalog/skus/{skuId}` | One visible sale unit |
| `GET /api/v1/catalog/products/{productId}` | All visible sale units of one product, ordered by skuId |
| `GET /api/v1/catalog/categories` | Undeleted `{id,name}` entries ordered by ID |
| `GET /api/v1/catalog/brands` | Undeleted `{id,name}` entries ordered by ID |

Page starts at 0; size 1–100; stable skuId ascending ordering. Invalid page/size or malformed
numeric parameter returns 400. Missing/hidden sale unit or product without visible SKUs returns
404 `CATALOG_NOT_FOUND`; detail never reveals hidden catalogue rows. SKU list/count run in one
repeatable-read transaction. No keyword/filter engine is included; its owner can build on this
contract without reading legacy columns.

Sale-unit DTO fields:

```json
{
  "skuId": 1,
  "productId": 1,
  "productName": "Nồi inox mẫu 2 lít",
  "skuCode": "DEMO-SKU-1",
  "skuName": "Dung tích 2 lít",
  "imageUrl": "/images/catalog-demo.svg",
  "categoryId": 1,
  "categoryName": "Đồ gia dụng mẫu",
  "brandId": 1,
  "brandName": "HomeMart Demo",
  "sellingPrice": "200000",
  "availableQuantity": 5,
  "purchasable": true,
  "productVersion": 0,
  "skuVersion": 0
}
```

IDs in this example are illustrative; discover actual IDs through the API. Money is a decimal
**string**, never a JavaScript Number source. Use `frontend/src/features/catalog/api.js`:
`getCatalogSkus`, `getCatalogSku`, `getProductSaleUnits`, `getCatalogCategories`,
`getCatalogBrands`, and `formatVnd`. Consumers must not submit client price/availability as
trusted values. Checkout rereads authoritative values and snapshots them transactionally.

## Write boundaries for UC owners

UC-13 changes product/SKU catalogue fields only. It does not change physical/reserved inventory
or insert stock movements. New SKU without inventory is safe to read as out of stock; stock
owners initialize/change inventory with the corresponding immutable movement in one transaction.
Category/brand existence, deletion and publishability must be checked in UC-13's service.
Changing catalogue price does not rewrite order snapshots, payment/refund evidence or archives.

Management writes must check productVersion/skuVersion and increment the affected version;
return a conflict for stale edits. Version fields alone are not a complete optimistic-lock
implementation. For transactions that lock several catalogue/stock rows, agree on product IDs
ascending, then SKU IDs ascending, then inventory SKU IDs ascending; keep external calls outside
locks. Checkout/stock owners must recheck visibility, price and available stock under their
transaction's concurrency rules. No generic variants engine or two-way sync trigger is included.

## Demo and verification

From repository root (same commands on macOS/Linux and Windows PowerShell), with Java 21,
Node 24/npm 11 and ignored local DB environment configured:

```text
npm run frontend:check
npm run backend:verify:postgres
npm run db:info
npm run db:migrate
npm run db:seed:catalog
npm run db:seed:catalog
```

`backend:verify:postgres` uses a disposable PostgreSQL 17.11 Docker database, Java 21/Maven
3.9.16, builds the JAR and removes its own database/network. It never loads `.env` or touches
Supabase. Seed is explicit and transactional, uses reserved DEMO codes and does not overwrite
existing records. It adds two labelled demo products/SKUs (stock 5 and 0), category/brand,
one nonzero opening movement, and no users. It is not a production inventory import. Deleted
or modified demo entries are not silently reset. Do not reuse DEMO codes for real merchandise.

On 2026-10-05: 11 PostgreSQL backend tests, 2 frontend tests and lint/build passed. V5 applied
and validated on Supabase; original catalogue aggregate digest matched after column rename.
A local V1–V3 -> V4 -> V5 upgrade also preserved price/stock/code; rerun executed zero.
A live Spring/JDBC API smoke against Supabase validates visibility, price/stock, image and
400/404 errors. No Manager was created. Mail health is excluded only in this catalogue preview because SMTP verification is outside
this task; database health is checked. Temporary preview uses localhost:8085; it is not a
team deployment. Stop it with `docker stop homemart-catalog-preview` and remove it with
`docker rm homemart-catalog-preview` when no longer needed.
