# UC-13 — Product and manual SKU management

Implemented locally on 2026-10-06 by the current AnNP task. UC-13 ownership and definition
come from canonical Backlog row 14; I14/J14 record schema and catalogue decisions. The live
RDS UC-13 is comparison evidence, not a verified frozen submitted revision. RDS edits are
deferred; personal review/learning files remain ignored under docs/database/.

## Scope and data

RDS Admin catalogue actor maps to the existing MANAGER role; no new account role is introduced.
CUSTOMER/STAFF cannot read or write management APIs. Same-origin session + CSRF protect writes.
Product fields live in app.product; sale-unit fields and whole-VND prices in app.hm_sku.
Category and brand management belong to their UC owners. This feature reads their existing entries.
Inventory/stock movements, order snapshots, immutable legacy_* fields are never changed here.
No Flyway migration is required: V1–V5 already provide the required columns/keys/versions.

Product draft/deactivation is INACTIVE. Existing records/SKUs and historical FKs remain intact;
there is no physical deletion. An ACTIVE product requires undeleted category and brand plus
nonblank product.image_url; SKU image is optional with public product-image fallback.
SKU activation validates those parent publication fields even when the parent is currently unpublished;
public visibility still requires both product and SKU ACTIVE. Deactivating a product does not
silently rewrite each SKU's status. Republishing restores visibility of its still-ACTIVE SKUs.

An inactive product edit may retain its existing soft-deleted classification reference so a Manager
can stop sale. Newly assigning a deleted reference and activating with one are rejected. Other owners
must preserve their reference-deletion integration contracts; public queries hide deleted references.

Identifiers are trimmed, nonblank, <=100 characters and globally unique, including inactive/deleted
rows. Existing PostgreSQL uniqueness is case-sensitive. Product names are 2–200, SKU names 1–255.
Prices are nonnegative whole-VND values with at most 18 integer digits; fractional/overflow input is
rejected before PostgreSQL can round numeric(18,0). Threshold is a nonnegative integer. JSON integer
fields reject fractional input. Public selling price is the final configured retail VND value;
this slice introduces no VAT calculation engine or claim of legal compliance.

Optional attributes are <=20 manually entered text key/value pairs; trimmed nonblank keys <=50,
values <=255; no automatic variant permutations. Image accepts existing http(s) URL or root-relative
path. No image upload subsystem is implemented. Audit data is generated server-side.

## Approved BR-14 clarification

User approved and recorded in Backlog J14 on 2026-10-06: warning recalculates when management
details are read/refreshed. lowStock = availableQuantity < minStockThreshold; missing inventory
means zero for this read. This is a visible/API warning, not an email or background notification job.
It does not reserve stock or promise availability at checkout.

## APIs

Success envelope is {data: ...}; errors use existing ApiProblem with code/detail/traceId.
The frontend management route is /manager/catalog, with a MANAGER link on Home.

| Method/path under /api/v1/manager/catalog | Request | Result |
| --- | --- | --- |
| GET /products?search=&status=&page=0&size=20 | Optional name search/status ACTIVE/INACTIVE; page >=0, size 1–100 | ManagementPage: items,page,size,totalElements. One item per product, skus=[]; includes unpublished products. |
| GET /products/{productId} | None | ManagedProduct including all undeleted active/inactive SKUs. |
| POST /products | {product: ProductFields, skus: [SkuFields,...]} | 201; creates master and 1–50 manual SKUs atomically. |
| PUT /products/{productId} | {productVersion, product: ProductFields} | Updated detail. Set status INACTIVE to deactivate. |
| POST /products/{productId}/skus | {productVersion, sku: SkuFields} | 201; adds manual SKU. |
| PUT /products/{productId}/skus/{skuId} | {productVersion, skuVersion, sku: SkuFields} | Updated detail. Set SKU status INACTIVE to deactivate. |

ProductFields: name,description,categoryId,brandId,imageUrl,status.
SkuFields: skuCode,barcode,skuName,attributes,imageUrl,originalPrice,sellingPrice,minStockThreshold,status.
Clients do not supply stock or audit fields. A SKU cannot be moved to another product via the API.
Monetary response values are decimal strings; frontend preserves decimal strings when writing.
The service validates direct calls as well as HTTP DTO validation.

ManagedProduct: productId, product fields, productVersion, skus.
ManagedSku: skuId,productId,sku fields,skuVersion,availableQuantity,lowStock.
Reads exclude is_deleted rows; inactive items remain reviewable by Managers.

## Transactions and conflicts

- All management writes are service transactions. Product creation + every initial SKU either commit together or roll back together.
- Existing writes lock the parent product first, then the owned SKU. Shared reference locks protect validation against concurrent deletion.
- productVersion must match on all existing-product writes; SKU edit additionally checks skuVersion. Any SKU write increments parent version;
  a SKU edit increments its own version. Reload returned details after saving; a stale request returns 409 CATALOG_STALE.
- Identifier checks provide useful errors; database UNIQUE remains authoritative during competing creates. Conflicts return
  409 SKU_CODE_DUPLICATE or SKU_BARCODE_DUPLICATE without leaving an extra master/partial SKU list.
- Invalid fields/references/publication input return 400; missing records/wrong parent return 404; session 401, permission/CSRF 403.
  Persistence unavailability returns a safe 503 CATALOG_SAVE_UNAVAILABLE; previous state is retained.
- Already-invalid sessions are revoked before management checks, including after a role change; a demoted Manager cannot retain cached Manager permissions.

## Existing API integration

Public GET /api/v1/catalog/** and the teammate GET /api/v1/products compatibility routes remain.
On /api/v1/products/{id}, that compatibility ID denotes a sale-unit skuId, not a management productId.
Old POST/PUT/DELETE /api/v1/products routes are retired: non-Managers are denied; Managers receive
410 CATALOG_WRITE_MOVED and must use the management contract. The old ProductService/JPA legacy
write model/mapper were removed because their only callers belonged to that retired path.
Do not restore writes to legacy_* or let a public compatibility ID become a master product write ID.

## Verification and traceability

From root with Node 24/npm 11 and Docker:

```text
npm run frontend:check
npm run backend:verify:postgres
```

CatalogManagementIntegrationTest proves normal multi-SKU create, inactive management reads,
publication/reference/price/threshold/attribute guards, duplicate rollback, all non-manager roles,
CSRF/anonymous/retired paths, stale versions, concurrent updates/duplicate creates, simulated database
failure, own-parent SKU guards, deactivation retaining deleted references, demotion revocation,
unchanged inventory/archives/order snapshots. Frontend tests cover money precision, duplicate manual
attributes, stale-edit errors, permission and retry states. No H2 or shared Supabase fixtures are used.

RDS trace: normal 13.0 -> POST create; A1.1 -> PUT product/SKU; A1.2 -> POST SKU; A1.3 -> INACTIVE update;
E1 -> 409 identifier conflict; E2 -> 400 publication; E3 -> 400 price/threshold; E4 -> 409 version;
E5 -> transaction rollback + safe 503. BR-05/06 parent/manual attributes; BR-07 database unique;
BR-08/37 whole final retail VND; BR-09 publication; BR-14 approved dynamic warning.
Hosted CI, team merge/deployment and formal RDS/SDS synchronization are not claimed by local verification.
## UI wording and demo evidence, 2026-10-06

The Manager page labels the product master “Sản phẩm” and manually entered sale units
“Phân loại” (API/schema symbols remain SKU). “Có thể bán” displays availableQuantity,
not physical stock: physical minus reserved. Zero displays “Hết hàng”; a positive quantity
with lowStock=true displays “Sắp hết hàng”. Threshold editing still uses minStockThreshold.
ACTIVE/INACTIVE labels are “Đang bán”/“Ngừng bán”; INACTIVE also covers unpublished drafts.
The UI text does not add automatic variant generation, upload, inventory editing or notification delivery.
Demo-only product photos use manufacturer URLs; example prices and opening stock are fixtures.
