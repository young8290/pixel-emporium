import http from './http.js'

const MODULE_PREFIX = '/user'

export function hi() {
  return http.get(MODULE_PREFIX + '/hi')
}
