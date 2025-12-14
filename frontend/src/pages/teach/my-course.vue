<template>
  <div class="my-course-container">
    <el-card class="my-course-card">
      <template #header>
        <div class="card-header">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
              <h2 style="margin: 0; display: flex; align-items: center; gap: 10px;">
                <el-icon size="24"><Document /></el-icon>
                我的课程管理
              </h2>
              <p style="margin: 8px 0 0; color: #909399; font-size: 14px;">
                管理您发布的所有课程，查看学生选课情况
              </p>
            </div>

            <div>
              <el-button type="primary" @click="goToPublishCourse">
                <el-icon><Plus /></el-icon>
                发布新课程
              </el-button>
            </div>
          </div>

          <!-- 统计信息 -->
          <div class="stats-row" v-if="courseStats">
            <el-row :gutter="20">
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ courseStats.total || 0 }}</div>
                  <div class="stat-label">课程总数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ courseStats.published || 0 }}</div>
                  <div class="stat-label">已上架</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ courseStats.totalStudents || 0 }}</div>
                  <div class="stat-label">总学生数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-item">
                  <div class="stat-value">{{ courseStats.avgStudents || 0 }}</div>
                  <div class="stat-label">平均学生/课程</div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </template>

      <!-- 筛选工具栏 -->
      <div class="toolbar">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索课程名称..."
            style="width: 300px; margin-right: 15px;"
            clearable
            @keyup.enter="handleSearch"
            @clear="resetSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
            v-model="filterStatus"
            placeholder="课程状态"
            style="width: 120px; margin-right: 15px;"
            clearable
            @change="handleFilterChange"
        >
          <el-option label="全部" value="" />
          <el-option label="已上架" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>

        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="resetFilters">
          重置
        </el-button>
      </div>

      <!-- 课程表格 -->
      <el-table
          :data="courses"
          v-loading="loading"
          style="width: 100%; margin-top: 20px;"
          :row-class-name="tableRowClassName"
      >
          <el-table-column prop="name" label="课程名称" width="200" align="center">
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 10px;">
              <div class="course-avatar">
                <el-icon size="20"><Reading /></el-icon>
              </div>
              <div>
                <div class="course-name">{{ scope.row.name }}</div>
                <div class="course-id">ID: {{ scope.row.id }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="描述" width="200" align="center">
          <template #default="scope">
            <div class="course-description">
              {{ scope.row.description || '暂无描述' }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="credit" label="学分" width="90" align="center">
          <template #default="scope">
            <el-tag :type="getCreditTagType(scope.row.credit)" size="small">
              {{ scope.row.credit }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="studentCount" label="学生数" width="90" align="center">
          <template #default="scope">
            <el-tag
                :type="scope.row.studentCount > 0 ? 'success' : 'info'"
                size="small"
            >
              {{ scope.row.studentCount }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="200" align="center">
          <template #default="scope">
            <el-switch
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                active-text="上架"
                inactive-text="下架"
                @change="(val) => handleUpdateCourseStatus(scope.row.id, val)"
            />
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="发布时间" width="180" align="center" >
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <div class="action-buttons">
              <el-button
                  type="primary"
                  link
                  @click="viewCourseStudents(scope.row)"
                  :disabled="scope.row.studentCount === 0"
              >
                <el-icon><User /></el-icon>
                查看学生
              </el-button>
              <el-button
                  type="info"
                  link
                  @click="editCourse(scope.row)"
              >
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button
                  type="danger"
                  link
                  @click="deleteCourse(scope.row.id)"
                  :disabled="scope.row.studentCount > 0"
              >
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!loading && courses.length === 0" class="empty-state">
        <el-icon size="60" color="#C0C4CC"><Collection /></el-icon>
        <h3 style="margin: 20px 0 10px; color: #909399;">
          {{ searchKeyword || filterStatus ? '没有找到相关课程' : '暂无发布的课程' }}
        </h3>
        <p style="color: #909399;">{{ getEmptyStateText() }}</p>
        <el-button
            v-if="!searchKeyword && !filterStatus"
            type="primary"
            @click="goToPublishCourse"
            style="margin-top: 20px;"
        >
          <el-icon><Plus /></el-icon>
          发布第一个课程
        </el-button>
      </div>

      <!-- 分页 -->
      <div v-if="courses.length > 0" class="pagination-container">
        <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :page-sizes="[5, 10, 20, 50]"
            :total="pagination.total"
            :layout="paginationLayout"
            :hide-on-single-page="false"
            background
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getMyCoursesForTeacher, updateCourseStatusByTeacher } from '~/api/manager'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Plus,
  Search,
  Reading,
  User,
  Edit,
  Delete,
  Collection
} from '@element-plus/icons-vue'

const router = useRouter()

// 搜索和筛选
const searchKeyword = ref('')
const filterStatus = ref('')

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
  pages: 0
})

// 课程数据
const courses = ref([])
const loading = ref(false)
const courseStats = ref(null)

// 响应式分页布局
const paginationLayout = computed(() => {
  return window.innerWidth < 768 ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'
})

// 加载课程数据
const loadCourses = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current,
      size: pagination.size
    }

    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }

    //添加这里：传递状态筛选
    if (filterStatus.value !== '') {
      params.status = filterStatus.value  // 传递状态值
    }
    const res = await getMyCoursesForTeacher(params)

    if (res.data.code === 200) {
      const data = res.data.data
      courses.value = data.courses || []

      // 更新分页信息
      pagination.total = data.total || 0
      pagination.pages = data.pages || 0
      pagination.current = data.current || 1
      pagination.size = data.size || 10

      // 计算统计数据
      calculateStats(courses.value)

      // 如果没有数据且不是第一页，回到第一页
      if (courses.value.length === 0 && pagination.current > 1) {
        pagination.current = 1
        loadCourses()
      }
    } else {
      ElMessage.error(res.data.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载课程失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 计算统计数据
const calculateStats = (courseList) => {
  if (!courseList || courseList.length === 0) {
    courseStats.value = null
    return
  }

  const stats = {
    total: courseList.length,
    published: courseList.filter(c => c.status === 1).length,
    totalStudents: courseList.reduce((sum, c) => sum + (c.studentCount || 0), 0),
    avgStudents: 0
  }

  stats.avgStudents = stats.total > 0 ? (stats.totalStudents / stats.total).toFixed(1) : 0
  courseStats.value = stats
}

// 更新课程状态
const handleUpdateCourseStatus = async (courseId, status) => {
  try {
    const res = await updateCourseStatusByTeacher(courseId, status)

    if (res.data.code === 200) {
      ElMessage.success(res.data.data || '状态更新成功')

      // 更新本地数据
      const course = courses.value.find(c => c.id === courseId)
      if (course) {
        course.status = status
        calculateStats(courses.value)  // 重新计算统计
      }
    } else {
      // 如果失败，恢复原来的状态
      const course = courses.value.find(c => c.id === courseId)
      if (course) {
        course.status = course.status === 1 ? 0 : 1
      }
      ElMessage.error(res.data.msg || '状态更新失败')
    }
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('更新失败')

    // 恢复原来的状态
    const course = courses.value.find(c => c.id === courseId)
    if (course) {
      course.status = course.status === 1 ? 0 : 1
    }
  }
}

// 查看课程学生
const viewCourseStudents = (course) => {
  ElMessageBox.alert(
      `课程 "${course.name}" 共有 ${course.studentCount} 名学生`,
      '学生信息',
      {
        confirmButtonText: '确定',
        callback: () => {
          // 这里可以跳转到详细的学生列表页面
          console.log('查看课程学生:', course.id)
        }
      }
  )
}

// 编辑课程
const editCourse = (course) => {
  ElMessage.info('编辑功能开发中...')
  // 可以跳转到编辑页面
  // router.push(`/teach/edit-course/${course.id}`)
}

// 删除课程
const deleteCourse = (courseId) => {
  ElMessageBox.confirm(
      '确定要删除这个课程吗？删除后不可恢复。',
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(() => {
    ElMessage.info('删除功能开发中...')
    // 这里调用删除接口
  }).catch(() => {
    // 用户取消
  })
}

// 分页处理
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  loadCourses()
}

const handleCurrentChange = (page) => {
  pagination.current = page
  loadCourses()
}

// 搜索和筛选
const handleSearch = () => {
  pagination.current = 1
  loadCourses()
}

const handleFilterChange = () => {
  pagination.current = 1
  loadCourses()
}

const resetSearch = () => {
  if (searchKeyword.value) {
    searchKeyword.value = ''
    pagination.current = 1
    loadCourses()
  }
}

const resetFilters = () => {
  searchKeyword.value = ''
  filterStatus.value = ''
  pagination.current = 1
  loadCourses()
}

// 导航
const goToPublishCourse = () => {
  router.push('/teach/publish-course')
}

// 工具函数
const formatDate = (dateString) => {
  if (!dateString) return ''
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN')
  } catch {
    return dateString
  }
}

const getCreditTagType = (credit) => {
  if (credit >= 4) return 'danger'
  if (credit >= 3) return 'warning'
  if (credit >= 2) return 'success'
  return 'info'
}

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 1 ? 'even-row' : ''
}

const getEmptyStateText = () => {
  if (searchKeyword.value) return '请尝试其他搜索关键词'
  if (filterStatus.value !== '') return '请尝试其他筛选条件'
  return '立即发布您的第一个课程，开始您的教学之旅'
}

onMounted(() => {
  loadCourses()
})
</script>

<style scoped>
.my-course-container {
  position: fixed;
  top: 96px;
  left: v-bind('$store.state.asideWidth');
  right: 0;
  bottom: 0;
  transition: margin-left 0.2s;
  overflow-y: auto;
  padding: 20px;
}

.my-course-card {
  min-height: calc(100vh - 140px);
}

.card-header {
  margin-bottom: 20px;
}

.stats-row {
  margin-top: 25px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-item {
  text-align: center;
  padding: 15px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 5px;
}

.stat-label {
  color: #606266;
  font-size: 14px;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
}

.course-avatar {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.course-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.course-id {
  font-size: 12px;
  color: #909399;
}

.course-description {
  color: #606266;
  line-height: 1.5;
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.pagination-container {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

/* 表格斑马纹 */
:deep(.even-row) {
  background-color: #fafafa;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

@media (max-width: 768px) {
  .my-course-container {
    padding: 10px;
  }

  .toolbar {
    flex-direction: column;
    gap: 10px;
  }

  .toolbar > * {
    width: 100% !important;
    margin-right: 0 !important;
  }

  .stats-row .el-col {
    margin-bottom: 15px;
  }
}
</style>