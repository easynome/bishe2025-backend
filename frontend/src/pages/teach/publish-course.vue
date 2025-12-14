<template>
  <div class="publish-course-container">
    <el-card class="publish-card">
      <template #header>
        <div class="card-header">
          <h2>发布新课程</h2>
          <p class="sub-title">发布新的学习课程供学生学习</p>
        </div>
      </template>

      <el-form
          ref="courseFormRef"
          :model="courseForm"
          :rules="courseRules"
          label-width="120px"
          label-position="top"
      >
        <el-row :gutter="30">
          <el-col :span="12">
            <el-form-item label="课程名称" prop="name" required>
              <el-input
                  v-model="courseForm.name"
                  placeholder="请输入课程名称"
                  maxlength="100"
                  show-word-limit
              />
              <div class="form-hint">课程名称应简洁明了，能准确反映课程内容</div>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="学分" prop="credit" required>
              <el-input-number
                  v-model="courseForm.credit"
                  :min="1"
                  :max="10"
                  placeholder="请输入学分"
                  style="width: 100%;"
              />
              <div class="form-hint">通常为1-5学分，根据课程难度和学时设置</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="课程描述" prop="description">
          <el-input
              v-model="courseForm.description"
              type="textarea"
              :rows="5"
              placeholder="请详细描述课程内容、教学目标、适合人群等"
              maxlength="500"
              show-word-limit
          />
          <div class="form-hint">详细的描述能帮助学生更好地了解课程</div>
        </el-form-item>

        <el-form-item label="课程封面" prop="coverUrl">
          <el-upload
              class="cover-upload"
              action="#"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleCoverChange"
          accept="image/*"
          >
          <div v-if="courseForm.coverUrl" class="cover-preview">
            <img :src="courseForm.coverUrl" alt="课程封面" />
            <div class="cover-actions">
              <el-button type="primary" link>更换图片</el-button>
              <el-button type="danger" link @click.stop="courseForm.coverUrl = ''">删除</el-button>
            </div>
          </div>
          <div v-else class="upload-placeholder">
            <el-icon size="40"><Upload /></el-icon>
            <p>点击上传课程封面</p>
            <p class="upload-hint">建议尺寸：800×450px，支持JPG、PNG格式</p>
          </div>
          </el-upload>
        </el-form-item>

        <el-form-item label="难度等级" prop="difficulty">
          <el-rate
              v-model="courseForm.difficulty"
              :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
              show-text
              :texts="['入门', '初级', '中级', '高级', '专家']"
          />
          <div class="form-hint">  设置课程难度，帮助学生选择合适的课程</div>
        </el-form-item>

        <el-form-item>
          <div class="form-actions">
            <el-button
                type="primary"
                @click="submitCourse"
                :loading="submitting"
                size="large"
            >
              <el-icon><Promotion /></el-icon>
              发布课程
            </el-button>
            <el-button
                @click="resetForm"
                :disabled="submitting"
                size="large"
            >
              重置
            </el-button>
            <el-button
                type="info"
                @click="$router.push('/course/list')"
                size="large"
            >
              返回课程列表
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <!-- 发布成功提示 -->
      <el-dialog
          v-model="publishSuccess"
          title="发布成功"
          width="400px"
          :show-close="false"
          :close-on-click-modal="false"
          :close-on-press-escape="false"
      >
        <div style="text-align: center; padding: 20px;">
          <el-icon size="60" color="#67C23A"><CircleCheck /></el-icon>
          <h3 style="margin: 20px 0 10px;">课程发布成功！</h3>
          <p style="color: #606266;">您的课程已成功发布到平台</p>
        </div>
        <template #footer>
          <div style="text-align: center;">
            <el-button type="primary" @click="goToMyCourses">查看我的课程</el-button>
            <el-button @click="publishSuccess = false; resetForm()">继续发布</el-button>
          </div>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { publishCourse } from '~/api/manager'  // 需要创建这个API
import { ElMessage } from 'element-plus'
import {
  Upload,
  Promotion,
  CircleCheck
} from '@element-plus/icons-vue'

const router = useRouter()
const courseFormRef = ref(null)

// 表单数据
const courseForm = reactive({
  name: '',
  description: '',
  credit: 2,
  difficulty: 3,
  coverUrl: ''
})

// 表单验证规则
const courseRules = {
  name: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符', trigger: 'blur' }
  ],
  credit: [
    { required: true, message: '请输入学分', trigger: 'blur' },
    { type: 'number', min: 1, max: 10, message: '学分范围为1-10', trigger: 'blur' }
  ],
  description: [
    { max: 500, message: '描述不能超过500个字符', trigger: 'blur' }
  ]
}

// 状态
const submitting = ref(false)
const publishSuccess = ref(false)

// 处理封面图片上传
const handleCoverChange = (file) => {
  // 实际开发中这里应该上传到服务器
  const reader = new FileReader()
  reader.onload = (e) => {
    courseForm.coverUrl = e.target.result
  }
  reader.readAsDataURL(file.raw)
}

// 提交课程
const submitCourse = async () => {
  const valid = await courseFormRef.value?.validate()
  if (!valid) return

  submitting.value = true

  try {
    const res = await publishCourse(courseForm)

    if (res.data.code === 200) {
      ElMessage.success('课程发布成功')
      publishSuccess.value = true
    } else {
      ElMessage.error(res.data.msg || '发布失败')
    }
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error(error.response?.data?.msg || '发布失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  courseFormRef.value?.resetFields()
  courseForm.credit = 2
  courseForm.difficulty = 3
  courseForm.coverUrl = ''
}

// 跳转到我的课程
const goToMyCourses = () => {
  router.push('/teach/my-course')
}
</script>

<style scoped>
.publish-course-container {
  position: fixed;
  top: 96px;
  left: v-bind('$store.state.asideWidth');
  right: 0;
  bottom: 0;
  transition: margin-left 0.2s;
  overflow-y: auto;
  padding: 20px;
}

.publish-card {
  max-width: 1000px;
  margin: 0 auto;
}

.card-header {
  text-align: center;
}

.card-header h2 {
  margin: 0;
  color: #303133;
}

.sub-title {
  margin: 10px 0 0;
  color: #909399;
  font-size: 14px;
}

.form-hint {
  position: relative;
  left: 10px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.cover-upload {
  width: 100%;
}

.upload-placeholder {
  border: 2px dashed #dcdfe6;
  border-radius: 6px;
  padding: 40px;
  text-align: center;
  color: #909399;
  cursor: pointer;
  transition: border-color 0.3s;
}

.upload-placeholder:hover {
  border-color: #409eff;
}

.upload-hint {
  font-size: 12px;
  margin-top: 5px;
  color: #c0c4cc;
}

.cover-preview {
  position: relative;
  width: 100%;
  max-width: 400px;
  border-radius: 6px;
  overflow: hidden;
}

.cover-preview img {
  width: 100%;
  height: auto;
  display: block;
}

.cover-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.7);
  padding: 10px;
  display: flex;
  justify-content: center;
  gap: 20px;
}

.form-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

@media (max-width: 768px) {
  .publish-course-container {
    padding: 10px;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-actions .el-button {
    width: 100%;
  }
}
</style>