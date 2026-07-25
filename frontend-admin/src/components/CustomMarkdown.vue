<script setup>
import {onMounted, onUnmounted, watch} from 'vue'
import {MODULE_PREFIX} from '@/api/business/upload.js'

import Vditor from 'vditor'
import "vditor/dist/index.css"

const UPLOAD_FROM_VDITOR = '/admin/file/uploadFromVditor'
const UPLOAD_OUTSIDE_LINK_FROM_VDITOR = '/admin/file/uploadOutsideLinkFromVditor'

const props = defineProps({
  markdownText: {
    type: String,
    default: ''
  },
  height: {
    type: String,
    default: '400px'
  },
  id: {
    type: String,
    required: true
  },
  defaultModel: {
    type: String,
    // 'edit', 'previewOnly', or 'edit&preview' (来自 Cherry Markdown 的方式)
    default: 'edit'
  }
})

// 编辑器实例
let vditorInstance;

onMounted(() => {
  initEditor()
})

onUnmounted(() => {
  if (vditorInstance) {
    vditorInstance.destroy()
  }
})

// 初始化编辑器
const initEditor = () => {
  if (vditorInstance) {
    vditorInstance.destroy()
  }

  if ('previewOnly' !== props.defaultModel) {
    vditorInstance = new Vditor(props.id,{
      cdn: '/vendor/vditor',
      mode: 'ir',
      height: props.height,
      preview: {
        actions: ["desktop", /*"tablet",*/ "mobile", /*"mp-wechat", "zhihu"*/],
      },
      upload: {
        fieldName: 'file[]',
        max: 10485760, // 上传文件最大 10 * 1024 * 1024 (Byte)
        url: import.meta.env.VITE_API_BASE_URL + MODULE_PREFIX + UPLOAD_FROM_VDITOR,
        linkToImgUrl: import.meta.env.VITE_API_BASE_URL + MODULE_PREFIX + UPLOAD_OUTSIDE_LINK_FROM_VDITOR,
        headers: {
          'accept': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('adminToken')}`
        }
      },
      counter: {
        enable: false,
      },
      cache: {
        enable: false,
      },
      after: () => {
        vditorInstance.setValue(props?.markdownText || '')
      },
      input: (text) => {
        emit('update:markdownText', text)
      },
      toolbar: [
        "emoji",
        "headings",
        "bold",
        "italic",
        "strike",
        "link",
        "|",
        "list",
        "ordered-list",
        "check",
        "outdent",
        "indent",
        "|",
        "quote",
        "line",
        "code",
        "inline-code",
        "insert-before",
        "insert-after",
        "|",
        "upload",
        //"record",
        "table",
        "|",
        "undo",
        "redo",
        "|",
        "fullscreen",
        "edit-mode",
        {
          name: "more",
          toolbar: [
            "both",
            //"code-theme",
            //"content-theme",
            "export",
            "outline",
            "preview",
            "devtools",
            "info",
            "help",
          ],
        }
      ],
    })
  }
}

// 监听外部值变化
watch(() => props.markdownText, (newVal) => {
  if (vditorInstance && newVal !== vditorInstance.getValue()) {
    vditorInstance.setValue(newVal || '')
  }
})

const emit = defineEmits(['update:markdownText'])

// 暴露方法给父组件
defineExpose({
  getValue: () => vditorInstance?.getValue() || '',
  setValue: (value) => vditorInstance?.setValue(value || ''),
  doPreview: () => {
    Vditor.preview(document.getElementById(props.id), props.markdownText, {
      cdn: '/vendor/vditor',
    });
  }
})

</script>

<template>
  <div :id="id"></div>
</template>

<style scoped>

</style>
