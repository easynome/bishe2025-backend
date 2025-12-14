<template>
  <div class="admin-users-container">
    <el-card class="admin-users-card">
      <template #header>
        <div class="card-header">
          <h2>用户管理</h2>
          <div style="color: #606266; font-size: 14px;">
            共 {{ pagination.total }} 位用户
          </div>
        </div>

        <!-- 搜索和筛选区域 -->
        <div class="filter-area">
          <el-input
              v-model="searchKeyword"
              placeholder="搜索用户名..."
              style="width: 250px; margin-right: 10px;"
              clearable
              @keyup.enter="handleSearch"
              @clear="resetSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>

          <el-select
              v-model="filterRoleId"
              placeholder="筛选角色"
              clearable
              style="width: 120px; margin-right: 10px;"
              @change="handleFilterChange"
          >
            <el-option label="全部角色" :value="null" />
            <el-option label="学生" :value="1" />
            <el-option label="教师" :value="2" />
            <el-option label="管理员" :value="3" />
          </el-select>

          <el-button type="primary" @click="handleSearch">
            搜索
          </el-button>
          <el-button @click="resetFilters">
            重置
          </el-button>
        </div>
      </template>

      <!-- 用户表格 -->
      <el-table
          :data="users"
          v-loading="loading"
          style="width: 100%"
          :row-class-name="tableRowClassName"
      >
        <el-table-column prop="id" label="ID"  align="center" />

        <el-table-column prop="username" label="用户名"  align="center" header-align="center"  >
          <template #default="scope">
            <div style="font-weight: 500;">
              {{ scope.row.username }}
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="roleName" label="角色"  align="center">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.roleId)" size="small">
              {{ scope.row.roleName }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="注册时间"  align="center">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态"  align="center">
          <template #default="scope">
            <el-switch
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="禁用"
                @change="(newVal)=>handleStatusChange(scope.row,newVal)"
            />
          </template>
        </el-table-column>

        <el-table-column label="角色操作"    align="center">
          <template #default="scope">
            <el-dropdown @command="(command) => handleRoleChange(scope.row, command)">
              <el-button type="primary" link>
                {{ scope.row.roleName }} <el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :command="1" :disabled="scope.row.roleId === 1">
                    设为学生
                  </el-dropdown-item>
                  <el-dropdown-item :command="2" :disabled="scope.row.roleId === 2">
                    设为教师
                  </el-dropdown-item>
                  <el-dropdown-item :command="3" :disabled="scope.row.roleId === 3">
                    设为管理员
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="center">
          <template #default="scope">
            <el-tooltip content="查看用户详情" placement="top">
              <el-button type="info" link @click="viewUserDetail(scope.row.id)">
                <el-icon><View /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="重置密码" placement="top">
              <el-button type="warning" link @click="resetPassword(scope.row.id)">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div v-if="users.length > 0" style="margin-top: 20px; display: flex; justify-content: center;">
        <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :page-sizes="[5, 10, 20, 50]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            background
        />
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && users.length === 0" style="text-align: center; padding: 60px 0;">
        <el-icon size="60" color="#C0C4CC"><User /></el-icon>
        <p style="margin-top: 15px; color: #909399; font-size: 16px;">
          {{ searchKeyword || filterRoleId ? '没有找到符合条件的用户' : '暂无用户数据' }}
        </p>
        <el-button v-if="searchKeyword || filterRoleId" @click="resetFilters" style="margin-top: 20px;">
          查看所有用户
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  getAdminUsersList,
  updateUserRole,
  updateUserStatus
} from '~/api/manager'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  View,
  Refresh,
  User,
  ArrowDown
} from '@element-plus/icons-vue'

const router = useRouter()

// 搜索和筛选
const searchKeyword = ref('')
const filterRoleId = ref(null)

// 分页数据
const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
  pages: 0
})

// 用户数据
const users = ref([])
const loading = ref(false)

// 加载用户数据
const loadUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.value.current,
      size: pagination.value.size
    }

    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }

    if (filterRoleId.value !== null) {
      params.roleId = filterRoleId.value
    }

    const res = await getAdminUsersList(params)

    if (res.data.code === 200) {
      const data = res.data.data
      users.value = data.users || []

      pagination.value.total = data.total || 0
      pagination.value.pages = data.pages || 0
      pagination.value.current = data.current || 1
      pagination.value.size = data.size || 10

      console.log('加载用户成功:', users.value.length, '条')
    } else {
      ElMessage.error(res.data.msg || '加载失败')
    }
  } catch (error) {
    console.error('加载用户失败:', error)
    ElMessage.error('加载失败: ' + (error.message || '网络错误'))
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.current = 1
  loadUsers()
}

// 重置搜索
const resetSearch = () => {
  if (searchKeyword.value) {
    searchKeyword.value = ''
    pagination.value.current = 1
    loadUsers()
  }
}

// 筛选改变
const handleFilterChange = () => {
  pagination.value.current = 1
  loadUsers()
}

// 重置所有筛选
const resetFilters = () => {
  searchKeyword.value = ''
  filterRoleId.value = null
  pagination.value.current = 1
  loadUsers()
}

// 分页大小改变
const handleSizeChange = (size) => {
  pagination.value.size = size
  pagination.value.current = 1
  loadUsers()
}

// 当前页改变
const handleCurrentChange = (page) => {
  pagination.value.current = page
  loadUsers()
}

// 状态改变
const handleStatusChange = async (user) => {
  const action = user.status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
        `确定要${action}该用户吗？`,
        `确认${action}`,
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )

      const res = await updateUserStatus(user.id, user.status)
      if (res.data.code === 200) {
        ElMessage.success(res.data.data || '状态更新成功')
      } else {
        // 失败时恢复原状态
        user.status = user.status === 1 ? 0 : 1
        ElMessage.error(res.data.msg || '状态更新失败')
      }
    } catch (error) {
      // 失败时恢复原状态
      user.status = user.status === 1 ? 0 : 1
      if (error !== 'cancel' && error !== '') {
        ElMessage.error('状态更新失败: ' + (error.message || '网络错误'))
      }
    }
  }

// 角色改变
const handleRoleChange = async (user, newRoleId) => {
  try {
    const res = await updateUserRole(user.id, newRoleId)
    if (res.data.code === 200) {
      ElMessage.success('角色更新成功')
      // 更新本地数据
      user.roleId = newRoleId
      user.roleName = getRoleName(newRoleId)
      loadUsers() // 重新加载以确保数据一致
    } else {
      ElMessage.error(res.data.msg || '角色更新失败')
    }
  } catch (error) {
    ElMessage.error('角色更新失败: ' + (error.message || '网络错误'))
  }
}

// 角色名称映射
const getRoleName = (roleId) => {
  const roleMap = { 1: '学生', 2: '教师', 3: '管理员' }
  return roleMap[roleId] || '未知'
}

// 角色标签类型
const getRoleTagType = (roleId) => {
  const typeMap = { 1: 'success', 2: 'warning', 3: 'danger' }
  return typeMap[roleId] || 'info'
}

// 日期格式化
const formatDate = (dateString) => {
  if (!dateString) return '未知'
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString('zh-CN')
  } catch {
    return dateString
  }
}

// 表格行样式
const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 1 ? 'even-row' : ''
}

// 查看用户详情
const viewUserDetail = (userId) => {
  ElMessage.info('用户详情功能开发中')
  // router.push(`/admin/user/${userId}`)
}

// 重置密码
const resetPassword = async (userId) => {
  try {
    await ElMessageBox.confirm(
        '确定要重置该用户的密码吗？重置后密码将变为默认密码。',
        '重置密码确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    ElMessage.success('密码重置成功（功能开发中）')
    // TODO: 调用重置密码接口
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
.admin-users-container {
  position: fixed;
  top: 96px;
  left: v-bind('$store.state.asideWidth');
  right: 0;
  bottom: 0;
  transition: margin-left 0.2s;
  overflow-y: auto;
  padding: 20px;
}

.admin-users-card {
  min-height: 600px;
}

.card-header {
  margin-bottom: 20px;
}

.card-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.filter-area {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 10px;
}

/* 表格斑马纹 */
:deep(.even-row) {
  background-color: #fafafa;
}

:deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

/* 状态开关样式 */
:deep(.el-switch__label) {
  color: #606266;
}

:deep(.el-switch__label.is-active) {
  color: #409eff;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .admin-users-container {
    padding: 10px;
  }

  .filter-area {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-area .el-input,
  .filter-area .el-select {
    width: 100% !important;
    margin-right: 0 !important;
    margin-bottom: 10px;
  }
}
</style>