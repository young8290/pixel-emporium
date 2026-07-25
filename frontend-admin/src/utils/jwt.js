export function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(decodeURIComponent(atob(payload).split('').map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`).join('')))
  } catch {
    return {}
  }
}
