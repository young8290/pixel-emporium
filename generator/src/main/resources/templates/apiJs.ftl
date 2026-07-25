import http from '../http.js'

const MODULE_PREFIX = '/${module}'

/**
 * 获取${tableNameCn}列表
 * @param {Object} params 查询参数
 * @param {number} params.page 当前页码
 * @param {number} params.size 每页显示条数
 * @returns {Promise}
 */
export function list${Domain}(params = {}) {
  const {page = 1, size = 10, keyword} = params
  return http.get(MODULE_PREFIX + '/${doHyphenMain}/list', {
    params: {
      page,
      size,
      keyword
    }
  })
}

export function save${Domain}(data) {
  return http.post(MODULE_PREFIX + '/${doHyphenMain}/save', data)
}

export function delete${Domain}(id) {
  return http.delete(MODULE_PREFIX + '/${doHyphenMain}/' + id)
}
