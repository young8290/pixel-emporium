import http from '../http.js'

const MODULE_PREFIX = '/user'

/**
 * 获取收货地址列表
 * @param {number} params.page 当前页码
 * @param {number} params.size 每页显示条数
 * @param {Object} params 查询参数
 * @returns {Promise}
 */
export function listAddress(params = {}) {
  const {page = 1, size = 10, keyword} = params
  return http.get(MODULE_PREFIX + '/address/list', {
    params: {
      page,
      size,
      keyword
    }
  })
}

export function saveAddress(data) {
  return http.post(MODULE_PREFIX + '/address/save', data)
}

export function deleteAddress(id) {
  return http.delete(MODULE_PREFIX + '/address/' + id)
}
