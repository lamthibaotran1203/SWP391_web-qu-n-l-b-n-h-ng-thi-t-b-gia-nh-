import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import * as brandApi from '../features/brands/api'
import { BrandsPage } from './BrandsPage'

vi.mock('../auth/useAuth', () => ({
  useAuth: vi.fn(),
}))

vi.mock('../features/brands/api', () => ({
  getBrands: vi.fn(),
  getBrandById: vi.fn(),
  createBrand: vi.fn(),
  updateBrand: vi.fn(),
  deactivateBrand: vi.fn(),
}))

const mockBrands = [
  {
    id: 1,
    name: 'Sunhouse',
    country: 'Vietnam',
    description: 'Thương hiệu gia dụng Việt Nam',
    logoUrl: 'https://example.com/sunhouse.png',
    isDeleted: false,
    status: 'ACTIVE',
    createdBy: 'manager@homemart.vn',
    createdAt: '2026-10-01T00:00:00Z',
  },
  {
    id: 2,
    name: 'Lock&Lock',
    country: 'Korea',
    description: 'Thương hiệu hộp bảo quản',
    logoUrl: null,
    isDeleted: true,
    status: 'INACTIVE',
    createdBy: 'manager@homemart.vn',
    createdAt: '2026-10-02T00:00:00Z',
  },
]

describe('BrandsPage (UC-12 Manage Brands)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useAuth.mockReturnValue({
      user: { authenticated: true, role: 'ADMIN', fullName: 'Admin' },
      connection: { status: 'connected' },
      reload: vi.fn(),
    })
    brandApi.getBrands.mockResolvedValue({
      content: mockBrands,
      number: 0,
      totalPages: 1,
      totalElements: 2,
    })
  })

  // 1. Brand list displays API data
  it('renders brand list, table headers and actions', async () => {
    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { level: 1, name: 'Quản lý thương hiệu' })).toBeInTheDocument()
    expect(await screen.findByText('Sunhouse')).toBeInTheDocument()
    expect(screen.getByText('Vietnam')).toBeInTheDocument()
    expect(screen.getByText('Lock&Lock')).toBeInTheDocument()
    expect(screen.getByText('Korea')).toBeInTheDocument()

    // Assert status badge within the specific brand table rows
    const sunhouseRow = screen.getByText('Sunhouse').closest('tr')
    expect(within(sunhouseRow).getByText('Hoạt động')).toBeInTheDocument()

    const lockLockRow = screen.getByText('Lock&Lock').closest('tr')
    expect(within(lockLockRow).getByText('Ngừng hoạt động')).toBeInTheDocument()
  })

  // 2. Search and status filters
  it('filters brands by keyword and status', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Sunhouse')).toBeInTheDocument()

    const searchInput = screen.getByPlaceholderText('Nhập tên thương hiệu hoặc quốc gia...')
    await user.type(searchInput, 'Sunhouse')

    const statusSelect = screen.getByLabelText(/Trạng thái/i)
    await user.selectOptions(statusSelect, 'ACTIVE')

    const searchBtn = screen.getByRole('button', { name: 'Tìm kiếm' })
    await user.click(searchBtn)

    await waitFor(() => {
      expect(brandApi.getBrands).toHaveBeenCalledWith(
        expect.objectContaining({
          keyword: 'Sunhouse',
          status: 'ACTIVE',
          page: 0,
        }),
      )
    })
  })

  // 3. Pagination and page reset
  it('handles pagination navigation and resets to page 0 on filter change', async () => {
    const user = userEvent.setup()
    brandApi.getBrands.mockResolvedValue({
      content: [mockBrands[0]],
      number: 0,
      totalPages: 2,
      totalElements: 12,
    })

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Trang 1 / 2')).toBeInTheDocument()

    const nextBtn = screen.getByRole('button', { name: 'Sau' })
    await user.click(nextBtn)

    await waitFor(() => {
      expect(brandApi.getBrands).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 1,
        }),
      )
    })

    // Reset button resets to page 0
    const resetBtn = screen.getByRole('button', { name: 'Đặt lại' })
    await user.click(resetBtn)

    await waitFor(() => {
      expect(brandApi.getBrands).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 0,
          keyword: '',
          status: '',
        }),
      )
    })
  })

  // 4. Create valid brand (UC-12.1)
  it('opens modal and creates brand successfully', async () => {
    const user = userEvent.setup()
    brandApi.createBrand.mockResolvedValue({
      id: 3,
      name: 'Philips',
      country: 'Netherlands',
      description: 'Hàng gia dụng châu Âu',
      logoUrl: null,
      isDeleted: false,
      status: 'ACTIVE',
    })

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    const addBtn = screen.getByRole('button', { name: 'Thêm thương hiệu' })
    await user.click(addBtn)

    expect(await screen.findByText('Thêm thương hiệu mới')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/Tên thương hiệu/i)
    const countryInput = screen.getByLabelText(/Quốc gia/i)
    await user.type(nameInput, 'Philips')
    await user.type(countryInput, 'Netherlands')

    const submitBtn = screen.getByRole('button', { name: 'Tạo mới' })
    await user.click(submitBtn)

    await waitFor(() => {
      expect(brandApi.createBrand).toHaveBeenCalledWith({
        name: 'Philips',
        country: 'Netherlands',
        description: null,
        logoUrl: null,
      })
    })

    expect(await screen.findByText(/Đã thêm thương hiệu "Philips" thành công!/i)).toBeInTheDocument()
  })

  // 5. Duplicate name error (409)
  it('displays error on duplicate brand name', async () => {
    const user = userEvent.setup()
    brandApi.createBrand.mockRejectedValue({
      code: 'BRAND_DUPLICATE',
      message: 'Tên thương hiệu đã tồn tại: Sunhouse',
    })

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    const addBtn = screen.getByRole('button', { name: 'Thêm thương hiệu' })
    await user.click(addBtn)

    const nameInput = screen.getByLabelText(/Tên thương hiệu/i)
    await user.type(nameInput, 'Sunhouse')

    const submitBtn = screen.getByRole('button', { name: 'Tạo mới' })
    await user.click(submitBtn)

    expect(await screen.findByText('Tên thương hiệu đã tồn tại: Sunhouse')).toBeInTheDocument()
  })

  // 6. Update brand (UC-12.3)
  it('opens edit modal and updates brand successfully', async () => {
    const user = userEvent.setup()
    brandApi.updateBrand.mockResolvedValue({
      id: 1,
      name: 'Sunhouse Group',
      country: 'Vietnam',
      description: 'Thương hiệu gia dụng Việt Nam',
      logoUrl: 'https://example.com/sunhouse.png',
      isDeleted: false,
    })

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Sunhouse')).toBeInTheDocument()

    const editBtns = screen.getAllByRole('button', { name: 'Sửa' })
    await user.click(editBtns[0])

    expect(await screen.findByText('Cập nhật thương hiệu')).toBeInTheDocument()

    const nameInput = screen.getByLabelText(/Tên thương hiệu/i)
    await user.clear(nameInput)
    await user.type(nameInput, 'Sunhouse Group')

    const saveBtn = screen.getByRole('button', { name: 'Lưu thay đổi' })
    await user.click(saveBtn)

    await waitFor(() => {
      expect(brandApi.updateBrand).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
          name: 'Sunhouse Group',
        }),
      )
    })

    expect(await screen.findByText(/Đã cập nhật thương hiệu "Sunhouse Group" thành công!/i)).toBeInTheDocument()
  })

  // 7. Deactivate brand (UC-12.4)
  it('opens confirmation modal and deactivates brand', async () => {
    const user = userEvent.setup()
    brandApi.deactivateBrand.mockResolvedValue({
      id: 1,
      name: 'Sunhouse',
      isDeleted: true,
      status: 'INACTIVE',
    })

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Sunhouse')).toBeInTheDocument()

    const deactivateBtn = screen.getByRole('button', { name: 'Vô hiệu hóa' })
    await user.click(deactivateBtn)

    expect(await screen.findByRole('heading', { name: 'Xác nhận vô hiệu hóa' })).toBeInTheDocument()
    expect(screen.getByText(/Bạn có chắc chắn muốn vô hiệu hóa thương hiệu/i)).toBeInTheDocument()

    const confirmBtn = screen.getByRole('button', { name: 'Xác nhận vô hiệu hóa' })
    await user.click(confirmBtn)

    await waitFor(() => {
      expect(brandApi.deactivateBrand).toHaveBeenCalledWith(1)
    })

    expect(await screen.findByText(/Đã vô hiệu hóa thương hiệu "Sunhouse" thành công!/i)).toBeInTheDocument()
  })

  // 8. BRAND_HAS_ACTIVE_PRODUCTS error (409)
  it('handles BRAND_HAS_ACTIVE_PRODUCTS conflict gracefully', async () => {
    const user = userEvent.setup()
    brandApi.deactivateBrand.mockRejectedValue({
      code: 'BRAND_HAS_ACTIVE_PRODUCTS',
      message: 'Không thể vô hiệu hóa thương hiệu vì vẫn còn 3 sản phẩm đang hoạt động.',
    })

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Sunhouse')).toBeInTheDocument()

    const deactivateBtn = screen.getByRole('button', { name: 'Vô hiệu hóa' })
    await user.click(deactivateBtn)

    const confirmBtn = screen.getByRole('button', { name: 'Xác nhận vô hiệu hóa' })
    await user.click(confirmBtn)

    expect(
      await screen.findByText('Không thể vô hiệu hóa thương hiệu vì vẫn còn 3 sản phẩm đang hoạt động.'),
    ).toBeInTheDocument()
  })

  // 9. Form validation errors
  it('validates empty and whitespace-only name on client side', async () => {
    const user = userEvent.setup()

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    const addBtn = screen.getByRole('button', { name: 'Thêm thương hiệu' })
    await user.click(addBtn)

    const submitBtn = screen.getByRole('button', { name: 'Tạo mới' })
    await user.click(submitBtn)

    expect(await screen.findByText('Tên thương hiệu không được để trống')).toBeInTheDocument()
    expect(brandApi.createBrand).not.toHaveBeenCalled()

    // Whitespace-only name is rejected
    const nameInput = screen.getByLabelText(/Tên thương hiệu/i)
    await user.type(nameInput, '     ')
    await user.click(submitBtn)

    expect(await screen.findByText('Tên thương hiệu không được để trống')).toBeInTheDocument()
    expect(brandApi.createBrand).not.toHaveBeenCalled()
  })

  // 10. Unauthorized access
  it('blocks non-ADMIN users from accessing the page', () => {
    useAuth.mockReturnValue({
      user: { authenticated: true, role: 'CUSTOMER' },
      connection: { status: 'connected' },
      reload: vi.fn(),
    })

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('Không có quyền quản lý thương hiệu')).toBeInTheDocument()
    expect(screen.queryByText('Thêm thương hiệu')).not.toBeInTheDocument()
  })

  // 11. Loading and empty states
  it('displays empty state when no brands are returned', async () => {
    brandApi.getBrands.mockResolvedValue({
      content: [],
      number: 0,
      totalPages: 0,
      totalElements: 0,
    })

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Không tìm thấy thương hiệu nào.')).toBeInTheDocument()
  })

  // 12. API/network failures
  it('displays error alert when API fetch fails', async () => {
    brandApi.getBrands.mockRejectedValue(new Error('Máy chủ đang bận, vui lòng thử lại sau.'))

    render(
      <MemoryRouter>
        <BrandsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Máy chủ đang bận, vui lòng thử lại sau.')).toBeInTheDocument()
  })
})
