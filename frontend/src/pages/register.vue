<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <h2 class="text-center text-2xl font-bold text-gray-800 mb-6">注册账号</h2>
      </template>

      <el-form
          ref="registerFormRef"
          :model="form"
          :rules="rules"
          label-width="110px"
          @keyup.enter="handleRegister"
      >
      <el-form-item label="用户名" prop="username">
        <el-input
            v-model="form.username"
            placeholder="请输入用户名（3-20位）"
            size="large"
        />
      </el-form-item>

      <el-form-item label="密码" prop="password">
        <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码（6-20位）"
            show-password
            size="large"
        />
      </el-form-item>

      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
            size="large"
        />
      </el-form-item>

      <el-form-item>
        <el-button
            type="primary"
            @click="handleRegister"
            :loading="loading"
            style="width: 100%"
            size="large"
            class="rounded-lg"
        >
          注册
        </el-button>
      </el-form-item>

      <el-form-item>
        <div style="text-align: center; margin-top: 1.5rem;">
          <span class="text-gray-600">已有账号？</span>
          <el-link type="primary" @click="goToLogin">去登录</el-link>
        </div>
      </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
// 脚本逻辑不变，沿用之前的代码
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '~/api/manager'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  const valid = await registerFormRef.value?.validate()
  if (!valid) return

  loading.value = true

  try {
    // 调用注册API（实际项目中替换为真实接口）
    const res = await register(form.username, form.password, form.confirmPassword)
    // 模拟成功响应（测试用）
    // const res = { data: { code: 200 } }

    if (res.data.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      ElMessage.error(res.data.msg || '注册失败')
    }
  } catch (error) {
    console.error('注册异常:', error)
    ElMessage.error(error.response?.data?.msg || '注册失败')
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
/* 背景优化：增加轻微纹理，减少大屏空旷感 */
.register-container {
  @apply min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-400 via-purple-400 to-blue-400;
  background-image: radial-gradient(rgba(255,255,255,0.1) 1px, transparent 1px);
  background-size: 20px 20px;
}

/* 核心优化：响应式宽度调整，解决大屏空白问题 */
.register-card {
  @apply shadow-xl mx-auto bg-white/95 backdrop-blur-sm transition-all duration-300 hover:shadow-2xl hover:-translate-y-1;
  padding: 2.5rem;
  /* 关键：最小宽度+最大宽度自适应 */
  min-width: 320px;    /* 小屏最小宽度，避免过窄 */
  max-width: 90%;      /* 大屏最大占比，减少空白 */
  width: 100%;         /* 自适应拉伸 */
  max-width: clamp(320px, 60vw, 800px); /*  clamp(最小值, 首选值, 最大值) */
}

/* 表单项间距优化，大屏下更舒展 */
.el-form-item {
  margin-bottom: 1.5rem;
}

/* 输入框/按钮样式统一 */
.el-input__wrapper {
  border-radius: 0.5rem !important;
}

/* 适配极宽屏幕（>1920px），避免卡片过宽 */
@media (min-width: 1920px) {
  .register-card {
    max-width: 700px; /* 超大屏固定最大宽度，保持美观 */
  }
}

/* 小屏适配，保持紧凑 */
@media (max-width: 768px) {
  .register-card {
    padding: 1.8rem;
    margin: 0 1rem; /* 小屏左右留边，避免贴边 */
  }
  .el-form {
    label-width: 90px !important; /* 小屏缩小标签宽度 */
  }
}
</style>