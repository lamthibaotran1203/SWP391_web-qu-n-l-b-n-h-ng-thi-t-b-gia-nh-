import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import * as categoryApi from '../features/categories/api'
import { CategoriesPage } from './CategoriesPage'

vi.mock('../auth/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('../features/categories/api', () => ({
  getCategories: vi.fn(),
  getCategory: vi.fn(),
  getCategoryOptions: vi.fn(),
  createCategory: vi.fn(),
  updateCategory: vi.fn(),
  deactivateCategory: vi.fn(),
}))

const root = {
  id: 1,
  parentId: null,
  parentName: null,
  code: 'KITCHEN',
  name: 'Nhà bếp',
  description: 'Thiết bị và dụng cụ nhà bếp',
  isDeleted: false,
  status: 'ACTIVE',
  createdBy: 'admin@example.invalid',
  createdAt: '2026-10-18T00:00:00Z',
}

function renderPage() {
  return render(<MemoryRouter><CategoriesPage /></MemoryRouter>)
}

describe('CategoriesPage (UC-11 Manage Product Categories)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuth.mockReturnValue({
      user: { authenticated: true, role: 'ADMIN' },
      connection: { status: 'connected' },
      reload: vi.fn(),
    })
    categoryApi.getCategories.mockResolvedValue({
      content: [root], number: 0, totalPages: 1, totalElements: 1,
    })
    categoryApi.getCategoryOptions.mockResolvedValue([
      { id: 2, parentId: null, code: 'HOME', name: 'Gia dụng' },
    ])
  })

  it('shows category list, hierarchy and status', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: 'Quản lý danh mục sản phẩm' })).toBeInTheDocument()
    expect(await screen.findByText('KITCHEN')).toBeInTheDocument()
    const row = screen.getByText('KITCHEN').closest('tr')
    expect(within(row).getByText('Nhà bếp')).toBeInTheDocument()
    expect(within(row).getByText('Hoạt động')).toBeInTheDocument()
  })

  it('creates a category with an optional parent', async () => {
    const user = userEvent.setup()
    categoryApi.createCategory.mockResolvedValue({ id: 3 })
    renderPage()
    await screen.findByText('KITCHEN')

    await user.click(screen.getByRole('button', { name: 'Thêm danh mục' }))
    await user.type(screen.getByLabelText('Mã danh mục *'), 'cookware')
    await user.type(screen.getByLabelText('Tên danh mục *'), 'Nồi và chảo')
    await user.selectOptions(screen.getByLabelText('Danh mục cha'), '2')
    await user.type(screen.getByLabelText('Mô tả'), 'Dụng cụ nấu ăn')
    await user.click(screen.getByRole('button', { name: 'Lưu danh mục' }))

    await waitFor(() => expect(categoryApi.createCategory).toHaveBeenCalledWith({
      code: 'cookware',
      name: 'Nồi và chảo',
      description: 'Dụng cụ nấu ăn',
      parentId: 2,
    }))
  })

  it('updates an existing category and excludes it from parent options', async () => {
    const user = userEvent.setup()
    categoryApi.updateCategory.mockResolvedValue(root)
    renderPage()
    await screen.findByText('KITCHEN')

    await user.click(screen.getByRole('button', { name: 'Sửa' }))
    expect(categoryApi.getCategoryOptions).toHaveBeenCalledWith(1)
    const name = screen.getByLabelText('Tên danh mục *')
    await user.clear(name)
    await user.type(name, 'Không gian bếp')
    await user.click(screen.getByRole('button', { name: 'Lưu danh mục' }))

    await waitFor(() => expect(categoryApi.updateCategory).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ code: 'KITCHEN', name: 'Không gian bếp' }),
    ))
  })

  it('loads details and deactivates without deleting history', async () => {
    const user = userEvent.setup()
    categoryApi.getCategory.mockResolvedValue({ ...root, parentName: 'Gia dụng' })
    categoryApi.deactivateCategory.mockResolvedValue({ ...root, isDeleted: true })
    renderPage()
    await screen.findByText('KITCHEN')

    await user.click(screen.getByRole('button', { name: 'Chi tiết' }))
    expect(await screen.findByText('Gia dụng')).toBeInTheDocument()
    await user.click(within(screen.getByRole('dialog')).getAllByRole('button', { name: 'Đóng' })[1])
    await user.click(screen.getByRole('button', { name: 'Ngừng' }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận ngừng' }))

    await waitFor(() => expect(categoryApi.deactivateCategory).toHaveBeenCalledWith(1))
    expect(await screen.findByText('Đã ngừng hoạt động danh mục.')).toBeInTheDocument()
  })

  it('blocks non-Admin users', () => {
    useAuth.mockReturnValue({
      user: { authenticated: true, role: 'STAFF' },
      connection: { status: 'connected' },
      reload: vi.fn(),
    })
    renderPage()

    expect(screen.getByRole('heading', { name: 'Không có quyền quản lý danh mục' })).toBeInTheDocument()
    expect(categoryApi.getCategories).not.toHaveBeenCalled()
  })
})
