<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { checkUsernameAvailable, login, register, uploadAvatar } from '../api/auth'
import { createSupportTicket, fetchLatestPublicConversation, sendCustomerSupportMessage } from '../api/supportTickets'

const router = useRouter()
const loading = ref(false)
const registering = ref(false)
const uploading = ref(false)
const supportVisible = ref(false)
const supportSubmitting = ref(false)
const activeTab = ref('login')
const loginFormRef = ref()
const registerFormRef = ref()
const supportFormRef = ref()
const avatarInputRef = ref()
const loginForm = reactive({
  username: 'admin',
  password: 'admin123',
})
const registerForm = reactive({
  username: '',
  displayName: '',
  password: '',
  confirmPassword: '',
  role: 'CUSTOMER',
  avatarUrl: '',
})
const supportForm = reactive({
  contactName: '',
  contactInfo: '',
  username: '',
  issueType: 'LOGIN_FAILED',
  content: '',
})
const supportResult = ref(null)

const loginRules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const accountPattern = /^(1\d{10}|[^\s@]+@[^\s@]+\.[^\s@]+)$/

async function validateRegisterUsername(_rule, value, callback) {
  const username = value?.trim()
  if (!username || !accountPattern.test(username)) {
    callback()
    return
  }
  try {
    const available = await checkUsernameAvailable(username)
    if (!available) {
      callback(new Error('登录账号已存在'))
      return
    }
    callback()
  } catch {
    callback(new Error('用户名校验失败，请稍后重试'))
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入手机号或邮箱', trigger: 'blur' },
    { pattern: accountPattern, message: '请输入正确的手机号或邮箱', trigger: 'blur' },
    { validator: validateRegisterUsername, trigger: 'blur' },
  ],
  displayName: [{ required: true, message: '请输入用户名称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur',
    },
  ],
  role: [{ required: true, message: '请选择登录角色', trigger: 'change' }],
}

const roleOptions = [
  { label: '客户', value: 'CUSTOMER', description: '注册成功后可直接登录预约服务' },
  { label: '管理员', value: 'ADMIN', description: '注册后需高级管理员审核' },
  { label: '高级管理员', value: 'SUPER_ADMIN', description: '注册后需已有高级管理员审核' },
]
const supportRules = {
  contactName: [{ required: true, message: '请输入联系人名称', trigger: 'blur' }],
  contactInfo: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  content: [{ required: true, message: '请输入问题描述', trigger: 'blur' }],
}

async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const result = await login(loginForm)
    localStorage.setItem('petCareToken', result.token)
    localStorage.setItem('petCareAccountId', result.accountId)
    localStorage.setItem('petCareUsername', result.username)
    localStorage.setItem('petCareDisplayName', result.displayName)
    localStorage.setItem('petCareRole', result.role)
    localStorage.setItem('petCareAvatarUrl', result.avatarUrl || '')
    localStorage.setItem('petCareCustomerId', result.customerId || '')
    ElMessage.success('登录成功')
    router.push(result.role === 'CUSTOMER' ? '/my-home' : '/dashboard')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value.validate()
  registering.value = true
  try {
    const result = await register({
      username: registerForm.username,
      displayName: registerForm.displayName,
      password: registerForm.password,
      role: registerForm.role,
      avatarUrl: registerForm.avatarUrl,
    })
    ElMessage.success(result.message || '注册成功')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
    resetRegisterForm()
  } finally {
    registering.value = false
  }
}

function resetRegisterForm() {
  registerForm.username = ''
  registerForm.displayName = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  registerForm.role = 'CUSTOMER'
  registerForm.avatarUrl = ''
  registerFormRef.value?.clearValidate()
}

function triggerAvatarUpload() {
  avatarInputRef.value?.click()
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) {
    return
  }
  uploading.value = true
  try {
    const result = await uploadAvatar(file)
    registerForm.avatarUrl = result.url
    ElMessage.success('头像上传成功')
  } finally {
    uploading.value = false
  }
}

async function submitSupportTicket() {
  await supportFormRef.value.validate()
  supportSubmitting.value = true
  try {
    let result
    if (supportResult.value?.id && supportResult.value.contactInfo === supportForm.contactInfo) {
      supportResult.value = await sendCustomerSupportMessage(supportResult.value.id, {
        contactInfo: supportForm.contactInfo,
        content: supportForm.content,
      })
      result = { message: '消息已发送' }
    } else {
      result = await createSupportTicket(supportForm)
      supportResult.value = await fetchLatestPublicConversation(supportForm.contactInfo)
    }
    localStorage.setItem('petCareSupportContactInfo', supportForm.contactInfo)
    ElMessage.success(result.message || '消息已发送')
    supportForm.content = ''
  } finally {
    supportSubmitting.value = false
  }
}

async function openSupportChat() {
  supportVisible.value = true
  const contactInfo = supportForm.contactInfo || localStorage.getItem('petCareSupportContactInfo') || ''
  if (!contactInfo) {
    return
  }
  supportForm.contactInfo = contactInfo
  try {
    supportResult.value = await fetchLatestPublicConversation(contactInfo)
  } catch {
    supportResult.value = null
  }
}

function handleSupportKeydown(event) {
  if (event.shiftKey) {
    return
  }
  event.preventDefault()
  submitSupportTicket()
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <h1>宠物订单系统</h1>
        <p>账号登录、自由注册与角色选择</p>
      </div>
      <el-tabs v-model="activeTab" stretch class="auth-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" class="login-form">
            <el-form-item label="登录账号" prop="username">
              <el-input v-model="loginForm.username" size="large" placeholder="手机号、邮箱或初始管理员账号" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" size="large" type="password" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="login-button" @click="handleLogin">
              登录
            </el-button>
            <el-button text class="support-button" @click="openSupportChat">联系客服</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-position="top" class="login-form">
            <div class="avatar-upload">
              <button class="avatar-preview" type="button" @click="triggerAvatarUpload">
                <img v-if="registerForm.avatarUrl" :src="registerForm.avatarUrl" alt="头像" />
                <span v-else>{{ registerForm.displayName?.slice(0, 1) || '头像' }}</span>
              </button>
              <div>
                <el-button :loading="uploading" @click="triggerAvatarUpload">上传头像</el-button>
                <p>支持 jpg、jpeg、png、webp，最大 2MB</p>
              </div>
              <input ref="avatarInputRef" class="file-input" type="file" accept="image/jpeg,image/png,image/webp" @change="handleAvatarChange" />
            </div>

            <el-form-item label="手机号或邮箱" prop="username">
              <el-input v-model="registerForm.username" size="large" placeholder="注册后将作为登录账号" />
            </el-form-item>
            <el-form-item label="用户名称" prop="displayName">
              <el-input v-model="registerForm.displayName" size="large" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" size="large" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" size="large" type="password" show-password @keyup.enter="handleRegister" />
            </el-form-item>
            <el-form-item label="登录角色" prop="role">
              <el-radio-group v-model="registerForm.role" class="role-options">
                <el-radio v-for="role in roleOptions" :key="role.value" :label="role.value" border>
                  <strong>{{ role.label }}</strong>
                  <span>{{ role.description }}</span>
                </el-radio>
              </el-radio-group>
            </el-form-item>
            <el-alert
              v-if="registerForm.role !== 'CUSTOMER'"
              title="管理员和高级管理员注册后，需要已有高级管理员审核通过才能登录。"
              type="warning"
              show-icon
              :closable="false"
            />
            <el-button type="primary" size="large" :loading="registering" class="login-button" @click="handleRegister">
              注册账号
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="supportVisible" title="在线客服" width="430px" class="support-dialog">
      <div class="chat-window">
        <div class="chat-messages">
          <div class="chat-row staff">
            <span class="chat-avatar staff-avatar">客</span>
            <p>您好，请直接描述遇到的问题。无法登录、审核状态、密码问题都可以在这里处理。</p>
          </div>
          <template v-if="supportResult">
            <div class="chat-row self">
              <p>{{ supportResult.content }}</p>
              <span class="chat-avatar">{{ supportResult.contactName?.slice(0, 1) || '我' }}</span>
            </div>
            <div v-for="reply in supportResult.replies" :key="reply.id" class="chat-row" :class="reply.replierRole === 'CUSTOMER' ? 'self' : 'staff'">
              <span v-if="reply.replierRole !== 'CUSTOMER'" class="chat-avatar staff-avatar">客</span>
              <p>{{ reply.content }}</p>
              <span v-if="reply.replierRole === 'CUSTOMER'" class="chat-avatar">{{ supportResult.contactName?.slice(0, 1) || '我' }}</span>
            </div>
          </template>
        </div>
        <el-form ref="supportFormRef" :model="supportForm" :rules="supportRules" class="chat-form">
          <div class="chat-profile">
            <el-form-item prop="contactName">
              <el-input v-model="supportForm.contactName" placeholder="昵称" maxlength="50" />
            </el-form-item>
            <el-form-item prop="contactInfo">
              <el-input v-model="supportForm.contactInfo" placeholder="手机号或邮箱" maxlength="80" />
            </el-form-item>
          </div>
          <el-form-item>
              <el-input v-model="supportForm.username" placeholder="手机号或邮箱（可选）" maxlength="80" />
          </el-form-item>
          <el-form-item prop="content">
            <el-input v-model="supportForm.content" type="textarea" :rows="3" maxlength="500" resize="none" placeholder="输入消息" @keydown.enter="handleSupportKeydown" />
          </el-form-item>
          <div class="chat-send-row">
            <span>Enter 发送，Ctrl + Enter 也可发送</span>
            <el-button type="primary" :loading="supportSubmitting" class="chat-send" @click="submitSupportTicket">发送</el-button>
          </div>
        </el-form>
      </div>
    </el-dialog>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    linear-gradient(90deg, rgba(255, 248, 235, 0.18), rgba(255, 255, 255, 0.16)),
    url('../assets/login-background.png') center / cover no-repeat;
}

.login-panel {
  width: min(480px, 100%);
  padding: 28px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(232, 224, 211, 0.78);
  border-radius: 8px;
  box-shadow: 0 1px 1px rgba(23, 33, 43, 0.04), 0 28px 70px rgba(86, 64, 32, 0.18);
  backdrop-filter: blur(12px);
}

.login-copy {
  margin-bottom: 24px;
}

.login-copy h1 {
  margin: 0 0 8px;
  font-size: 24px;
  line-height: 32px;
  font-weight: 700;
  letter-spacing: 0;
}

.login-copy p {
  margin: 0;
  color: var(--pc-muted);
}

.login-button {
  width: 100%;
  margin-top: 4px;
}

.support-button {
  width: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 8px;
  margin-left: 0;
  padding-left: 0;
  padding-right: 0;
  text-align: center;
}

.auth-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.avatar-preview {
  width: 64px;
  height: 64px;
  flex: 0 0 64px;
  border: 1px solid var(--pc-border);
  border-radius: 50%;
  background: var(--pc-primary-soft);
  color: var(--pc-primary-hover);
  font-weight: 700;
  overflow: hidden;
  cursor: pointer;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.avatar-upload p {
  margin: 8px 0 0;
  color: var(--pc-muted);
  font-size: 12px;
}

.file-input {
  display: none;
}

.role-options {
  width: 100%;
  display: grid;
  gap: 10px;
}

.role-options :deep(.el-radio) {
  width: 100%;
  height: auto;
  min-height: 54px;
  display: flex;
  align-items: center;
  margin: 0;
  padding: 10px 12px;
  white-space: normal;
}

.role-options :deep(.el-radio__label) {
  display: grid;
  gap: 4px;
  line-height: 18px;
}

.role-options span {
  color: var(--pc-muted);
  font-size: 12px;
}

.support-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.chat-window {
  display: flex;
  flex-direction: column;
  height: 560px;
  margin: -8px -6px 0;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #f5f5f5;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  background: #f5f5f5;
}

.chat-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}

.chat-row.self {
  justify-content: flex-end;
}

.chat-row p {
  max-width: 280px;
  margin: 0;
  padding: 10px 12px;
  border-radius: 4px;
  background: #ffffff;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-row.self p {
  background: #95ec69;
}

.chat-avatar {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 30px;
  border-radius: 50%;
  background: #f59e0b;
  color: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.staff-avatar {
  background: #d97706;
}

.chat-form {
  padding: 12px;
  border-top: 1px solid #e4e7ed;
  background: #f5f5f5;
}

.chat-profile {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 10px;
}

.chat-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.chat-send-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #98a2b3;
  font-size: 12px;
}

.chat-send {
  min-width: 92px;
  background: #07c160;
  border-color: #07c160;
}

@media (max-width: 720px) {
  .login-page {
    align-items: flex-start;
    padding: 36px 16px;
    background-position: 42% center;
  }
}
</style>
