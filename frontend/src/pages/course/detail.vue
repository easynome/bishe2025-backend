<!--&lt;!&ndash; pages/course/detail.vue &ndash;&gt;-->
<!--<template>-->
<!--  <div>-->
<!--    <el-card>-->
<!--      <template #header>-->
<!--        <div class="card-header">-->
<!--          <span>课程详情 - {{ course.name }}</span>-->
<!--        </div>-->
<!--      </template>-->

<!--      <div class="course-info">-->
<!--        <p><strong>课程描述：</strong>{{ course.description }}</p>-->
<!--        <p><strong>学分：</strong>{{ course.credit }}</p>-->
<!--        <p><strong>发布时间：</strong>{{ formatDate(course.createdAt) }}</p>-->
<!--      </div>-->

<!--      &lt;!&ndash; 评分组件 &ndash;&gt;-->
<!--      <div class="rating-section" v-if="!hasRated">-->
<!--        <h3>请为这门课程评分</h3>-->
<!--        <el-rate v-model="rating" :colors="colors" @change="submitRating" />-->
<!--      </div>-->

<!--      <div class="rating-section" v-else>-->
<!--        <h3>您已评分：{{ userRating }} 星</h3>-->
<!--        <el-rate v-model="userRating" disabled />-->
<!--      </div>-->
<!--    </el-card>-->
<!--  </div>-->
<!--</template>-->

<!--<script setup>-->
<!--import { ref, onMounted } from 'vue'-->
<!--import { useRoute } from 'vue-router'-->
<!--import { getCourseDetail, rateCourse } from '~/api/manager'-->
<!--import { ElMessage } from 'element-plus'-->

<!--const route = useRoute()-->
<!--const courseId = route.params.id-->

<!--const course = ref({})-->
<!--const rating = ref(0)-->
<!--const userRating = ref(0)-->
<!--const hasRated = ref(false)-->
<!--const colors = ['#99A9BF', '#F7BA2A', '#FF9900']-->

<!--const loadCourseDetail = async () => {-->
<!--  try {-->
<!--    const res = await getCourseDetail(courseId)-->
<!--    course.value = res.data.data || {}-->
<!--    // TODO: 检查用户是否已评分-->
<!--  } catch (error) {-->
<!--    ElMessage.error('加载课程详情失败')-->
<!--  }-->
<!--}-->

<!--const submitRating = async (value) => {-->
<!--  try {-->
<!--    await rateCourse(courseId, value)-->
<!--    ElMessage.success('评分成功！')-->
<!--    hasRated.value = true-->
<!--    userRating.value = value-->
<!--  } catch (error) {-->
<!--    ElMessage.error('评分失败')-->
<!--    rating.value = 0-->
<!--  }-->
<!--}-->

<!--onMounted(() => {-->
<!--  loadCourseDetail()-->
<!--})-->
<!--</script>-->
<template>
  <div class="course-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>{{ course.name || '课程详情' }}</h2>
          <el-tag type="primary" v-if="course.credit">学分: {{ course.credit }}</el-tag>
        </div>
      </template>

      <!-- 课程基本信息 -->
      <div class="course-info">
        <p v-if="course.description" class="description">{{ course.description }}</p>
        <p v-else class="no-description">该课程暂无详细描述</p>

        <div class="meta-info">
          <span v-if="course.createdAt">
            <el-icon><Calendar /></el-icon>
            发布时间: {{ formatDate(course.createdAt) }}
          </span>
        </div>
      </div>

      <el-divider />

      <!-- 评分区域 -->
      <div class="rating-section">
        <h3>为这门课程评分</h3>
        <p class="rating-hint">您的评分将帮助我们为您推荐更合适的课程</p>

        <!-- 未评分状态 -->
        <div v-if="!hasRated && !loadingRating">
          <el-rate
              v-model="tempRating"
              :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
              @change="handleRateChange"
              show-text
              text-color="#ff9900"
              :texts="['很差', '较差', '一般', '较好', '很好']"
          />
          <div class="rating-actions" v-if="tempRating > 0">
            <el-button type="primary" @click="submitRating" :loading="submitting">
              提交{{ tempRating }}星评分
            </el-button>
            <el-button @click="tempRating = 0">取消</el-button>
          </div>
        </div>

        <!-- 已评分状态 -->
        <div v-else class="rated-info">
          <div class="rated-badge">
            <el-icon color="#67C23A"><SuccessFilled /></el-icon>
            <span style="margin-left: 8px;">您已评分: </span>
            <el-rate v-model="userRating" disabled class="rated-stars" />
            <span class="rating-score">{{ userRating }} 星</span>
          </div>
          <p class="rated-hint">
            评分已提交！<el-link type="primary" @click="goToRecommend">
            查看个性化推荐 →
          </el-link>
          </p>
          <el-button type="info" size="small" @click="hasRated = false; tempRating = 0">
            重新评分
          </el-button>
        </div>

        <!-- 加载状态 -->
        <div v-if="loadingRating" class="loading-rating">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载评分信息...</span>
        </div>
      </div>

      <el-divider />

      <!-- 相关推荐 -->
      <div class="related-courses" v-if="similarCourses.length > 0">
        <h3>学习了这门课的同学也喜欢</h3>
        <el-row :gutter="20">
          <el-col :span="8" v-for="course in similarCourses" :key="course.id">
            <el-card shadow="hover" class="related-card" @click="viewCourse(course.id)">
              <h4>{{ course.name }}</h4>
              <p class="course-credit">学分: {{ course.credit }}</p>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCourseDetail, rateCourse, getRecommendations } from '~/api/manager'
import { ElMessage } from 'element-plus'
import {
  Calendar,
  SuccessFilled,
  Loading
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const courseId = route.params.id

// 课程数据
const course = ref({})
const loading = ref(false)

// 评分相关
const tempRating = ref(0)
const userRating = ref(0)
const hasRated = ref(false)
const loadingRating = ref(false)
const submitting = ref(false)

// 相关课程
const similarCourses = ref([])

// 加载课程详情
const loadCourseDetail = async () => {
  loading.value = true
  try {
    const res = await getCourseDetail(courseId)
    if (res.data.code === 200) {
      course.value = res.data.data || {}
      // 加载后检查用户是否已评分
      checkUserRating()
    } else {
      ElMessage.error('加载课程详情失败')
    }
  } catch (error) {
    console.error('加载课程详情失败:', error)
    ElMessage.error('加载课程详情失败')
  } finally {
    loading.value = false
  }
}

// 检查用户是否已评分
const checkUserRating = async () => {
  // TODO: 调用接口检查用户是否已评分
  // 暂时模拟
  loadingRating.value = false
}

// 评分变化
const handleRateChange = (value) => {
  console.log('评分变化:', value)
}

// 提交评分
const submitRating = async () => {
  if (tempRating.value === 0) {
    ElMessage.warning('请先选择评分')
    return
  }

  submitting.value = true
  try {
    const res = await rateCourse(courseId, tempRating.value)
    if (res.data.code === 200) {
      ElMessage.success('评分成功！')
      userRating.value = tempRating.value
      hasRated.value = true

      // 跳转到推荐页或显示相关推荐
      loadSimilarCourses()
    } else {
      ElMessage.error(res.data.msg || '评分失败')
    }
  } catch (error) {
    console.error('评分失败:', error)
    ElMessage.error(error.response?.data?.msg || '评分失败')
  } finally {
    submitting.value = false
  }
}

// 加载相关课程
const loadSimilarCourses = async () => {
  try {
    const res = await getRecommendations(3)
    if (res.data.code === 200) {
      similarCourses.value = res.data.data || []
    }
  } catch (error) {
    console.error('加载相关课程失败:', error)
  }
}

// 跳转到推荐页
const goToRecommend = () => {
  router.push('/study/recommend')
}

// 查看其他课程
const viewCourse = (id) => {
  router.push(`/course/detail/${id}`)
}

// 日期格式化
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
}

onMounted(() => {
  loadCourseDetail()
})
</script>

<style scoped>
.course-detail {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.description {
  line-height: 1.6;
  color: #606266;
}

.no-description {
  color: #909399;
  font-style: italic;
}

.meta-info {
  margin-top: 15px;
  color: #909399;
  font-size: 14px;
}

.meta-info .el-icon {
  margin-right: 5px;
}

.rating-section {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.rating-hint {
  color: #666;
  margin-bottom: 20px;
}

.rating-actions {
  margin-top: 20px;
}

.rated-info {
  padding: 15px;
  background: #f0f9eb;
  border-radius: 6px;
  border: 1px solid #e1f3d8;
}

.rated-badge {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.rated-stars {
  margin: 0 10px;
}

.rating-score {
  color: #e6a23c;
  font-weight: bold;
  font-size: 16px;
}

.rated-hint {
  margin: 10px 0;
}

.loading-rating {
  text-align: center;
  padding: 20px;
  color: #909399;
}

.loading-rating .el-icon {
  margin-right: 10px;
}

.related-courses {
  margin-top: 30px;
}

.related-card {
  cursor: pointer;
  transition: transform 0.3s;
  margin-bottom: 20px;
}

.related-card:hover {
  transform: translateY(-5px);
}

.course-credit {
  color: #e6a23c;
  margin-top: 10px;
}
</style>