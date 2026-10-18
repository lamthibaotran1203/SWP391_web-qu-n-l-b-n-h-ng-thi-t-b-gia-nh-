const roleLabels = {
  CUSTOMER: 'Khách hàng',
  STAFF: 'Nhân viên',
  ADMIN: 'Quản trị viên',
}

export function getRoleLabel(role) {
  return roleLabels[role] || role
}
