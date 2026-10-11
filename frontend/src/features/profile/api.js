import { apiRequest } from '../../api/http'

const path = '/api/v1/account/profile'

export function getPersonalProfile(options = {}) {
  return apiRequest(path, options)
}

export function updatePersonalProfile({ fullName, phone }) {
  return apiRequest(path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ fullName, phone: phone.trim() || null }),
  })
}
