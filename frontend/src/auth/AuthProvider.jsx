import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  getCurrentUser,
  login as requestLogin,
  logout as requestLogout,
  refreshCsrfToken,
  updateCurrentUser as requestUpdateCurrentUser,
} from '../api/http'
import { AuthContext } from './auth-context'
import { updatePersonalProfile } from '../features/profile/api'

const anonymousUser = {
  authenticated: false,
  email: null,
  fullName: null,
  role: null,
}

async function fetchSession() {
  await refreshCsrfToken()
  return getCurrentUser()
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(anonymousUser)
  const [connection, setConnection] = useState({ status: 'loading', message: '' })

  useEffect(() => {
    let active = true

    fetchSession()
      .then((currentUser) => {
        if (!active) return
        setUser(currentUser)
        setConnection({
          status: 'connected',
          message: 'Kết nối máy chủ ổn định.',
        })
      })
      .catch((error) => {
        if (!active) return
        setUser(anonymousUser)
        setConnection({
          status: 'error',
          message: error.message || 'Không thể kết nối đến máy chủ.',
        })
      })

    return () => {
      active = false
    }
  }, [])

  const reload = useCallback(async () => {
    setConnection({ status: 'loading', message: '' })
    try {
      const currentUser = await fetchSession()
      setUser(currentUser)
      setConnection({
        status: 'connected',
        message: 'Kết nối máy chủ ổn định.',
      })
    } catch (error) {
      setUser(anonymousUser)
      setConnection({
        status: 'error',
        message: error.message || 'Không thể kết nối đến máy chủ.',
      })
    }
  }, [])

  const login = useCallback(async (email, password) => {
    const currentUser = await requestLogin(email, password)
    await refreshCsrfToken()
    setUser(currentUser)
    return currentUser
  }, [])

  const logout = useCallback(async () => {
    await requestLogout()
    await refreshCsrfToken()
    setUser(await getCurrentUser())
  }, [])

  const updateFullName = useCallback(async (fullName) => {
    const currentUser = await requestUpdateCurrentUser(fullName)
    setUser(currentUser)
    return currentUser
  }, [])

  const saveProfile = useCallback(async (fields) => {
    const profile = await updatePersonalProfile(fields)
    setUser((current) => ({ ...current, fullName: profile.fullName }))
    return profile
  }, [])

  const value = useMemo(
    () => ({ user, connection, login, logout, reload, updateFullName, saveProfile }),
    [connection, login, logout, reload, updateFullName, saveProfile, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
