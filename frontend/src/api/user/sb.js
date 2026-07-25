import http from '../http.js'

const MODULE_PREFIX = '/user'

/**
 * 获取 SB 列表
 * @param {number} params.page 当前页码
 * @param {number} params.size 每页显示条数
 * @returns {Promise}
 */
export function listSb(params = {}) {
  const { page = 1, size = 10 } = params

  return http.get(MODULE_PREFIX + '/sb/list', {
    params: {
      page,
      size
    }
  })
}
