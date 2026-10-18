import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { getPublicCategories } from '../features/catalog/api'
import { ProductCategoriesPage } from './ProductCategoriesPage'
vi.mock('../features/catalog/api', () => ({ getPublicCategories: vi.fn() }))
beforeEach(() => vi.resetAllMocks())
afterEach(cleanup)
function show() { render(<MemoryRouter><ProductCategoriesPage /></MemoryRouter>) }
it('loads public roots and expands their children, linking to search by category ID', async () => {
  getPublicCategories.mockResolvedValueOnce([{ id: 1, name: 'Nhà bếp', hasChildren: true }]).mockResolvedValueOnce([{ id: 2, parentId: 1, name: 'Nồi', hasChildren: false }])
  const user = userEvent.setup(); show()
  const expand = await screen.findByRole('button', { name: 'Mở rộng Nhà bếp' })
  expect(getPublicCategories.mock.calls[0][0]).toEqual({ roots: true })
  await user.click(expand)
  expect(await screen.findByRole('link', { name: 'Nồi' })).toHaveAttribute('href', '/products?categoryId=2')
  expect(getPublicCategories.mock.calls[1][0]).toEqual({ parentId: 1 })
  expect(screen.getByRole('button', { name: 'Thu gọn Nhà bếp' })).toHaveAttribute('aria-expanded', 'true')
})
it('shows a dedicated empty category message', async () => {
  getPublicCategories.mockResolvedValue([]); show()
  expect(await screen.findByText('Chưa có danh mục công khai')).toBeInTheDocument()
})
it('retries category loading errors without displaying a false empty state', async () => {
  getPublicCategories.mockRejectedValueOnce(new Error('Lỗi tải danh mục')).mockResolvedValueOnce([{ id: 1, name: 'Nhà bếp', hasChildren: false }])
  const user = userEvent.setup(); show()
  expect(await screen.findByRole('alert')).toHaveTextContent('Lỗi tải danh mục')
  expect(screen.queryByText('Chưa có danh mục công khai')).not.toBeInTheDocument()
  await user.click(screen.getByRole('button', { name: 'Thử lại' }))
  expect(await screen.findByRole('link', { name: 'Nhà bếp' })).toBeInTheDocument()
})
it('allows retrying a failed child load', async () => {
  getPublicCategories.mockResolvedValueOnce([{ id: 1, name: 'Nhà bếp', hasChildren: true }]).mockRejectedValueOnce(new Error('Lỗi danh mục con')).mockResolvedValueOnce([])
  const user = userEvent.setup(); show()
  await user.click(await screen.findByRole('button', { name: 'Mở rộng Nhà bếp' }))
  await screen.findByRole('alert')
  await user.click(screen.getByRole('button', { name: 'Thử lại' }))
  expect(await screen.findByText('Chưa có danh mục con công khai.')).toBeInTheDocument()
})
