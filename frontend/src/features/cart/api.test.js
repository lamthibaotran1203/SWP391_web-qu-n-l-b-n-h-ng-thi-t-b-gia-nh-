import { beforeEach, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/http'

vi.mock('../../api/http', () => ({ apiRequest: vi.fn() }))
beforeEach(() => { vi.resetAllMocks(); vi.resetModules() })

it('reuses the request id after a lost response but creates a new id for the next accepted add', async () => {
  const { addCartItem } = await import('./api')
  apiRequest.mockRejectedValueOnce(new Error('Lost response')).mockResolvedValue({ items: [] })
  const item = { skuId: 12, quantity: 2 }
  await expect(addCartItem(item)).rejects.toThrow('Lost response')
  const firstId = apiRequest.mock.calls[0][1].headers['Idempotency-Key']
  await addCartItem(item)
  expect(apiRequest.mock.calls[1][1].headers['Idempotency-Key']).toBe(firstId)
  await addCartItem(item)
  expect(apiRequest.mock.calls[2][1].headers['Idempotency-Key']).not.toBe(firstId)
})

it('sends the displayed revision with a replacement quantity', async () => {
  const { updateCartItem } = await import('./api')
  apiRequest.mockResolvedValue({ items: [] })
  await updateCartItem(12, 3, 'revision-1')
  expect(apiRequest).toHaveBeenCalledWith('/api/v1/cart/items/12', expect.objectContaining({
    method: 'PUT', body: JSON.stringify({ quantity: 3, expectedVersion: 'revision-1' }),
  }))
})
