<!-- src/components/Login.vue -->
<template>
  <!-- 全屏风景背景 + 半透明模糊容器 -->
  <div class="login-container">
    <!-- 卡片：半透明模糊 + 圆角/阴影优化 -->
    <el-card
        shadow="always"
        :body-style="{ padding: '0px' }"
        class="login-card"
    >
      <el-row class="items-center login-layout">
        <el-col
            :xs="24"
            :sm="24"
            :md="12"
            class="left-section"
        >
          <div class="text-center text-indigo-600 p-8 title-content">
            <p class="system-title">个性化学习</p>
            <p class="system-title">推荐系统</p>
            <p class="system-desc">智能匹配你的学习内容</p>
          </div>
        </el-col>

        <!-- 表单区：仅调整样式，逻辑完全保留 -->
        <el-col
            :xs="24"
            :sm="24"
            :md="12"
            class="flex items-center justify-center right-section"
        >
          <div class="w-full max-w-sm p-6 sm:p-8 md:p-10 lg:p-12 form-content">
            <h2 class="text-center text-xl sm:text-2xl md:text-3xl lg:text-4xl font-bold text-gray-700 mb-8 welcome-text">
              欢迎回来
            </h2>

            <el-form ref="ruleFormRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
              <el-form-item prop="username">
                <el-input v-model="form.username" size="large" placeholder="请输入用户名" :prefix-icon="User" class="form-input" />
              </el-form-item>

              <el-form-item prop="password">
                <el-input
                    v-model="form.password"
                    size="large"
                    type="password"
                    placeholder="请输入密码"
                    :prefix-icon="Lock"
                    show-password
                    class="form-input"
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" size="large" class="w-full login-btn" :loading="loading" @click="handleLogin">
                  登录
                </el-button>
              </el-form-item>
            </el-form>

            <p class="text-center text-sm text-gray-500 mt-4 register-link">
              还没有账号？
              <span @click="router.push('/register')" class="text-indigo-600 cursor-pointer hover:text-indigo-800">
                立即注册
              </span>
            </p>
            <p class="text-center text-xs text-gray-400 mt-6 footer-text">
              此站点是基于java的个性化学习推荐系统的演示地址
            </p>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
/* -------------- 纯 JavaScript，无 TS 类型 -------------- */
import {reactive, ref} from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import {useRouter} from "vue-router";
import {useStore} from "vuex";
import {toast} from "~/composables/util";


const ruleFormRef = ref(null)   // 表单实例
const loading = ref(false)
const router = useRouter()
const store = useStore()

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    {
      required: true,
      message: '请输入用户名',
      trigger: 'blur'
    },
    {
      min: 3,
      message: '用户名长度不能小于3个字符',
      trigger: 'blur'
    },
  ],
  password: [
    {
      required: true,
      message: '请输入密码',
      trigger: 'blur'
    },
    {
      min: 5,
      message: '密码长度不能小于5个字符',
      trigger: 'blur'
    },
  ]
}

async function handleLogin() {
  const valid = await ruleFormRef.value?.validate()
  if (!valid) return false

  loading.value = true

  try {
    const res = await store.dispatch('login', form)

    if (res && res.code === 200) {
      toast("登录成功")
      // 获取用户信息
      const userInfo = await store.dispatch('getUserInfo')
      console.log('>>> 获取用户信息成功', userInfo)

      setTimeout(() => router.push('/'), 500)
    } else {
      toast(res.msg || '登录失败', 'error')
    }
  } catch (error) {
    toast(error.message || '网络错误，请稍后重试', 'error')
    console.error('登录异常:', error)
  } finally {
    loading.value = false
  }
}

</script>

<style scoped>
/* 1. 全局容器：风景背景 + 全屏布局 */
.login-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 风景背景（可替换为你喜欢的链接） */
  background-image: url('https://picsum.photos/id/1036/1920/1080');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  padding: 1rem;
  box-sizing: border-box;
}

/* 2. 卡片：半透明模糊 + 圆角/阴影优化 */
.login-card {
  width: 100%;
  max-width: 800px;
  min-height: 500px;
  margin: 0;
  border-radius: 1.25rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  overflow: hidden;
}

/* 3. 双栏布局保持原有响应式逻辑 */
.login-layout {
  height: 100%;
}

/* 4. 左侧标题区：艺术字体 + 融合式半透明背景 */
.left-section {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f8f9fc;
  min-height: 500px;
}

/* 引入艺术字体（站酷高端黑，免费可商用） */
@font-face {
  font-family: 'ArtFont';
  src: url('https://cdn.jsdelivr.net/npm/font-zcool-gdh@1.0.0/dist/zcool-gdh.woff2') format('woff2');
  font-weight: normal;
  font-style: normal;
}

.title-content {
  width: 100%;
}

.system-title {
  font-family: 'ArtFont', sans-serif;
  font-size: clamp(1.8rem, 4vw, 2.5rem);
  font-weight: bold;
  line-height: 1.6;
  color: #3a5fcd;
  margin: 0.2rem 0;
}
.system-desc{
  font-size: clamp(1.2rem, 2.5vw, 1.5rem);
  color: #6b7280;
}

/* 5. 右侧表单区：样式优化，保持逻辑不变 */
.right-section {
  min-height: 500px;
}

.form-content {
  width: 100%;
  box-sizing: border-box;
}

.welcome-text {
  color: #374151;
  margin-bottom: 1.5rem !important;
}

.form-input {
  width: 100%;
  margin-bottom: 1rem;
}

.form-input .el-input__wrapper {
  border-radius: 0.5rem;
}

.login-btn {
  border-radius: 0.5rem;
  background-color: #4169e1;
  border: none;
  padding: 0.75rem 0;
}

.footer-text {
  line-height: 1.8;
  white-space: pre-line;
  margin-top: 2rem !important;
  color: #9ca3af;
}

/* 响应式适配（保留原有断点逻辑） */
@media (max-width: 768px) {
  .login-card {
    min-height: 450px;
  }
  .left-section {
    min-height: auto;
    padding: 3rem 2rem !important;
  }
  .right-section {
    padding: 2rem 2rem 3rem !important;
  }
  .system-title {
    font-size: 1.8rem;
  }
}

@media (max-width: 576px) {
  .login-container {
    padding: 0.5rem;
  }
  .login-card {
    border-radius: 1rem;
  }
  .form-content {
    padding: 1.5rem 1rem !important;
  }
}
</style>