import { ElMessage } from 'element-plus'

export const PixelToast = {
  success: (message) => ElMessage.success({ message, duration: 2200 }),
  warning: (message) => ElMessage.warning({ message, duration: 2800 }),
  error: (message) => ElMessage.error({ message, duration: 3200 }),
  info: (message) => ElMessage.info({ message, duration: 2400 })
}

export default PixelToast
