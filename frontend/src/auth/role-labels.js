const roleLabels = {
  CUSTOMER: 'Khách hàng',
  STAFF: 'Nhân viên',
  MANAGER: 'Quản lý',
}

export function getRoleLabel(role) {
  return roleLabels[role] || role
}
