<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete as DeleteIcon, Lock, Upload, User, UserFilled } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import AvatarCropperDialog from '../components/AvatarCropperDialog.vue'
import { changePassword, fetchProfile, restoreDefaultAvatar, updateProfile, uploadAvatar } from '../api/auth'
import { validateAvatarFile } from '../utils/avatarImage'

const loading = ref(false)
const saving = ref(false)
const passwordSaving = ref(false)
const profileFormRef = ref()
const passwordFormRef = ref()
const avatarInputRef = ref()
const avatarFile = ref(null)
const avatarPreviewUrl = ref('')
const currentAvatarUrl = ref('')
const removeAvatarAfterSave = ref(false)
const avatarDragging = ref(false)
const avatarCropperVisible = ref(false)
const avatarCropperFile = ref(null)

const profile = reactive({
  username: '',
  displayName: '',
  role: '',
  status: '',
  phone: '',
  avatarUrl: '',
  createdAt: '',
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules = {
  displayName: [{ required: true, message: '请输入用户名称', trigger: 'blur' }],
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
}

const roleNames = {
  SUPER_ADMIN: '高级管理员',
  ADMIN: '管理员',
  CUSTOMER: '客户',
}

function avatarSourceLabel() {
  if (avatarPreviewUrl.value) {
    return '待保存头像'
  }
  if (currentAvatarUrl.value && !removeAvatarAfterSave.value) {
    return '上传头像'
  }
  return '默认头像'
}

function avatarSrc() {
  if (avatarPreviewUrl.value) {
    return avatarPreviewUrl.value
  }
  if (currentAvatarUrl.value && !removeAvatarAfterSave.value) {
    return currentAvatarUrl.value
  }
  return ''
}

function revokeAvatarPreview() {
  if (avatarPreviewUrl.value) {
    URL.revokeObjectURL(avatarPreviewUrl.value)
  }
  avatarPreviewUrl.value = ''
}

function resetAvatarState(avatarUrl = '') {
  revokeAvatarPreview()
  avatarFile.value = null
  avatarCropperFile.value = null
  currentAvatarUrl.value = avatarUrl || ''
  removeAvatarAfterSave.value = false
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
}

async function loadProfile() {
  loading.value = true
  try {
    const result = await fetchProfile()
    Object.assign(profile, {
      username: result.username,
      displayName: result.displayName,
      role: result.role,
      status: result.status,
      phone: result.phone || '',
      avatarUrl: result.avatarUrl || '',
      createdAt: result.createdAt,
    })
    resetAvatarState(result.avatarUrl || '')
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  await profileFormRef.value.validate()
  saving.value = true
  try {
    let avatarUrl = removeAvatarAfterSave.value ? '' : currentAvatarUrl.value
    if (removeAvatarAfterSave.value) {
      const result = await restoreDefaultAvatar()
      avatarUrl = result.avatarUrl || ''
    }
    if (avatarFile.value) {
      const uploadResult = await uploadAvatar(avatarFile.value)
      avatarUrl = uploadResult.url
    }
    const result = await updateProfile({
      displayName: profile.displayName,
      phone: profile.phone,
      avatarUrl,
    })
    Object.assign(profile, {
      displayName: result.displayName,
      phone: result.phone || '',
      avatarUrl: result.avatarUrl || '',
    })
    resetAvatarState(result.avatarUrl || '')
    localStorage.setItem('petCareDisplayName', result.displayName)
    localStorage.setItem('petCareAvatarUrl', result.avatarUrl || '')
    window.dispatchEvent(new CustomEvent('pet-care-user-updated'))
    ElMessage.success('资料已更新')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  await passwordFormRef.value.validate()
  passwordSaving.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    passwordFormRef.value?.clearValidate()
    ElMessage.success('密码已修改')
  } finally {
    passwordSaving.value = false
  }
}

function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

async function applyAvatarFile(file) {
  try {
    if (!validateAvatarFile(file)) {
      return
    }
    avatarCropperFile.value = file
    avatarCropperVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '头像处理失败，请重新选择')
  }
}

async function handleAvatarChange(event) {
  await applyAvatarFile(event.target.files?.[0])
  event.target.value = ''
}

async function handleAvatarDrop(event) {
  avatarDragging.value = false
  await applyAvatarFile(event.dataTransfer.files?.[0])
}

function handleAvatarDragLeave(event) {
  if (!event.currentTarget.contains(event.relatedTarget)) {
    avatarDragging.value = false
  }
}

function handleAvatarCropConfirm(result) {
  revokeAvatarPreview()
  avatarFile.value = result.file
  avatarPreviewUrl.value = result.previewUrl
  removeAvatarAfterSave.value = false
  avatarCropperFile.value = null
  ElMessage.success('头像效果已确认，保存资料后生效')
}

function handleAvatarCropCancel() {
  avatarCropperFile.value = null
}

function removeSelectedAvatar() {
  revokeAvatarPreview()
  avatarFile.value = null
  removeAvatarAfterSave.value = true
  if (avatarInputRef.value) {
    avatarInputRef.value.value = ''
  }
  localStorage.setItem('petCareAvatarUrl', '')
  window.dispatchEvent(new CustomEvent('pet-care-user-updated'))
}

onMounted(loadProfile)
</script>

<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h1 class="page-title">我的资料</h1>
      </div>
    </div>

    <div v-loading="loading" class="profile-grid">
      <el-card shadow="never" class="profile-card">
        <template #header>
          <div class="card-title">
            <el-icon><User /></el-icon>
            <span>基础资料</span>
          </div>
        </template>

        <div
          class="avatar-section"
          :class="{ 'is-dragover': avatarDragging }"
          @dragenter.prevent="avatarDragging = true"
          @dragover.prevent="avatarDragging = true"
          @dragleave.prevent="handleAvatarDragLeave"
          @drop.prevent="handleAvatarDrop"
        >
          <button class="avatar-preview" :class="{ 'has-image': avatarSrc() }" type="button" @click="triggerAvatarUpload">
            <img v-if="avatarSrc()" :src="avatarSrc()" alt="头像" />
            <el-icon v-else><UserFilled /></el-icon>
          </button>
          <div>
            <el-tag effect="light" type="info">{{ avatarSourceLabel() }}</el-tag>
            <div class="avatar-buttons">
              <el-button :icon="Upload" @click="triggerAvatarUpload">{{ avatarFile ? '重新上传' : '上传头像' }}</el-button>
              <el-button :icon="DeleteIcon" plain @click="removeSelectedAvatar">恢复默认</el-button>
            </div>
            <p>{{ avatarFile ? '当前头像待保存。' : '' }}选择图片后进入裁剪界面，可拖动圆形区域调整效果；支持拖拽，jpg/png/webp，最大 2MB</p>
          </div>
          <input ref="avatarInputRef" class="file-input" type="file" accept="image/jpeg,image/png,image/webp" @change="handleAvatarChange" />
        </div>

        <el-form ref="profileFormRef" :model="profile" :rules="profileRules" label-width="90px">
          <el-form-item label="登录账号">
            <el-input v-model="profile.username" disabled />
          </el-form-item>
          <el-form-item label="角色">
            <el-input :model-value="roleNames[profile.role] || profile.role" disabled />
          </el-form-item>
          <el-form-item label="用户名称" prop="displayName">
            <el-input v-model="profile.displayName" maxlength="50" show-word-limit />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profile.phone" maxlength="30" />
          </el-form-item>
          <el-form-item label="注册时间">
            <el-input v-model="profile.createdAt" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="profile-card">
        <template #header>
          <div class="card-title">
            <el-icon><Lock /></el-icon>
            <span>修改密码</span>
          </div>
        </template>

        <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password @keyup.enter="savePassword" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="passwordSaving" @click="savePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <AvatarCropperDialog
      v-model="avatarCropperVisible"
      :file="avatarCropperFile"
      @confirm="handleAvatarCropConfirm"
      @cancel="handleAvatarCropCancel"
    />
  </AppLayout>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-header p {
  margin: 6px 0 0;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(340px, 0.9fr);
  gap: 16px;
}

.profile-card {
  border-radius: 8px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 22px;
  padding: 12px;
  border: 1px dashed #ead8bb;
  border-radius: 8px;
  background: #fffaf2;
  transition: border-color 180ms ease, background 180ms ease;
}

.avatar-section.is-dragover {
  border-color: #f59e0b;
  background: #fff3dc;
}

.avatar-section p {
  margin: 8px 0 0;
  color: var(--pc-muted);
  font-size: 12px;
}

.avatar-preview {
  width: 76px;
  height: 76px;
  flex: 0 0 76px;
  border: 1px solid var(--pc-border);
  border-radius: 50%;
  background: linear-gradient(180deg, #facc15 0%, #f59e0b 100%);
  color: #ffffff;
  font-weight: 700;
  overflow: hidden;
  cursor: pointer;
  font-size: 34px;
  box-shadow: 0 8px 18px rgba(166, 98, 19, 0.1);
}

.avatar-preview.has-image {
  background: #fff7e8;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.file-input {
  display: none;
}

.avatar-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.avatar-buttons .el-button {
  margin-left: 0;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
