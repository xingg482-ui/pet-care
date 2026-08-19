<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, EditPen, Plus, Refresh, Search, SwitchButton, Upload } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import AvatarCropperDialog from '../components/AvatarCropperDialog.vue'
import { validateAvatarFile } from '../utils/avatarImage'
import {
  createPetAvatarLibraryItem,
  fetchPetAvatarLibrary,
  updatePetAvatarLibraryItem,
  updatePetAvatarLibraryStatus,
  uploadPetAvatarLibraryImage,
} from '../api/petAvatarLibrary'

const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)
const records = ref([])
const formRef = ref()
const imageInputRef = ref()
const uploadFile = ref(null)
const uploadPreviewUrl = ref('')
const imageDragging = ref(false)
const brokenImageUrls = ref(new Set())
const imageCropperVisible = ref(false)
const imageCropperFile = ref(null)

const query = reactive({
  keyword: '',
  species: '',
  status: '',
})

const form = reactive({
  species: '狗',
  breed: '',
  keywords: '',
  avatarUrl: '',
  sortOrder: 0,
  remark: '',
})

const rules = {
  species: [{ required: true, message: '请选择种类', trigger: 'change' }],
  breed: [{ required: true, message: '请输入品种名称', trigger: 'blur' }],
}

const speciesOptions = ['狗', '猫', '其他']
const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
]

async function loadRecords() {
  loading.value = true
  try {
    records.value = await fetchPetAvatarLibrary({
      keyword: query.keyword || undefined,
      species: query.species || undefined,
      status: query.status || undefined,
    })
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.species = ''
  query.status = ''
  loadRecords()
}

function normalizeAssetUrl(url) {
  if (!url) {
    return ''
  }
  if (/^https?:\/\//.test(url)) {
    return url
  }
  return url.startsWith('/') ? url : `/${url}`
}

function imageUrl(row) {
  const url = normalizeAssetUrl(row.avatarUrl)
  return url && !brokenImageUrls.value.has(url) ? url : ''
}

function editorPreviewUrl() {
  const url = normalizeAssetUrl(uploadPreviewUrl.value || form.avatarUrl)
  return url && !brokenImageUrls.value.has(url) ? url : ''
}

function revokePreview() {
  if (uploadPreviewUrl.value) {
    URL.revokeObjectURL(uploadPreviewUrl.value)
  }
  uploadPreviewUrl.value = ''
}

function resetForm() {
  Object.assign(form, {
    species: '狗',
    breed: '',
    keywords: '',
    avatarUrl: '',
    sortOrder: 0,
    remark: '',
  })
  revokePreview()
  uploadFile.value = null
  if (imageInputRef.value) {
    imageInputRef.value.value = ''
  }
}

function openCreateDialog() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  editingId.value = row.id
  resetForm()
  Object.assign(form, {
    species: row.species,
    breed: row.breed,
    keywords: row.keywords,
    avatarUrl: row.avatarUrl,
    sortOrder: row.sortOrder,
    remark: row.remark,
  })
  dialogVisible.value = true
}

function openImagePicker() {
  imageInputRef.value?.click()
}

function markBrokenImage(url) {
  const normalizedUrl = normalizeAssetUrl(url)
  if (!normalizedUrl) {
    return
  }
  const nextBrokenUrls = new Set(brokenImageUrls.value)
  nextBrokenUrls.add(normalizedUrl)
  brokenImageUrls.value = nextBrokenUrls
}

async function applyImageFile(file) {
  try {
    if (!validateAvatarFile(file)) {
      return
    }
    imageCropperFile.value = file
    imageCropperVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '头像处理失败，请重新选择')
  }
}

function handleImageCropConfirm(result) {
  revokePreview()
  uploadFile.value = result.file
  uploadPreviewUrl.value = result.previewUrl
  imageCropperFile.value = null
  ElMessage.success('头像效果已确认，保存后生效')
}

function handleImageCropCancel() {
  imageCropperFile.value = null
}

async function handleImageChange(event) {
  await applyImageFile(event.target.files?.[0])
  event.target.value = ''
}

async function handleImageDrop(event) {
  imageDragging.value = false
  await applyImageFile(event.dataTransfer.files?.[0])
}

function handleImageDragLeave(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) {
    imageDragging.value = false
  }
}

function handleImageLoadError(url) {
  markBrokenImage(url)
  if (url === uploadPreviewUrl.value) {
    revokePreview()
    uploadFile.value = null
  }
}

async function saveItem() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form, sortOrder: Number(form.sortOrder || 0) }
    let saved
    if (editingId.value) {
      saved = await updatePetAvatarLibraryItem(editingId.value, payload)
      ElMessage.success('形象库条目已更新')
    } else {
      saved = await createPetAvatarLibraryItem(payload)
      ElMessage.success('形象库条目已新增')
    }
    if (uploadFile.value) {
      await uploadPetAvatarLibraryImage(saved.id, uploadFile.value)
      ElMessage.success('形象库头像已上传')
    }
    dialogVisible.value = false
    resetForm()
    loadRecords()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  const actionText = nextStatus === 'ENABLED' ? '启用' : '停用'
  await ElMessageBox.confirm(`确定${actionText}「${row.breed}」头像吗？`, '确认操作', { type: 'warning' })
  await updatePetAvatarLibraryStatus(row.id, nextStatus)
  ElMessage.success(`形象库条目已${actionText}`)
  loadRecords()
}

onMounted(loadRecords)
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h1 class="page-title">宠物形象库</h1>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增头像</el-button>
    </div>

    <el-card shadow="never" class="filter-panel">
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="品种 / 关键词" clearable />
        </el-form-item>
        <el-form-item label="种类">
          <el-select v-model="query.species" placeholder="全部种类" clearable class="small-select">
            <el-option v-for="item in speciesOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable class="small-select">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadRecords">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="library-panel">
      <div v-loading="loading" class="avatar-grid-wrap">
        <el-empty v-if="!records.length && !loading" description="暂无形象库头像" />
        <div v-else class="avatar-grid">
          <article v-for="row in records" :key="row.id" class="avatar-card" :class="{ 'is-disabled': row.status !== 'ENABLED' }">
            <div class="avatar-image">
              <img v-if="imageUrl(row)" :src="imageUrl(row)" :alt="`${row.breed}头像`" @error="handleImageLoadError(imageUrl(row))" />
              <span v-else>{{ row.breed?.slice(0, 1) || '宠' }}</span>
            </div>
            <div class="avatar-card-body">
              <div class="avatar-card-top">
                <div>
                  <h2>{{ row.breed }}</h2>
                  <p>{{ row.species }} · {{ row.sourceType === 'SYSTEM' ? '系统内置' : '自定义' }}</p>
                </div>
                <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
                  {{ row.status === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </div>
              <div class="keyword-line">{{ row.keywords || '未配置关键词' }}</div>
              <div class="avatar-actions-row">
                <el-button :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
                <el-button
                  :icon="row.status === 'ENABLED' ? SwitchButton : CircleCheck"
                  :type="row.status === 'ENABLED' ? 'warning' : 'success'"
                  plain
                  @click="toggleStatus(row)"
                >
                  {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                </el-button>
              </div>
            </div>
          </article>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑形象库头像' : '新增形象库头像'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="头像">
          <div
            class="editor-avatar"
            :class="{ 'is-dragover': imageDragging }"
            @dragenter.prevent="imageDragging = true"
            @dragover.prevent="imageDragging = true"
            @dragleave.prevent="handleImageDragLeave"
            @drop.prevent="handleImageDrop"
          >
            <div class="editor-avatar-preview">
              <img v-if="editorPreviewUrl()" :src="editorPreviewUrl()" alt="头像预览" @error="handleImageLoadError(editorPreviewUrl())" />
              <span v-else>宠</span>
            </div>
            <div class="editor-avatar-tools">
              <el-button :icon="Upload" @click="openImagePicker">{{ uploadFile ? '重新上传' : '上传图片' }}</el-button>
              <span class="muted">
                {{ uploadFile ? '当前头像待保存。' : '' }}选择图片后进入裁剪界面，可拖动圆形区域调整效果；支持拖拽，jpg/png/webp，最大 2MB
              </span>
              <input ref="imageInputRef" class="file-input" type="file" accept="image/png,image/jpeg,image/webp" @change="handleImageChange" />
            </div>
          </div>
        </el-form-item>
        <el-form-item label="种类" prop="species">
          <el-select v-model="form.species" class="full-width">
            <el-option v-for="item in speciesOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种" prop="breed">
          <el-input v-model="form.breed" maxlength="50" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" maxlength="300" placeholder="多个关键词用逗号分隔" />
        </el-form-item>
        <el-form-item label="头像地址">
          <el-input v-model="form.avatarUrl" maxlength="300" placeholder="上传后自动填充，也可填写已有图片地址" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :step="10" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>

    <AvatarCropperDialog
      v-model="imageCropperVisible"
      :file="imageCropperFile"
      @confirm="handleImageCropConfirm"
      @cancel="handleImageCropCancel"
    />
  </AppLayout>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.filter-panel {
  margin-bottom: 16px;
  border-radius: 8px;
}

.small-select {
  width: 140px;
}

.full-width {
  width: 100%;
}

.library-panel {
  border-radius: 8px;
}

.library-panel :deep(.el-card__body) {
  padding: 0;
}

.avatar-grid-wrap {
  min-height: 240px;
  padding: 20px;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.avatar-card {
  overflow: hidden;
  border: 1px solid #ebe5db;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(41, 31, 18, 0.06);
}

.avatar-card.is-disabled {
  opacity: 0.68;
}

.avatar-image {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 18px 18px 8px;
}

.avatar-image img,
.editor-avatar-preview img {
  width: 120px;
  height: 120px;
  display: block;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-image span {
  width: 120px;
  height: 120px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(145deg, #fff7e7 0%, #f4ddbc 100%);
  color: #a76213;
  font-size: 34px;
  font-weight: 800;
}

.avatar-card-body {
  padding: 12px 16px 16px;
}

.avatar-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.avatar-card h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  line-height: 24px;
}

.avatar-card p {
  margin: 4px 0 0;
  color: #667085;
  font-size: 13px;
}

.keyword-line {
  min-height: 42px;
  margin-top: 10px;
  overflow: hidden;
  color: #667085;
  font-size: 13px;
  line-height: 21px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.avatar-actions-row {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.avatar-actions-row .el-button {
  flex: 1;
  margin-left: 0;
}

.editor-avatar {
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  padding: 12px;
  border: 1px dashed #ead8bb;
  border-radius: 8px;
  background: #fffaf2;
  transition: border-color 180ms ease, background 180ms ease;
}

.editor-avatar.is-dragover {
  border-color: #f59e0b;
  background: #fff3dc;
}

.editor-avatar-preview {
  width: 120px;
  height: 120px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #f0e3d0;
  border-radius: 50%;
  background: linear-gradient(145deg, #fff7e7 0%, #f4ddbc 100%);
  color: #a76213;
  font-size: 32px;
  font-weight: 800;
}

.editor-avatar-tools {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.file-input {
  display: none;
}

@media (max-width: 1280px) {
  .avatar-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .avatar-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .page-header,
  .editor-avatar {
    flex-direction: column;
  }

  .editor-avatar {
    align-items: flex-start;
  }

  .editor-avatar-tools,
  .editor-avatar-tools .el-button {
    width: 100%;
  }

  .avatar-grid {
    grid-template-columns: 1fr;
  }
}
</style>
