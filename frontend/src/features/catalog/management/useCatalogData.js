import { useEffect, useState } from 'react'
import { getCatalogBrands, getCatalogCategories } from '../api'
import { getManagedProduct } from './api'

export function useManagedProduct(productId) {
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState(null)
  const requestKey = `${productId}:${revision}`
  const current = result?.key === requestKey ? result : null

  useEffect(() => {
    let active = true
    const controller = new AbortController()
    getManagedProduct(productId, { signal: controller.signal })
      .then((product) => { if (active) setResult({ key: requestKey, product }) })
      .catch((error) => { if (active) setResult({ key: requestKey, error }) })
    return () => { active = false; controller.abort() }
  }, [productId, requestKey])

  return { product: current?.product, error: current?.error, loading: !current,
    reload: () => setRevision((value) => value + 1) }
}

export function useCatalogReferences() {
  const [revision, setRevision] = useState(0)
  const [result, setResult] = useState(null)
  const current = result?.revision === revision ? result : null

  useEffect(() => {
    let active = true
    Promise.all([getCatalogCategories(), getCatalogBrands()])
      .then(([categories, brands]) => { if (active) setResult({ revision, categories, brands }) })
      .catch((error) => { if (active) setResult({ revision, error }) })
    return () => { active = false }
  }, [revision])

  return { categories: current?.categories || [], brands: current?.brands || [],
    loading: !current, error: current?.error, reload: () => setRevision((value) => value + 1) }
}
