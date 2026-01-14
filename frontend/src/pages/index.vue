<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="mb-20">
      <!-- 数据统计卡片 -->
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #409EFF;">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ dashboardData.totalStudents || 0 }}</div>
              <div class="stat-label">活跃学生数</div>
            </div>
          </div>
          <div class="stat-footer">
            <span>已进行评分的学生总数</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #67C23A;">
              <el-icon><Reading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ dashboardData.courseCount || 0 }}</div>
              <div class="stat-label">课程总数</div>
            </div>
          </div>
          <div class="stat-footer">
            <span>平台现有课程数量</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #E6A23C;">
              <el-icon><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ hotCoursesCount }}</div>
              <div class="stat-label">热门课程</div>
            </div>
          </div>
          <div class="stat-footer">
            <span>评分次数最多的课程</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background-color: #909399;">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ engineStatusShort }}</div>
              <div class="stat-label">推荐引擎</div>
            </div>
          </div>
          <div class="stat-footer">
            <span>用户协同过滤算法</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 热门课程列表 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="hot-courses-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span>热门课程 Top 10</span>
              <el-button type="primary" link @click="refreshData">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <div v-if="loading" class="loading-container">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载数据中...</span>
          </div>

          <div v-else-if="hotCourses.length === 0" class="empty-state">
            <el-icon size="50"><Collection /></el-icon>
            <p>暂无热门课程数据</p>
          </div>

          <el-table v-else :data="hotCourses" style="width: 100%">
            <el-table-column label="排名" width="80" align="center">
              <template #default="scope">
                <el-tag :type="getRankType(scope.$index + 1)">
                  {{ scope.$index + 1 }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column prop="name" label="课程名称" min-width="200">
              <template #default="scope">
                <div class="course-name">
                  <span>{{ scope.row.name }}</span>
                  <el-tag v-if="scope.row.credit" size="small" type="info">
                    {{ scope.row.credit }}学分
                  </el-tag>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="120" align="center">
              <template #default="scope">
                <el-button type="primary" link @click="viewCourse(scope.row.id)">
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 系统信息 -->
      <el-col :span="8">
        <el-card class="system-info-card" shadow="never">
          <template #header>
            <span>系统信息</span>
          </template>

          <div class="system-info">
            <div class="info-item">
              <span class="info-label">推荐算法：</span>
              <span class="info-value">{{ dashboardData.engineStatus || '用户协同过滤' }}</span>
            </div>

            <div class="info-item">
              <span class="info-label">数据更新时间：</span>
              <span class="info-value">{{ dashboardData.updateTime || '--' }}</span>
            </div>

            <div class="info-item">
              <span class="info-label">系统版本：</span>
              <span class="info-value">v1.0.0</span>
            </div>

            <div class="info-item">
              <span class="info-label">当前用户：</span>
              <span class="info-value">{{ currentUser.username || '--' }}</span>
            </div>

            <div class="info-item">
              <span class="info-label">用户角色：</span>
              <span class="info-value">
                  <el-tag :type="getRoleTagType(currentUser.roleName)">
                    {{ currentUser.roleName || '--' }}
                  </el-tag>
              </span>
            </div>
          </div>

          <el-divider />

          <!-- 修改快捷操作部分 -->
          <div class="quick-actions">
            <h4>快捷操作</h4>
            <el-space wrap>
              <!-- 公共操作 -->
              <el-button type="primary" @click="goToCourseList">
                <el-icon><List /></el-icon>
                课程列表
              </el-button>

              <!-- 学生专属 -->
              <el-button v-if="currentUser.roleName === '学生'" type="info" @click="goToRecommend">
                <el-icon><Star /></el-icon>
                推荐课程
              </el-button>

              <el-button v-if="currentUser.roleName === '学生'" type="success" @click="goToMyLearning">
                <el-icon><Reading /></el-icon>
                我的学习
              </el-button>

              <!-- 教师专属 -->
              <el-button v-if="currentUser.roleName === '教师'" type="warning" @click="goToPublishCourse">
                <el-icon><Upload /></el-icon>
                发布课程
              </el-button>

              <el-button v-if="currentUser.roleName === '教师'" type="success" @click="goToMyCourses">
                <el-icon><Document /></el-icon>
                我的课程
              </el-button>

              <!-- 管理员专属 -->
              <el-button v-if="currentUser.roleName === '管理员'" type="danger" @click="goToUserManagement">
                <el-icon><User /></el-icon>
                用户管理
              </el-button>

              <el-button v-if="currentUser.roleName === '管理员'" type="warning" @click="goToCourseManagement">
                <el-icon><Setting /></el-icon>
                课程管理
              </el-button>
            </el-space>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { getDashboardData } from '~/api/manager'
import { ElMessage } from 'element-plus'
import {
  User,
  Reading,
  Star,
  Cpu,
  Refresh,
  Loading,
  Collection,
  List,
  Upload
} from '@element-plus/icons-vue'

const router = useRouter()
const store = useStore()

// 数据
const dashboardData = ref({})
const loading = ref(false)

// 当前用户信息
const currentUser = computed(() => store.state.user || {})

// 计算属性
const hotCourses = computed(() => dashboardData.value.hotList || [])
const hotCoursesCount = computed(() => hotCourses.value.length)

const engineStatusShort = computed(() => {
  const status = dashboardData.value.engineStatus
  if (!status) return 'CF'
  return status.includes('User-CF') ? '协同过滤' : 'CF'
})

// 加载数据
const loadDashboardData = async () => {
  loading.value = true
  try {
    const res = await getDashboardData()
    if (res.data.code === 200) {
      dashboardData.value = res.data.data || {}
      ElMessage.success('数据加载成功')
    } else {
      ElMessage.error(res.data.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 刷新数据
const refreshData = () => {
  loadDashboardData()
}

// 排名标签类型
const getRankType = (rank) => {
  if (rank === 1) return 'danger'  // 第一名
  if (rank === 2) return 'warning' // 第二名
  if (rank === 3) return 'success' // 第三名
  return ''  // 其他
}

// 角色标签类型
const getRoleTagType = (role) => {
  switch (role) {
    case '管理员': return 'danger'
    case '教师': return 'warning'
    case '学生': return 'success'
    default: return 'info'
  }
}

// 导航功能
const viewCourse = (id) => {
  router.push(`/course/detail/${id}`)
}

const goToCourseList = () => {
  router.push('/course/list')
}

const goToPublishCourse = () => {
  router.push('/teach/publish-course')
}

const goToUserManagement = () => {
  router.push('/admin/users/list')
}

const goToRecommend = () => {
  router.push('/study/recommend')
}

const goToMyLearning = () => {
  router.push('/study/my-learning')
}

const goToMyCourses = () => {
  router.push('/teach/my-course')
}

const goToCourseManagement = () => {
  router.push('/admin/courses/list')
}
onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.dashboard-container {
  position: fixed;
  top: 96px;
  left: v-bind('$store.state.asideWidth');
  right: 0;
  bottom: 0;
  transition: margin-left 0.2s;
  overflow-y: auto;
  padding: 20px;
}

.mb-20 {
  margin-bottom: 20px;
}

/* 统计卡片样式 */
.stat-card {
  height: 120px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.stat-content {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.stat-icon .el-icon {
  font-size: 24px;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.stat-footer {
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
  font-size: 12px;
  color: #909399;
}

/* 热门课程卡片 */
.hot-courses-card,
.system-info-card {
  height: 100%;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.course-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.loading-container {
  text-align: center;
  padding: 50px 0;
  color: #909399;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #909399;
}

/* 系统信息 */
.system-info {
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-bottom: 15px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  width: 120px;
  color: #606266;
  font-size: 14px;
}

.info-value {
  flex: 1;
  color: #303133;
  font-weight: 500;
}

/* 快捷操作 */
.quick-actions h4 {
  margin-bottom: 15px;
  color: #303133;
}
</style>