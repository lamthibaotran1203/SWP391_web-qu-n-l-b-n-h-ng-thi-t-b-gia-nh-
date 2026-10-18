import { apiRequest } from '../../api/http'

const request = (method, body) => ({ method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })
export const getCart = (options = {}) => apiRequest('/api/v1/cart', options)
export const addCartItem = (item) => apiRequest('/api/v1/cart/items', request('POST', item))
export const updateCartItem = (skuId, quantity) => apiRequest(`/api/v1/cart/items/${skuId}`, request('PUT', { quantity }))
export const removeCartItems = (skuIds) => apiRequest('/api/v1/cart/items', request('DELETE', { skuIds }))
