<template>
  <div class="course-management">
    <el-card class="management-card">
      <template #header>
        <div class="card-header">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div>
              <h2 style="margin: 0; color: #303133;
              display: flex; align-items: center; gap: 10px;">
                <el-icon><Document/></el-icon>
                课程管理
              </h2>
              <p style="margin: 5px 0 0 0; color: #909399; font-size: 14px;">
                管理平台所有课程信息
              </p>
            </div>

            <div style="display: flex; gap: 10px;">
              <!-- 状态筛选 -->
              <el-select
                  v-model="filterStatus"
                  placeholder="课程状态"
                  clearable
                  style="width: 120px;"
                  @change="handleFilterChange"
              >
                <el-option label="全部" :value="null" />
                <el-option label="上架" :value="1" />
                <el-option label="下架" :value="0" />
              </el-select>

              <!-- 搜索框 -->
              <el-input
                  v-model="searchKeyword"
                  placeholder="搜索课程名称或描述..."
                  style="width: 250px;"
                  clearable
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>

              <el-button type="primary" @click="handleSearch">
                搜索
              </el-button>
              <el-button @click="resetSearch">重置</el-button>
            </div>
          </div>

          <!-- 统计信息 -->
          <div v-if="!loading" style="margin-top: 15px; display: flex; gap: 20px;">
            <div style="display: flex; align-items: center; gap: 5px;">
              <el-icon color="#409EFF"><Collection /></el-icon>
              <span style="color: #606266;">课程总数: {{ pagination.total }}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 5px;">
              <el-icon color="#67C23C"><CircleCheck /></el-icon>
              <span style="color: #606266;">上架课程: {{ stats.onShelves }}</span>
            </div>
            <div style="display: flex; align-items: center; gap: 5px;">
              <el-icon color="#E6A23C"><Clock /></el-icon>
              <span style="color: #606266;">本页平均评分: {{ stats.avgScore.toFixed(1) }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- 课程表格 -->
      <div v-loading="loading">
        <el-table
            :data="courses"
            style="width: 100%"
            :row-class-name="tableRowClassName"
            stripe
        >
          <el-table-column
              prop="id"
              label="ID"
              width="80"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <span style="color: #909399; font-weight: 500;">{{ scope.row.id }}</span>
            </template>
          </el-table-column>

          <el-table-column
              prop="name"
              label="课程名称"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <div>
                <div style="font-weight: 600; color: #303133; margin-bottom: 4px;">
                  {{ scope.row.name }}
                </div>
                <div style="font-size: 12px; color: #909399;">
                  学分: {{ scope.row.credit }} | 教师: {{ scope.row.teacherName || '未知' }}
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column
              label="课程描述"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <div style="color: #606266; line-height: 1.5; max-height: 60px; overflow: hidden;">
                {{ scope.row.description || '暂无描述' }}
              </div>
            </template>
          </el-table-column>

          <el-table-column
              label="学生/评分"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <div style="display: flex; flex-direction: column; gap: 4px;">
                <div style="display: flex; align-items: center; justify-content: center; gap: 5px;">
                  <el-icon size="14" color="#409EFF"><User /></el-icon>
                  <span style="color: #409EFF; font-weight: 500;">{{ scope.row.studentCount || 0 }}</span>
                  <span style="color: #909399; font-size: 12px;">学生</span>
                </div>
                <div style="display: flex; align-items: center; justify-content: center; gap: 5px;">
                  <el-icon size="14" color="#E6A23C"><Star /></el-icon>
                  <span style="color: #E6A23C; font-weight: 500;">{{ scope.row.avgScore || 0 }}</span>
                  <span style="color: #909399; font-size: 12px;">分</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column
              prop="createdAt"
              label="创建时间"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <div style="color: #909399; font-size: 13px;">
                {{ formatDate(scope.row.createdAt) }}
              </div>
            </template>
          </el-table-column>

          <el-table-column
              label="状态"

              align="center"
              header-align="center"
          >
            <template #default="scope">
              <el-tag
                  :type="scope.row.status === 1 ? 'success' : 'info'"
                  size="small"
                  effect="light"
              >
                {{ scope.row.status === 1 ? '上架' : '下架' }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
              label="操作"
              fixed="right"
              width="200"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <div style="display: flex; gap: 8px; justify-content: center;">
                <!-- 查看详情 -->
                <el-button
                    type="primary"
                    link
                    size="small"
                    @click="viewDetail(scope.row.id)"
                >
                  <el-icon><View /></el-icon>
                  详情
                </el-button>

                <!-- 状态切换 -->
                <el-button
                    v-if="scope.row.status === 1"
                    type="warning"
                    link
                    size="small"
                    @click="changeStatus(scope.row.id, 0)"
                >
                  <el-icon><Remove /></el-icon>
                  下架
                </el-button>
                <el-button
                    v-else
                    type="success"
                    link
                    size="small"
                    @click="changeStatus(scope.row.id, 1)"
                >
                  <el-icon><CircleCheck /></el-icon>
                  上架
                </el-button>

                <!-- 编辑 -->
                <el-button
                    type="info"
                    link
                    size="small"
                    @click="editCourse(scope.row)"
                >
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div v-if="courses.length > 0" style="margin-top: 20px; display: flex; justify-content: center;">
          <el-pagination
              v-model:current-page="pagination.current"
              v-model:page-size="pagination.size"
              :page-sizes="[5, 10, 20, 50]"
              :total="pagination.total"
              :layout="paginationLayout"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
              background
          />
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading" style="text-align: center; padding: 60px 20px;">
          <el-icon size="60" color="#C0C4CC"><Collection /></el-icon>
          <p style="margin-top: 15px; color: #909399; font-size: 16px;">
            {{ searchKeyword || filterStatus !== null ? '没有找到相关课程' : '暂无课程数据' }}
          </p>
          <p v-if="searchKeyword || filterStatus !== null" style="color: #909399; margin-top: 10px;">
            尝试调整搜索条件
          </p>
          <el-button
              v-if="searchKeyword || filterStatus !== null"
              @click="resetSearch"
              style="margin-top: 20px;"
          >
            查看全部课程
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 课程详情对话框 -->
    <el-dialog
        v-model="detailDialogVisible"
        title="课程详情"
        width="700px"
        :close-on-click-modal="false"
    >
      <CourseDetailDialog
          v-if="detailDialogVisible"
          :course-id="selectedCourseId"
          @close="detailDialogVisible = false"
      />
    </el-dialog>

    <!-- 编辑课程对话框 -->
    <el-dialog
        v-model="editDialogVisible"
        title="编辑课程"
        width="500px"
        :close-on-click-modal="false"
    >
      <CourseEditDialog
          v-if="editDialogVisible"
          :course="editingCourse"
          @success="handleEditSuccess"
          @close="editDialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Collection,
  CircleCheck,
  Clock,
  User,
  Star,
  View,
  Remove,
  Edit, Document
} from '@element-plus/icons-vue'
import {
  getAdminCoursesList,
  updateCourseStatusByAdmin
} from '~/api/manager'
import CourseDetailDialog from '~/components/admin/CourseDetailDialog.vue'
import CourseEditDialog from '~/components/admin/CourseEditDialog.vue'

// 搜索和筛选
const searchKeyword = ref('')
const filterStatus = ref(null)

// 分页
const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
  pages: 0
})

// 课程数据
const courses = ref([])
const loading = ref(false)

// 统计信息
const stats = ref({
  onShelves: 0,
  avgScore: 0
})

// 对话框控制
const detailDialogVisible = ref(false)
const editDialogVisible = ref(false)
const selectedCourseId = ref(null)
const editingCourse = ref(null)

// 响应式分页布局
const paginationLayout = computed(() => {
  return window.innerWidth < 768
      ? 'prev, pager, next'
      : 'total, sizes, prev, pager, next, jumper'
})

// 加载课程数据
const loadCourses = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.value.current,
      size: pagination.value.size,
      keyword: searchKeyword.value.trim() || undefined,
      status: filterStatus.value
    }

    const res = await getAdminCoursesList(params)

    if (res.data.code === 200) {
      const data = res.data.data
      courses.value = data.courses || []

      pagination.value.total = data.total || 0
      pagination.value.pages = data.pages || 0
      pagination.value.current = data.current || 1
      pagination.value.size = data.size || 10

      // 计算统计信息(基于当前页数据除了上架数量)
      const onShelves = data.onShelvesCount || 0

      const totalScore = courses.value.reduce((sum, c) => sum + (c.avgScore || 0), 0)
      const avgScore = courses.value.length > 0 ? totalScore / courses.value.length :0
      // 3. 更新 stats
      stats.value = { onShelves, avgScore }


      // 如果没有数据且不是第一页，回到第一页
      if (courses.value.length === 0 && pagination.value.current > 1) {
        pagination.value.current = 1
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

// 计算统计信息
// const calculateStats = () => {
//   const onShelves = courses.value.filter(c => c.status === 1).length
//   const totalScore = courses.value.reduce((sum, c) => sum + (c.avgScore || 0), 0)
//   const avgScore = courses.value.length > 0 ? totalScore / courses.value.length : 0
//
//   stats.value = { onShelves, avgScore }
// }

// 分页大小改变
const handleSizeChange = (size) => {
  pagination.value.size = size
  pagination.value.current = 1
  loadCourses()
}

// 当前页改变
const handleCurrentChange = (page) => {
  pagination.value.current = page
  loadCourses()
}

// 搜索
const handleSearch = () => {
  pagination.value.current = 1
  loadCourses()
}

// 筛选改变
const handleFilterChange = () => {
  pagination.value.current = 1
  loadCourses()
}

// 重置搜索
const resetSearch = () => {
  searchKeyword.value = ''
  filterStatus.value = null
  pagination.value.current = 1
  loadCourses()
}

// 查看详情
const viewDetail = (id) => {
  selectedCourseId.value = id
  detailDialogVisible.value = true
}

// 编辑课程
const editCourse = (course) => {
  editingCourse.value = { ...course }
  editDialogVisible.value = true
}

// 编辑成功回调
const handleEditSuccess = () => {
  editDialogVisible.value = false
  loadCourses()
  ElMessage.success('课程信息更新成功')
}

// 修改课程状态
const changeStatus = async (id, newStatus) => {
  try {
    const statusText = newStatus === 1 ? '上架' : '下架'

    await ElMessageBox.confirm(
        `确定要${statusText}该课程吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const res = await updateCourseStatusByAdmin(id, newStatus)

    if (res.data.code === 200) {
      ElMessage.success(res.data.msg || `${statusText}成功`)
      loadCourses() // 刷新列表
    } else {
      ElMessage.error(res.data.msg || `${statusText}失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('修改状态失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 表格行样式
const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 1 ? 'even-row' : ''
}

// 日期格式化
const formatDate = (dateString) => {
  if (!dateString) return '未知时间'
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateString
  }
}

onMounted(() => {
  loadCourses()
})

// 监听窗口大小变化
window.addEventListener('resize', () => {
  // 布局会自动通过computed更新
})
</script>

<style scoped>
.course-management {
  position: fixed;
  top: 96px;
  left: v-bind('$store.state.asideWidth');
  right: 0;
  bottom: 0;
  transition: margin-left 0.2s;
  overflow-y: auto;
  padding: 20px;
}

.management-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  padding-bottom: 15px;
  border-bottom: 1px solid #ebeef5;
}

/* 表格斑马纹 */
:deep(.even-row) {
  background-color: #fafafa;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

/* 操作按钮样式 */
:deep(.el-button--small) {
  padding: 5px 8px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .course-management {
    padding: 10px;
  }

  .card-header > div:first-child {
    flex-direction: column;
    gap: 15px;
  }

  .el-input {
    width: 100% !important;
  }

  :deep(.el-table) {
    font-size: 13px;
  }

  :deep(.el-table-column) {
    padding: 8px 4px !important;
  }
}
</style>