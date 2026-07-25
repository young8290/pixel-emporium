<script setup>
import {ref} from 'vue'
import {Plus, ZoomIn, Delete, Loading} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import {upload} from '@/api/business/upload.js'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])

// 上传和加载状态
const uploadLoading = ref(false)
const imageLoading = ref(false)

// 预览对话框
const dialogImageUrl = ref('')
const dialogImageVisible = ref(false)

// 处理上传成功
const handleSuccess = (res) => {
  if (res?.code === 200) {
    emit('update:modelValue', res.data)
    ElMessage.success('上传成功')
  } else if (res) {
    ElMessage.error(res.msg || '上传失败')
  }
}

// 上传前验证
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片格式!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
    return false
  }
  return true
}

// 自定义上传方法
const customUpload = async (options) => {
  try {
    uploadLoading.value = true
    const formData = new FormData()
    formData.append('file', options.file)
    const res = await upload(formData)

    if (res?.code === 200) {
      emit('update:modelValue', res.data)
    } else {
      ElMessage.error(res.msg || '上传失败')
    }
  } catch (error) {
    console.error('文件上传错误:', error)
    ElMessage.error(error.message || '上传出错')
    options.onError(error)
  } finally {
    uploadLoading.value = false
  }
}

// 处理图片加载
const handleImageLoad = () => {
  imageLoading.value = false
}

// 图片预览
const handlePreview = () => {
  if (props.modelValue) {
    imageLoading.value = true
    dialogImageUrl.value = props.modelValue
    dialogImageVisible.value = true
  }
}

// 删除图片
const handleRemove = () => {
  emit('update:modelValue', '')
  ElMessage.success('已清除图片')
}
</script>

<template>
  <el-upload
    class="image-uploader"
    :http-request="customUpload"
    :show-file-list="false"
    :on-success="handleSuccess"
    :before-upload="beforeUpload"
    :on-remove="handleRemove"
  >
    <template v-if="modelValue">
      <div class="image-preview">
        <el-image
          :src="modelValue"
          class="image"
          @load="handleImageLoad"
        >
          <template #placeholder>
            <div class="image-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <div>加载中...</div>
            </div>
          </template>
        </el-image>
        <div class="image-actions">
          <el-button
            type="primary"
            link
            @click.stop="handlePreview"
          >
            <el-icon><ZoomIn /></el-icon>
          </el-button>
          <el-button
            type="danger"
            link
            @click.stop="handleRemove"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
    </template>
    <template v-else-if="uploadLoading">
      <div class="upload-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <div>上传中...</div>
      </div>
    </template>
    <el-icon v-else class="uploader-icon"><Plus /></el-icon>
  </el-upload>

  <!-- 图片预览对话框 -->
  <el-dialog
    v-model="dialogImageVisible"
    title="预览"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
  >
    <el-image
      :src="dialogImageUrl"
      style="width: 100%"
      @load="handleImageLoad"
    >
      <template #placeholder>
        <div class="image-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <div>加载中...</div>
        </div>
      </template>
    </el-image>
  </el-dialog>
</template>

<style>
.image-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.image-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  text-align: center;
  display: flex;
  justify-content: center;
  align-items: center;
}

.image {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.image-preview {
  position: relative;
  display: inline-block;
}

.image-preview:hover .image-actions {
  opacity: 1;
}

.image-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  background-color: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.3s;
}

.image-actions .el-button {
  color: white;
  font-size: 20px;
}

.image-actions .el-button:hover {
  transform: scale(1.1);
}

.upload-loading,
.image-loading {
  width: 178px;
  height: 178px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--el-text-color-secondary);
}

.upload-loading .el-icon,
.image-loading .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

@keyframes rotating {
  0% {
    transform: rotate(0);
  }
  100% {
    transform: rotate(360deg);
  }
}

.is-loading {
  animation: rotating 2s linear infinite;
}
</style>
