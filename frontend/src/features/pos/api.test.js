import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiRequest } from '../../api/http'
import { getPosCatalog, processPosTransaction } from './api'

vi.mock('../../api/http', () => ({
  apiRequest: vi.fn(),
}))

describe('POS API', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    apiRequest.mockResolvedValue({})
  })

  it('loads paginated live catalog results using the search term', async () => {
    const options = { signal: new AbortController().signal }

    await getPosCatalog({ search: '  ấm  ', page: 2, size: 12 }, options)

    expect(apiRequest).toHaveBeenCalledWith(
      '/api/v1/catalog/skus?page=2&size=12&search=%E1%BA%A5m',
      options,
    )
  })

  it('posts the payment and sale lines with a stable idempotency key', async () => {
    const items = [{ skuId: 71, quantity: 2 }]

    await processPosTransaction(items, 'CASH', 'transaction-key')

    expect(apiRequest).toHaveBeenCalledWith('/api/v1/staff/pos/transactions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': 'transaction-key',
      },
      body: JSON.stringify({ items, paymentMethod: 'CASH' }),
    })
  })
})
