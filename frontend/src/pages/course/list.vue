<template>
  <div class="course-list-container">
    <el-card class="course-list-card">
      <template #header>
        <div class="card-header">
          <span>课程列表</span>
        </div>
      </template>

      <el-table :data="courses" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column prop="name" label="课程名称" min-width="200"/>
        <el-table-column prop="description" label="描述" min-width="300"/>
        <el-table-column prop="credit" label="学分" width="100" align="center" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="viewDetail(scope.row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCourseList } from "~/api/manager.js"
import { ElMessage } from 'element-plus'

const router = useRouter()
const courses = ref([])
const loading = ref(false)

const loadCourses = async () => {
  loading.value = true
  try {
    const res = await getCourseList()
    courses.value = res.data.data || []
    ElMessage.success('加载成功')
  } catch (error) {
    console.error('加载课程失败', error)
    ElMessage.error('加载课程失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/course/detail/${id}`)
}

onMounted(() => {
  loadCourses()
})
</script>
<style scoped>
.course-list-container{
  position: fixed;
  top: 96px;
  left: v-bind('$store.state.asideWidth');
  right: 0;
  bottom: 0;
  transition: margin-left 0.2s;
  overflow-y: auto;
  padding: 20px;
}
.course-list-card {
  margin: 0;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: #fff;
  min-height: calc(100% - 40px);
}
.card-header {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}
.el-table{
  margin-top: 20px;
  border-radius: 4px;
  overflow: hidden;
}
.el-table-column{
  font-size: 14px;
}
</style>