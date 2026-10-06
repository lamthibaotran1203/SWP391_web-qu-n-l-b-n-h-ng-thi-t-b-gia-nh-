export function productForm(product) {
  return { name: product?.name || '', description: product?.description || '', categoryId: product?.categoryId || '',
    brandId: product?.brandId || '', imageUrl: product?.imageUrl || '', status: product?.status || 'INACTIVE' }
}
export function skuForm(sku) {
  return { skuCode: sku?.skuCode || '', barcode: sku?.barcode || '', skuName: sku?.skuName || '',
    originalPrice: sku?.originalPrice || '0', sellingPrice: sku?.sellingPrice || '0', minStockThreshold: sku?.minStockThreshold ?? 0,
    imageUrl: sku?.imageUrl || '', status: sku?.status || 'INACTIVE',
    attributeRows: Object.entries(sku?.attributes || {}).map(([key, value]) => ({ key, value })) }
}
export function productPayload(form) {
  return { ...form, categoryId: form.categoryId ? Number(form.categoryId) : null, brandId: form.brandId ? Number(form.brandId) : null }
}
export function skuPayload(form) {
  const { attributeRows, ...fields } = form
  const attributes = Object.create(null)
  for (const row of attributeRows) {
    const key = row.key.trim()
    if (!key || !row.value.trim() || Object.hasOwn(attributes, key)) throw new Error('Nhập đủ tên và giá trị thông số; tên thông số không được trùng.')
    attributes[key] = row.value.trim()
  }
  // Money remains decimal strings; only the bounded integer threshold becomes a Number.
  return { ...fields, minStockThreshold: Number(form.minStockThreshold), attributes }
}
