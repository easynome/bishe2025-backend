<template>
  <div class="recommend-container">
    <el-card class="recommend-card">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span style="font-size: 18px; font-weight: bold;">为您推荐的课程</span>
          <el-button type="primary" @click="loadRecommendations" :loading="loading">
            刷新推荐
          </el-button>
        </div>
      </template>

      <!-- 加载状态 -->
      <div v-if="loading" style="text-align: center; padding: 40px;">
        <el-icon size="30" class="is-loading"><Loading /></el-icon>
        <p>正在为您生成个性化推荐...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="courses.length === 0" style="text-align: center; padding: 40px;">
        <el-icon size="50"><Collection /></el-icon>
        <p style="margin-top: 10px; color: #909399;">暂无推荐课程</p>
        <p style="color: #909399; font-size: 14px;">请先给一些课程评分，系统才能了解您的偏好</p>
        <el-button type="primary" @click="$router.push('/course/list')" style="margin-top: 20px;">
          去给课程评分
        </el-button>
      </div>

      <!-- 课程列表 -->
      <div v-else>
        <el-row :gutter="20">
          <el-col :span="8" v-for="course in courses" :key="course.id" style="margin-bottom: 20px;">
            <el-card shadow="hover" class="course-card">
              <template #header>
                <div style="font-weight: bold; color: #409EFF;">{{ course.name }}</div>
              </template>

              <div style="min-height: 60px; color: #666; margin-bottom: 15px;">
                {{ course.description || '该课程暂无描述' }}
              </div>

              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="color: #E6A23C;">
                  <el-icon><Star /></el-icon>
                  学分：{{ course.credit }}
                </span>
                <el-button type="primary" size="small" @click="viewDetail(course.id)">
                  查看详情
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecommendations } from '~/api/manager'
import { ElMessage } from 'element-plus'
import { Loading, Collection, Star } from '@element-plus/icons-vue'

const router = useRouter()
const courses = ref([])
const loading = ref(false)

const loadRecommendations = async () => {
  loading.value = true
  try {
    const res = await getRecommendations(6)
    courses.value = res.data.data || []
    if (courses.value.length > 0) {
      ElMessage.success(`为您推荐了 ${courses.value.length} 门课程`)
    }
  } catch (error) {
    console.error('加载推荐失败', error)
    ElMessage.error('加载推荐失败：' + (error.response?.data?.msg || error.message))
  } finally {
    loading.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/course/detail/${id}`)
}

onMounted(() => {
  loadRecommendations()
})
</script>

<style scoped>
.recommend-container{
  //position: fixed;
  top: 128px;
  left: v-bind('$store.state.asideWidth');
  right: 0;
  bottom: 0;
  transition: left 0.2s;
  overflow-y: auto;
  padding: 20px;
  min-height: calc(100% - 64px);
}
.recommend-card{
  margin: 0;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: #fff;
  min-height: calc(100% - 40px);
}
.course-card {
  height: 100%;
  transition: transform 0.3s;
}
.course-card:hover {
  transform: translateY(-5px);
}
</style>