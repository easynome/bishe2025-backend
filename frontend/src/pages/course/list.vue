<!--<template>-->
<!--  <div class="course-list-container">-->
<!--    <el-card class="course-list-card">-->
<!--      &lt;!&ndash;头部：标题+搜索框&ndash;&gt;-->
<!--      <template #header>-->
<!--        <div class="card-header">-->
<!--          <span>课程列表</span>-->
<!--        </div>-->
<!--      </template>-->

<!--      <el-table :data="courses" v-loading="loading">-->
<!--        <el-table-column prop="id" label="ID" width="80" align="center" />-->
<!--        <el-table-column prop="name" label="课程名称" min-width="200"/>-->
<!--        <el-table-column prop="description" label="描述" min-width="300"/>-->
<!--        <el-table-column prop="credit" label="学分" width="100" align="center" />-->
<!--        <el-table-column label="操作" width="120" fixed="right" align="center">-->
<!--          <template #default="scope">-->
<!--            <el-button type="primary" link @click="viewDetail(scope.row.id)">-->
<!--              查看详情-->
<!--            </el-button>-->
<!--          </template>-->
<!--        </el-table-column>-->
<!--      </el-table>-->
<!--    </el-card>-->
<!--  </div>-->
<!--</template>-->

<template>
  <div class="course-list-container">
    <div class="course-list-card">
      <div class="card-header" style="padding: 20px; border-bottom: 1px solid #ebeef5;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <span style="font-size: 20px; font-weight: 600; color: #303133;">课程列表</span>
            <span style="margin-left: 12px; color:#909399; font-size: 14px;">
              共 {{ pagination.total }} 门课程
            </span>
          </div>

          <div style="display: flex;gap: 10px; align-items: center;">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索课程..."
              style="width:250px;"
              clearable
              @keyup.enter="handleSearch"
              @clear="resetSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </div>
        </div>

        <!--分页信息 -->
        <div v-if="courses.length > 0" style="margin-top: 15px; color: #606266; font-size: 13px;">
          显示第{{(pagination.current-1)*pagination.size+1}}-
          {{Math.min(pagination.current*pagination.size,pagination.total)}}条，
          每页显示
          <el-select
            v-model="pagination.size"
            @change="handleSizeChange"
            style="width: 80px; margin: 0 5px;"
            size="small"
          >
            <el-option label="5" value="5"/>
            <el-option label="10" value="10"/>
            <el-option label="20" value="20"/>
            <el-option label="50" value="50"/>
          </el-select>
          条
        </div>
      </div>

      <!--主要内容区域 -->
      <div style="padding: 20px">
        <!--加载状态 -->
        <div v-if="loading" style="text-align: center; padding: 80px 0;">
          <el-icon size="30" class="is-loading"><Loading/></el-icon>
          <p style="margin-top: 15px; color: #909399 ;">加载课程数据中...</p>
        </div>

        <!--空状态 -->
        <div v-else-if="courses.length === 0" style="text-align: center; padding: 80px 0;">
          <el-icon size="60" color="#C0C4CC"><Collection/></el-icon>
          <p style="margin-top: 15px; color: #909399; font-size: 16px;">
            {{searchKeyword ? '没有找到相关课程' : '暂无课程数据'}}
          </p>
          <p v-if="searchKeyword" style="color: #909399 ; font-size: 16px;">
            尝试调整搜索关键词
          </p>
          <el-button
            v-if="searchKeyword"
            @cliick="resetSearch"
            style="margin-top: 20px"
          >查看全部课程
          </el-button>
        </div>
        <!--课程表格 -->
        <el-table
            v-else
            :data="courses"
            style="width: 100%"
            :row-class-name="tableRowClassName"
        >
          <el-table-column
              prop="id"
              label="ID"
              width="80"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <span style="color: #909399;">{{scope.row.id}}</span>
            </template>
          </el-table-column>

          <el-table-column
              prop="name"
              label="课程名称"
              min-width="200"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <span style="font-weight:500;color: #303133;">
                {{scope.row.name}}
              </span>
            </template>
          </el-table-column>

          <el-table-column
              prop="description"
              label="课程描述"
              min-width="300"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <span style="color: #606266; line-height: 1.5;">
                {{scope.row.description || '暂无课程描述'}}
              </span>
            </template>
          </el-table-column>

          <el-table-column
              prop="credit"
              label="学分"
              width="100"
              align="center"
              header-align="center"
          >
            <template #default="scope">
              <el-tag
                  :type="getCreditTagType(scope.row.credit)"
                  size="small"
                  style="font-weight: 500"
              >
                {{scope.row.credit}}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column
            label="操作"
            width="150"
            fixed="right"
            align="center"
            header-align="center"
          >
            <template #default="scope">
              <el-button
                type="primary"
                link
                @click="viewDetail(scope.row.id)"
                style="font-weight: 500;"
              >
                <el-icon style="margin-right: 4px;"><View/></el-icon>
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!--分页组件 -->
        <div v-if="courses.length > 0&&pagination.pages>1" style="margin-top: 30px;
        display: flex; justify-content: center ;">
          <el-pagination
              v-model:current-page="pagination.current"
              v-model:page-size="pagination.size"
              :page-sizes="[5, 10, 20, 50]"
              :total="pagination.total"
              :layout="paginationLayout"
              :hide-on-single-page="false"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
              background
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, computed} from 'vue'
import { useRouter } from 'vue-router'
import { getCourseList } from "~/api/manager.js"
import { ElMessage } from 'element-plus'
import {
  Search,
  Loading,
  Collection,
  View
} from "@element-plus/icons-vue";

const router = useRouter()
//搜索关键词
const searchKeyword = ref('')
//分页数据
const pagination = ref({
  current: 1,
  size: 10,
  total: 0,
  pages: 0
})

//响应式分页布局
const paginationLayout=computed(()=>{
  return window.innerWidth < 768
      ? 'prev, pager, next'
      : 'total, prev, pager, next'
})

//课程数据
const courses = ref([])
const loading = ref(false)

//加载课程数据
const loadCourses = async () => {
  loading.value = true
  console.log('=== 加载课程调试 ===')
  console.log('处理后:', {
    课程数量: courses.value.length,
    总记录: pagination.value.total,
    总页数: pagination.value.pages,
    当前页: pagination.value.current,
    每页大小: pagination.value.size
  })
  try {

    const params={
      page: pagination.value.current,
      size: pagination.value.size,
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }

    console.log('调用API参数:', params)
    const res = await getCourseList(params)
    console.log('API响应:', res)
    if(res.data.code===200){
      const data=res.data.data
      console.log('后端返回的数据结构:', data)
      console.log('所有字段:', Object.keys(data))

      courses.value=data.courses|| []

      pagination.value.total=data.total|| 0
      pagination.value.pages=data.pages|| 0  //  注意这里是data.page
      pagination.value.current=data.current|| 1
      pagination.value.size=data.size|| 10


      //如果没有数据且不为第一页，回到第一页
      if(courses.value.length ===0 && pagination.value.current>1){
        pagination.value.current=1
        loadCourses()
      }
    }else{
      ElMessage.error(res.data.msg||'加载失败')
    }
    // courses.value = res.data.data || []
    // ElMessage.success('加载成功')
  } catch (error) {
    console.error('加载课程失败', error)
    ElMessage.error('加载失败'+(error.message||'网络错误'))
  } finally {
    loading.value = false
  }
}

//分页大小改变
const handleSizeChange = (size) => {
  pagination.value.size = Number(size)
  pagination.value.current = 1
  loadCourses()
}

//分页当前页改变
const handleCurrentChange = (page) => {
  pagination.value.current = page
  loadCourses()
}

//搜索
const handleSearch = () => {
  if(searchKeyword.value.trim()) {
    pagination.value.current = 1
    loadCourses()
  }else {
    ElMessage.warning('请输入搜索关键词')
  }
}

//重置搜索
const resetSearch = () => {
  if(searchKeyword.value) {
    searchKeyword.value = ''
    pagination.value.current = 1
    loadCourses()
  }
}
//查看课程详情
const viewDetail = (id) => {
  router.push(`/course/detail/${id}`)
}

//表格行样式
const tableRowClassName=({rowIndex})=>{
  return rowIndex % 2 === 1 ? 'even-row' : ''
}

//学分标签类型
const getCreditTagType = (credit) => {
  if(credit>=4)return 'danger'
  if(credit>=3)return 'warning'
  if (credit>=2) return 'success'
  return 'info'
}

//监听窗口大小变化
window.addEventListener('resize', () => {
  //布局会自动通过computed 属性计算，所以这里不需要监听窗口大小
})
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
  overflow: hidden;
}
.card-header {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
  background: #f8f9fa;
  border-bottom: 1px solid #ebeef5;
}
.el-table{
  margin-top: 0;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}
.el-table-column{
  font-size: 14px;
}
:deep(.even-row){
  background-color: #fafafa;
}
:deep(.el-table__row:hover){
  background-color: #f5f7fa;
}
/* 分页样式优化 */
:deep(.el-pagination){
  padding: 20px 0;
}
:deep(.el-pagination.is-background .btn-next),
:deep(.el-pagination.is-background .btn-prev),
:deep(.el-pagination.is-background .el-pager li) {
  background-color: #f4f4f5;
  color: #606266;
  min-width: 32px;
  height: 32px;
  line-height: 32px;
  border-radius: 4px;
}
:deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background-color: #409eff;
  color: #fff;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .course-list-container {
    padding: 10px;
    top: 80px;
  }

  .card-header > div:first-child {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }

  .card-header > div:first-child > div:last-child {
    width: 100%;
  }

  .el-input {
    width: 100% !important;
  }
}


</style>