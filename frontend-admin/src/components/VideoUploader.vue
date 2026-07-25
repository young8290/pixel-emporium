<script setup>
import {ref} from 'vue'
import {Plus, Delete} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import {upload} from '@/api/business/upload.js'

defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue', 'duration-change'])

// 上传和加载状态
const uploadLoading = ref(false)

// 添加上传进度状态
const uploadProgress = ref(0)

// 处理视频加载完成，获取时长
const handleVideoLoad = (event) => {
  const video = event.target
  const duration = Math.round(video.duration)
  emit('duration-change', duration)
}

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
  const isVideo = file.type.startsWith('video/')
  const isLt500M = file.size / 1024 / 1024 < 500

  if (!isVideo) {
    ElMessage.error('只能上传视频格式!')
    return false
  }
  if (!isLt500M) {
    ElMessage.error('视频大小不能超过 500MB!')
    return false
  }
  return true
}

// 自定义上传方法
const customUpload = async (options) => {
  try {
    uploadLoading.value = true
    uploadProgress.value = 0

    const formData = new FormData()
    formData.append('file', options.file)

    const res = await upload(formData, {
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      }
    })

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
    uploadProgress.value = 0
  }
}

// 删除视频
const handleRemove = () => {
  emit('update:modelValue', '')
  ElMessage.success('已清除视频')
}
</script>

<template>
  <el-upload
    class="video-uploader"
    :http-request="customUpload"
    :show-file-list="false"
    :on-success="handleSuccess"
    :before-upload="beforeUpload"
    :on-remove="handleRemove"
  >
    <template v-if="modelValue">
      <div class="video-preview">
        <video
          :src="modelValue"
          class="video"
          controls
          @loadedmetadata="handleVideoLoad"
        />
        <div class="video-actions">
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
        <el-progress
          type="circle"
          :percentage="uploadProgress"
          :status="uploadProgress === 100 ? 'success' : ''"
        />
        <div class="upload-status">{{ uploadProgress === 100 ? '处理中...' : '上传中...' }}</div>
      </div>
    </template>
    <div v-else class="upload-trigger">
      <el-icon class="uploader-icon"><Plus /></el-icon>
      <div class="upload-text">点击上传视频</div>
      <div class="upload-tip">支持 MP4、WebM 等格式，不超过 500MB</div>
    </div>
  </el-upload>
</template>

<style>
.video-uploader .el-upload {
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}

.video-uploader .el-upload:hover {
  border-color: var(--el-color-primary);
}

.upload-trigger {
  width: 360px;
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--el-text-color-secondary);
}

.uploader-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  margin-bottom: 4px;
}

.upload-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.video {
  width: 360px;
  height: 200px;
  display: block;
  object-fit: contain;
  background-color: #000;
}

.video-preview {
  position: relative;
  display: inline-block;
}

.video-preview:hover .video-actions {
  opacity: 1;
}

.video-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-actions .el-button {
  color: white;
  font-size: 20px;
  background-color: rgba(0, 0, 0, 0.5);
  border-radius: 50%;
  padding: 8px;
}

.video-actions .el-button:hover {
  transform: scale(1.1);
  background-color: rgba(0, 0, 0, 0.7);
}

.upload-loading {
  width: 360px;
  height: 200px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--el-text-color-secondary);
}

.upload-status {
  margin-top: 16px;
  font-size: 14px;
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
