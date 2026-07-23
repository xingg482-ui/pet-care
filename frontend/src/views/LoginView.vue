<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const formRef = ref()
const form = reactive({
  username: 'admin',
  password: 'admin123',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const result = await login(form)
    localStorage.setItem('petCareToken', result.token)
    localStorage.setItem('petCareUsername', result.username)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <h1>宠物订单系统</h1>
        <p>单门店宠物服务管理后台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" class="login-button" @click="handleLogin">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #eef7f2 0%, #f7f5ff 50%, #f5f7fb 100%);
}

.login-panel {
  width: min(420px, 100%);
  padding: 32px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 20px 45px rgba(31, 41, 55, 0.12);
}

.login-copy {
  margin-bottom: 28px;
}

.login-copy h1 {
  margin: 0 0 8px;
  font-size: 28px;
}

.login-copy p {
  margin: 0;
  color: #6b7280;
}

.login-button {
  width: 100%;
}
</style>
