<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteImage,
  listImages,
  setMainImage,
  styleStatus,
  stylizeImage,
  uploadImage,
} from '@/api/business/image'

const open = ref(false)
const loading = ref(false)
const product = ref(null)
const images = ref([])
const pollingIds = reactive(new Set())

async function refresh() {
  if (!product.value?.id) return
  loading.value = true
  try {
    images.value = await listImages(product.value.id)
  } finally {
    loading.value = false
  }
}

async function show(row) {
  product.value = row
  open.value = true
  await refresh()
}

async function upload(file) {
  if (!file?.raw) return
  const data = new FormData()
  data.append('productId', product.value.id)
  data.append('file', file.raw)
  await uploadImage(data)
  ElMessage.success('图片上传成功')
  await refresh()
}

async function remove(image) {
  await ElMessageBox.confirm('确定删除这张商品图？', '危险操作', { type: 'warning' })
  await deleteImage(image.id)
  await refresh()
}

async function makeMain(image) {
  await setMainImage(image.id)
  ElMessage.success('主图已更新')
  await refresh()
}

async function stylize(image, presetKey) {
  pollingIds.add(image.id)
  try {
    await stylizeImage(image.id, presetKey)
    for (let attempt = 0; attempt < 20; attempt += 1) {
      await new Promise((resolve) => window.setTimeout(resolve, 1500))
      const state = await styleStatus(image.id)
      if (state?.styleStatus !== 'PENDING') {
        if (state?.styleStatus === 'DONE') ElMessage.success('像素风转换完成')
        if (state?.styleStatus === 'FAILED') ElMessage.error('转换失败，可重新选择预设重试')
        break
      }
    }
    await refresh()
  } finally {
    pollingIds.delete(image.id)
  }
}

defineExpose({ show })
</script>

<template>
  <el-dialog v-model="open" :title="`${product?.title || ''} · 商品多图与像素化`" width="820px">
    <el-upload
      :auto-upload="false"
      :show-file-list="false"
      accept="image/*"
      @change="upload"
    >
      <el-button type="primary">上传图片</el-button>
    </el-upload>

    <div v-loading="loading" class="image-grid">
      <article v-for="image in images" :key="image.id">
        <div class="preview">
          <img :src="image.url || image.originalUrl" alt="商品图" />
          <div v-if="pollingIds.has(image.id) || image.styleStatus === 'PENDING'" class="pending">
            <span class="pixel-spinner" aria-hidden="true" />
            <span>转换中</span>
          </div>
          <el-tag v-if="image.isMain" class="main-tag">主图</el-tag>
          <el-tag
            v-if="image.styleStatus === 'FAILED'"
            class="failed-tag"
            type="danger"
          >
            转换失败
          </el-tag>
        </div>

        <details v-if="image.styledUrl" class="compare">
          <summary>原图 / 像素图对比</summary>
          <div>
            <img :src="image.originalUrl" alt="原图" />
            <img :src="image.styledUrl" alt="像素图" />
          </div>
        </details>

        <footer>
          <el-button link :disabled="image.isMain" @click="makeMain(image)">设主图</el-button>
          <el-dropdown
            @command="stylize(image, $event)"
          >
            <el-button link :loading="pollingIds.has(image.id)">转像素风</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="stardew-soft">柔和</el-dropdown-item>
                <el-dropdown-item command="stardew-strong">强像素</el-dropdown-item>
                <el-dropdown-item command="lineart">线稿</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button link type="danger" @click="remove(image)">删除</el-button>
        </footer>
      </article>
      <el-empty v-if="!loading && !images.length" description="尚未上传商品图" />
    </div>
  </el-dialog>
</template>

<style scoped>
.image-grid {
  min-height: 180px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--sp-3);
  margin-top: var(--sp-3);
}

article {
  padding: var(--sp-2);
  border: 2px solid var(--c-wood);
  background: var(--c-surface-2);
}

.preview {
  position: relative;
}

.preview > img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  image-rendering: pixelated;
}

.main-tag,
.failed-tag {
  position: absolute;
  top: var(--sp-2);
}

.main-tag { left: var(--sp-2); }
.failed-tag { right: var(--sp-2); }

.pending {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: var(--sp-2);
  color: var(--c-surface);
  background: color-mix(in srgb, var(--c-ink) 72%, transparent);
}

.pixel-spinner {
  width: 24px;
  height: 24px;
  border: 4px solid var(--c-line);
  border-top-color: var(--c-amber);
  animation: spin var(--dur-3) steps(8) infinite;
}

.compare summary {
  padding-block: var(--sp-2);
  color: var(--c-moss-dark);
  cursor: pointer;
}

.compare div {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--sp-1);
}

.compare img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
}

footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
}

@keyframes spin {
  to { transform: rotate(1turn); }
}

@media (max-width: 700px) {
  .image-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}

@media (prefers-reduced-motion: reduce) {
  .pixel-spinner { animation: none; }
}
</style>
