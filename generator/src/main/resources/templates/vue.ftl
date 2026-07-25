<script setup>
import {ref, reactive, onMounted<#if !readOnly>, computed</#if><#if hasMarkdownColumn>, useTemplateRef, nextTick</#if>} from 'vue'
import {<#if !readOnly>Plus, </#if>Refresh<#if hasImageColumn>, ZoomIn</#if><#if hasVideoColumn>, VideoPlay</#if>} from '@element-plus/icons-vue'
import {ElMessage<#if !readOnly>, ElMessageBox</#if>} from 'element-plus'
import {<#if !readOnly>save${Domain}, delete${Domain}, </#if>list${Domain}} from '@/api/${module}/${domain}.js'
<#if hasImageColumn>
import ImageUploader from '@/components/ImageUploader.vue'
</#if>
<#if hasVideoColumn>
import VideoUploader from '@/components/VideoUploader.vue'
</#if>
<#if hasMarkdownColumn>
import CustomMarkdown from '@/components/CustomMarkdown.vue'
</#if>
<#if hasRefColumn>
<#list refFields as refField>
<#assign RefDomain = refField.refTable?replace('_', ' ')?split(' ')?map(w -> w?cap_first)?join('')>
import ${RefDomain}Select from '@/components/${RefDomain}Select.vue'
</#list>
</#if>
<#assign ignoreColumns = ["createdAt", "createdBy", "updatedAt", "updatedBy", "deleted", "deletedAt", "deletedBy"]>

// ${tableNameCn}列表数据
const ${domain}List = ref([])
<#if !readOnly>

// 控制对话框的显示
const dialogVisible = ref(false)

<#if hasMarkdownColumn>
// 控制 markdown 内容预览对话框的显示
const markdownPreviewVisible = ref(false)
const markdownPreviewText = ref('')
</#if>

// ${tableNameCn}表单数据
const ${domain}Form = reactive({
<#list fieldList as field>
  <#if !ignoreColumns?seq_contains(field.nameLowerCamelCase)>
    <#if field.nameLowerCamelCase == 'id'>
  ${field.nameLowerCamelCase}: null,
    <#elseif (field.refTable)?has_content>
  ${field.nameLowerCamelCase}: null,
    <#elseif field.type?contains('int')>
  ${field.nameLowerCamelCase}: <#if field.defaultValue?has_content>${field.defaultValue}<#else>0</#if>,
    <#elseif field.javaType == 'BigDecimal'>
  ${field.nameLowerCamelCase}: <#if field.defaultValue?has_content>${field.defaultValue}<#else>0.0</#if>,
    <#else >
  ${field.nameLowerCamelCase}: <#if field.defaultValue?has_content>'${field.defaultValue}'<#else>null</#if>,
    </#if>
  </#if>
</#list>
})

// 表单规则
const rules = {
<#list fieldList as field>
  <#if field.nameLowerCamelCase != 'id' && field.nameLowerCamelCase != 'userId' && !ignoreColumns?seq_contains(field.nameLowerCamelCase)>
  ${field.nameLowerCamelCase}: [
    {required: <#if field.nullAble>false<#else>true</#if>, message: '请输入${field.nameCn}', trigger: 'blur'},
    <#if field.nameLowerCamelCase == "idCard">
    {pattern: /^\d{18}$/, message: '身份证号必须为18位数字', trigger: 'blur'}
    </#if>
    <#if field.nameLowerCamelCase == "mobile">
    {pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur'}
    </#if>
  ],
  </#if>
</#list>
}

const formRef = ref(null)

const handleAdd = () => {
  dialogVisible.value = true
}

const resetForm = () => {
<#list fieldList as field>
  <#if !ignoreColumns?seq_contains(field.nameLowerCamelCase)>
    <#if field.nameLowerCamelCase == 'id'>
  ${domain}Form.${field.nameLowerCamelCase} = null
    <#elseif (field.refTable)?has_content>
  ${domain}Form.${field.nameLowerCamelCase} = null
    <#elseif field.type?contains('int')>
  ${domain}Form.${field.nameLowerCamelCase} = <#if field.defaultValue?has_content>${field.defaultValue}<#else>0</#if>
    <#elseif field.javaType == 'BigDecimal'>
  ${domain}Form.${field.nameLowerCamelCase} = <#if field.defaultValue?has_content>${field.defaultValue}<#else>0.0</#if>
    <#else>
  ${domain}Form.${field.nameLowerCamelCase} = <#if field.defaultValue?has_content>'${field.defaultValue}'<#else>null</#if>
     </#if>
  </#if>
</#list>
}

const submitForm = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    const res = await save${Domain}(${domain}Form)
    if (res.code === 200) {
      ElMessage.success(${domain}Form.id ? '编辑成功' : '添加成功')
      dialogVisible.value = false
      resetForm()
      // 刷新乘客列表
      pagination.page = 1
      await get${Domain}List()
    } else {
      // 场景：http 返回头的状态码是 200，但是返回体中的 code 字段值不是 200
      ElMessage.error(res.msg || (${domain}Form.id ? '编辑${tableNameCn}失败' : '添加${tableNameCn}失败'))
    }
  } catch (error) {
    ElMessage.error(error.response.data.msg || (${domain}Form.id ? '编辑${tableNameCn}失败' : '添加${tableNameCn}失败'))
  }
}

const handleEdit = (row) => {
  Object.assign(${domain}Form, row)
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    '确定要删除${tableNameCn} ' + row.id + ' 吗？',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await delete${Domain}(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        // 如果当前页只有一条数据且不是第一页，删除后自动跳转到上一页
        if (${domain}List.value.length === 1 && pagination.page > 1) {
          pagination.page--
        }
        await get${Domain}List()
      } else {
        ElMessage.error(res.msg || '删除失败')
      }
    } catch (error) {
      console.error('删除${tableNameCn}失败:', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
    // 取消删除，不做任何操作
    console.log('取消删除，不做任何操作')
  })
}

const dialogTitle = computed(() => {
  return ${domain}Form.id ? '编辑${tableNameCn}' : '新增${tableNameCn}'
})
</#if>

const tableLoading = ref(false)

// 获取${tableNameCn}列表数据
const get${Domain}List = async () => {
  tableLoading.value = true
  try {
    const res = await list${Domain}({
      ...pagination,
      keyword: searchForm.keyword
    })
    if (res.code === 200) {
      ${domain}List.value = res.data.list
      pagination.total = res.data.total
    } else {
      ElMessage.error(res.msg || '获取${tableNameCn}列表失败')
    }
  } catch (error) {
    console.error('获取${tableNameCn}列表失败:', error)
    ElMessage.error('获取${tableNameCn}列表失败')
  } finally {
    tableLoading.value = false
  }
}

// 在组件挂载时获取数据
onMounted(() => {
  get${Domain}List()
})

// 分页相关数据
const pagination = reactive({
  // 每页显示的数据条数
  size: 10,
  // 当前页码，从1开始
  page: 1,
  // 总数据条数，会在获取数据时更新
  total: 0
})

// 分页变化处理函数
const handleCurrentChange = async (val) => {
  pagination.page = val
  await get${Domain}List()
}

const handleSizeChange = async (val) => {
  pagination.size = val
  pagination.page = 1
  await get${Domain}List()
}

const handleRefresh = () => {
  get${Domain}List()
}

<#-- 生成下拉框选项（从字段注释中提取，格式：xxx【1-选项一、2-选项二、3-选项三】） -->
<#assign hasEnum = false>
<#list fieldList as field>
    <#if field.comment?contains('【') && field.comment?contains('】')>
        <#assign commentPart = field.comment?split('【')[1]?split('】')[0]>
        <#if commentPart?contains('、')>
            <#assign hasEnum = true>
// ${field.comment?split('｜')?first}选项
const ${field.nameLowerCamelCase}Options = [
            <#assign options = commentPart?split('、')>
            <#list options as option>
                <#assign optionTrimmed = option?trim>
                <#if optionTrimmed?contains('-')>
                    <#assign optionParts = optionTrimmed?split('-')>
                    <#if optionParts?size gte 2>
  { label: '${optionParts[1]?trim}', value: '${optionParts[0]?trim}' }<#if option_has_next>,</#if>
                    </#if>
                </#if>
            </#list>
]
        </#if>
    </#if>
</#list>
<#if hasEnum>

<#-- 下拉框选项映射 -->
const optionsMap = {
<#list fieldList as field>
    <#if field.hasOption>
  '${field.nameLowerCamelCase}': ${field.nameLowerCamelCase}Options,
    </#if>
</#list>
}

// 获取枚举字段的显示标签
const getOptionLabel = (fieldName, value) => {
  if (value === null || value === undefined || value === '') return '-'
  const options = optionsMap[fieldName]
  if (!options) return String(value)
  const option = options.find(opt => opt.value === String(value))
  return option?.label || String(value)
}

</#if>
// 搜索表单
const searchForm = reactive({
  keyword: ''  // 搜索关键字
})

// 处理搜索
const handleSearch = () => {
  pagination.page = 1  // 重置到第一页
  get${Domain}List()
}

// 重置搜索
const handleReset = () => {
  searchForm.keyword = ''
  handleSearch()
}

<#if hasImageColumn || hasVideoColumn>
const previewVisible = ref(false)
const previewUrl = ref('')
const previewType = ref('') // 如：'image' 或 'video'

// 处理图片/视频预览
const handlePreview = (url, type) => {
  if (!url) return
  previewUrl.value = url
  previewType.value = type
  previewVisible.value = true
}
</#if>

<#if hasMarkdownColumn>
// 处理 markdown 内容预览
const contentPreviewRef = useTemplateRef('contentPreview')
const handleContentPreview = (content) => {
  markdownPreviewText.value = content
  markdownPreviewVisible.value = true
  // 调用子组件 (CustomMarkdown) 的 doPreview 方法
  nextTick(() => {
    contentPreviewRef.value.doPreview()
  });
}
</#if>
</script>

<template>
  <div class="${domain}-container">
    <!-- 顶部按钮 -->
    <div class="top-tools">
      <div class="left">
      <#if !readOnly>
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增${tableNameCn}</el-button>
      </#if>
        <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
      </div>
      <div class="right">
        <el-form :inline="true" :model="searchForm" @submit.prevent>
          <el-form-item class="mb0">
            <el-input
                    v-model="searchForm.keyword"
                    placeholder="请输入关键词"
                    clearable
            />
          </el-form-item>
          <el-form-item class="mb0">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- ${tableNameCn}列表 -->
    <el-table
      :data="${domain}List"
      v-loading="tableLoading"
      style="width: 100%"
      border
    >
      <#list fieldList as field>
        <#if field.nameLowerCamelCase != 'id' && !ignoreColumns?seq_contains(field.nameLowerCamelCase)>
          <#if field.hasOption>
      <el-table-column prop="${field.nameLowerCamelCase}" label="${field.nameCn}">
        <template #default="{ row }">
          {{ getOptionLabel('${field.nameLowerCamelCase}', row.${field.nameLowerCamelCase}) }}
        </template>
      </el-table-column>
          <#elseif field.imageColumn>
      <el-table-column prop="${field.nameLowerCamelCase}" label="${field.nameCn}">
        <template #default="{ row }">
          <div v-if="row.${field.nameLowerCamelCase}" class="preview-cell">
            <el-image
                    :src="row.${field.nameLowerCamelCase}"
                    class="preview-image"
                    <#--:preview-src-list="[row.${field.nameLowerCamelCase}]"-->
                    <#--preview-teleported-->
                    fit="cover"
            />
            <div class="preview-actions">
              <el-button
                type="primary"
                link
                @click.stop="handlePreview(row.${field.nameLowerCamelCase}, 'image')"
              >
                <el-icon><ZoomIn /></el-icon>
              </el-button>
            </div>
          </div>
          <span v-else></span>
        </template>
      </el-table-column>
          <#elseif field.videoColumn>
      <el-table-column label="${field.nameCn}" prop="${field.nameLowerCamelCase}" min-width="120">
        <template #default="{ row }">
          <div v-if="row.${field.nameLowerCamelCase}" class="preview-cell">
            <video
              :src="row.${field.nameLowerCamelCase}"
              class="preview-video"
            />
            <div class="preview-actions">
              <el-button
                type="primary"
                link
                @click.stop="handlePreview(row.${field.nameLowerCamelCase}, 'video')"
              >
                <el-icon><VideoPlay /></el-icon>
              </el-button>
            </div>
          </div>
          <span v-else>-</span>
        </template>
      </el-table-column>
          <#elseif field.markdownColumn>
      <el-table-column prop="${field.nameLowerCamelCase}" label="${field.nameCn}">
        <template #default="{ row }">
          <el-button
            v-if="row.${field.nameLowerCamelCase}"
            type="primary"
            link
            @click="handleContentPreview(row.${field.nameLowerCamelCase})"
          >
            预览
          </el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
          <#else >
      <el-table-column prop="${field.nameLowerCamelCase}" label="${field.nameCn}"/>
          </#if>
        </#if>
      </#list>
      <#if !readOnly>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
      </#if>
    </el-table>

    <!-- 分页组件 -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[2, 5, 10, 20]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
    <#if !readOnly>

    <!-- ${tableNameCn}对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="<#if hasMarkdownColumn>880px<#else >500px</#if>"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="${domain}Form"
        :rules="rules"
        label-width="100px"
        status-icon
      >
      <#list fieldList as field>
        <#if field.nameLowerCamelCase != 'id' && !ignoreColumns?seq_contains(field.nameLowerCamelCase)>
        <el-form-item label="${field.nameCn}" prop="${field.nameLowerCamelCase}">
          <#if (field.refTable)?has_content>
          <#assign RefDomain = field.refTable?replace('_', ' ')?split(' ')?map(w -> w?cap_first)?join('')>
          <${RefDomain}Select v-model="${domain}Form.${field.nameLowerCamelCase}" />
          <#elseif field.comment?contains('【') && field.comment?contains('】') && field.comment?split('【')[1]?split('】')[0]?contains('、')>
          <el-select
            v-model="${domain}Form.${field.nameLowerCamelCase}"
            style="width: 100%"
            placeholder="请选择${field.nameCn}"
          >
            <el-option
              v-for="option in ${field.nameLowerCamelCase}Options"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <#elseif field.javaType == 'LocalTime'>
          <el-time-picker
            v-model="${domain}Form.${field.nameLowerCamelCase}"
            placeholder="请输入${field.nameCn}"
            value-format="HH:mm:ss"
          />
          <#elseif field.javaType == 'LocalDate'>
          <el-date-picker
            v-model="${domain}Form.${field.nameLowerCamelCase}"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请输入${field.nameCn}"
          />
          <#elseif field.javaType == 'LocalDateTime'>
          <el-date-picker
            v-model="${domain}Form.${field.nameLowerCamelCase}"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="请输入${field.nameCn}"
          />
          <#elseif field.type?contains('int') && !field.type?starts_with('tinyint')>
          <el-input-number v-model="${domain}Form.${field.nameLowerCamelCase}" />
          <#elseif field.javaType == 'BigDecimal'>
          <el-input-number v-model="${domain}Form.${field.nameLowerCamelCase}" :precision="2" />
          <#elseif field.javaType == 'Double'>
          <el-input-number v-model="${domain}Form.${field.nameLowerCamelCase}" />
          <#elseif field.type?starts_with('tinyint')>
          <el-switch v-model="${domain}Form.${field.nameLowerCamelCase}" :active-value="1" :inactive-value="0" />
          <#elseif field.type?starts_with('varchar') && field.length == 512>
          <el-input
            v-model="${domain}Form.${field.nameLowerCamelCase}"
            placeholder="请输入${field.nameCn}"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
          />
          <#elseif field.imageColumn>
          <image-uploader v-model="${domain}Form.${field.nameLowerCamelCase}" />
          <#elseif field.videoColumn>
          <video-uploader
            v-model="${domain}Form.${field.nameLowerCamelCase}"
            @duration-change="duration => ${domain}Form.duration = duration"
          />
          <#elseif field.markdownColumn>
          <el-input
            v-model="${domain}Form.${field.nameLowerCamelCase}"
            placeholder="请输入${field.nameCn}"
            clearable
            style="display: none"
          />
          <custom-markdown
            :markdown-text="${domain}Form.${field.nameLowerCamelCase}"
            @update:markdown-text="val => ${domain}Form.${field.nameLowerCamelCase} = val"
            id="${domain}-${field.nameLowerCamelCase}-editor"
          />
          <#elseif field.type?ends_with('text')>
          <el-input
            v-model="${domain}Form.${field.nameLowerCamelCase}"
            placeholder="请输入${field.nameCn}"
            type="textarea"
            :autosize="{ minRows: 5, maxRows: 10 }"
          />
          <#else>
          <el-input
            v-model="${domain}Form.${field.nameLowerCamelCase}"
            placeholder="请输入${field.nameCn}"
            clearable
          />
          </#if>
        </el-form-item>
        </#if>
      </#list>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取 消</el-button>
          <el-button type="primary" @click="submitForm">确 定</el-button>
        </span>
      </template>
    </el-dialog>
    </#if>

    <#if hasImageColumn || hasVideoColumn>
    <!-- 图片/视频预览对话框 -->
    <el-dialog
      v-model="previewVisible"
      title="预览"
      width="800px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <el-image
        v-if="previewType === 'image'"
        :src="previewUrl"
        class="preview-dialog-image"
        fit="contain"
      />
      <video
        v-if="previewType === 'video'"
        :src="previewUrl"
        class="preview-dialog-video"
        controls
      />
    </el-dialog>
    </#if>

    <#if hasMarkdownColumn>
    <!-- markdown 内容预览对话框 -->
    <el-dialog
      v-model="markdownPreviewVisible"
      title="Markdown 内容预览"
      width="800px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <custom-markdown
        ref="contentPreview"
        :markdown-text="markdownPreviewText"
        id="markdown-preview-editor"
        :default-model="'previewOnly'"
        style="width: 100%"
      />
    </el-dialog>
    </#if>
  </div>
</template>

<style scoped>
.${domain}-container {
  padding: 20px;
}

.top-tools {
  margin-bottom: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.top-tools .left {
  display: flex;
  gap: 10px;
}

.top-tools .right {
  display: flex;
  align-items: center;
  margin-left: auto;

  .el-form .el-form-item:last-child {
    margin-right: 0;
  }
}

@media screen and (max-width: 768px) {
  .top-tools {
    flex-direction: column;
    align-items: flex-start;
  }

  .top-tools .right {
    margin-left: 0;
    width: 100%;
  }
}

.mb0 {
  margin-bottom: 0;
}

.el-form--inline .el-form-item {
  margin-right: 16px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-scrollbar) {
  margin-bottom: 0;
}
<#if hasImageColumn || hasVideoColumn>
.preview-cell {
  position: relative;
  width: 100px;
  height: 60px;
  overflow: hidden;
  border-radius: 4px;
}

.preview-video,
.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-cell:hover .preview-actions {
  opacity: 1;
}

.preview-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.3s;
}

.preview-actions .el-button {
  color: white;
  font-size: 20px;
}

.preview-dialog-image {
  width: 100%;
  max-height: 600px;
}

.preview-dialog-video {
  width: 100%;
  max-height: 600px;
  background-color: #000;
}
</#if>
</style>
