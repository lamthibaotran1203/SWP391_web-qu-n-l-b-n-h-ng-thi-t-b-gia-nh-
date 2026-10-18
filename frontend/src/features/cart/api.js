import { apiRequest } from '../../api/http'

const request = (method, body) => ({ method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
export const getCart = (options = {}) => apiRequest('/api/v1/cart', options)
const pendingAdds = new Map()

export async function addCartItem(item) {
  const payloadKey = JSON.stringify([item.skuId, item.quantity])
  let requestId = pendingAdds.get(payloadKey)
  if (!requestId) {
    requestId = crypto.randomUUID()
    pendingAdds.set(payloadKey, requestId)
  }
  const options = request('POST', item)
  options.headers['Idempotency-Key'] = requestId
  try {
    const cart = await apiRequest('/api/v1/cart/items', options)
    if (pendingAdds.get(payloadKey) === requestId) pendingAdds.delete(payloadKey)
    return cart
  } catch (failure) {
    // Mất kết nối/response: giữ cùng mã để lần thử lại không cộng thêm lần nữa.
    if (failure.status >= 400 && failure.status < 500 && pendingAdds.get(payloadKey) === requestId) pendingAdds.delete(payloadKey)
    throw failure
  }
}

export const updateCartItem = (skuId, quantity, expectedVersion) => apiRequest(`/api/v1/cart/items/${skuId}`, request('PUT', { quantity, expectedVersion }))
export const removeCartItems = (skuIds) => apiRequest('/api/v1/cart/items', request('DELETE', { skuIds }))
