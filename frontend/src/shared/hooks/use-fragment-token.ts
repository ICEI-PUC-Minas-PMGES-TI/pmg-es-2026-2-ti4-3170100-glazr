import { useState } from 'react'

export function useFragmentToken(): string | null {
  const [token] = useState(() => {
    const value = new URLSearchParams(window.location.hash.slice(1)).get('token')
    window.history.replaceState(window.history.state, '', window.location.pathname + window.location.search)
    return value
  })
  return token
}
