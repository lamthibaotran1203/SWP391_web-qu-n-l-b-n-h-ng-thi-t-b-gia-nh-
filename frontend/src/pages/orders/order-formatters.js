export const orderStatusLabels = {
  PENDING_PAYMENT: 'Chờ thanh toán',
  CONFIRMED: 'Đã xác nhận',
  PROCESSING: 'Đang xử lý',
  SHIPPED: 'Đang giao',
  DELIVERED: 'Đã giao',
  CANCELLED: 'Đã hủy',
  COMPLETED: 'Hoàn tất',
}

export const paymentStatusLabels = {
  PENDING: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  FAILED: 'Thanh toán lỗi',
  REFUNDED: 'Đã hoàn tiền',
}

export const channelLabels = {
  ONLINE: 'Trực tuyến',
  COUNTER: 'Tại quầy',
}

export const paymentMethodLabels = {
  COD: 'COD',
  CASH: 'Tiền mặt',
  CARD: 'Thẻ',
  BANK_TRANSFER: 'Chuyển khoản',
  ONLINE_GATEWAY: 'Cổng thanh toán',
}

export function formatLabel(labels, value) {
  return labels[value] || value?.replaceAll('_', ' ') || '—'
}

export function formatCurrency(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(amount)
}

export function formatDate(value) {
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'Asia/Ho_Chi_Minh',
  }).format(new Date(value))
}
