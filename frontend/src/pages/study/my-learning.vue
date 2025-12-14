<template>
  <div class="my-learning">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>我的学习中心</h2>
          <el-tag type="primary" v-if="ratedCourses.length > 0">
            已评分 {{ ratedCourses.length }} 门课程
          </el-tag>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 已评分课程 -->
        <el-tab-pane label="已评分课程" name="rated">
          <div v-if="loading" class="loading-container">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>

          <div v-else-if="ratedCourses.length === 0" class="empty-state">
            <el-icon size="60" color="#909399"><Star /></el-icon>
            <h3>暂无评分记录</h3>
            <p class="hint-text">您还没有对任何课程进行评分</p>
            <p class="hint-text">快去发现感兴趣的课程并评分，获取个性化推荐吧！</p>
            <el-button type="primary" @click="goToCourseList" style="margin-top: 20px;">
              <el-icon><Collection /></el-icon>
              去课程列表评分
            </el-button>
          </div>

          <div v-else>
            <!-- 评分统计 -->
            <div class="stats-card">
              <el-row :gutter="20">
                <el-col :span="6">
                  <div class="stat-item">
                    <div class="stat-value">{{ ratedCourses.length }}</div>
                    <div class="stat-label">评分总数</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="stat-item">
                    <div class="stat-value">{{ avgRating.toFixed(1) }}</div>
                    <div class="stat-label">平均评分</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="stat-item">
                    <div class="stat-value">{{ highRatingCount }}</div>
                    <div class="stat-label">高评分(≥4星)</div>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div class="stat-item">
                    <div class="stat-value">{{ recentRatedCount }}</div>
                    <div class="stat-label">最近7天</div>
                  </div>
                </el-col>
              </el-row>
            </div>

            <!-- 评分列表 -->
            <el-table :data="ratedCourses" style="width: 100%" class="rating-table">
              <el-table-column prop="courseName" label="课程名称"  align="center" header-align="center">
                <template #default="scope">
                  <div class="course-name-cell">
                    <span class="course-name">{{ scope.row.courseName }}</span>
                    <el-tag size="small" v-if="scope.row.credit" type="info">
                      学分: {{ scope.row.credit }}
                    </el-tag>
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="我的评分"  align="center" header-align="center" >
                <template #default="scope">
                  <div class="rating-display">
                    <el-rate v-model="scope.row.score" disabled class="rating-stars" />
                    <span class="rating-text">{{ getRatingText(scope.row.score) }}</span>
                    <span class="rating-value">{{ scope.row.score }}星</span>
                  </div>
                </template>
              </el-table-column>

              <el-table-column  label="评分时间" align="center" header-align="center">
                <template #default="scope">
                  <div @click="console.log('时间数据：', scope.row)">
                  {{ formatDate(scope.row.createdAt) }}
                  </div>
                </template>
              </el-table-column>

              <el-table-column label="操作"  align="center" header-align="center">
                <template #default="scope">
                  <el-button type="primary" link @click="viewCourseDetail(scope.row.courseId)">
                    查看课程
                  </el-button>
                  <el-button type="info" link @click="reRateCourse(scope.row.courseId)">
                    重新评分
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 评分分布图（简单版本） -->
            <div class="rating-distribution" v-if="ratingDistribution.length > 0">
              <h3>评分分布</h3>
              <div class="distribution-bars">
                <div v-for="item in ratingDistribution" :key="item.rating" class="distribution-item">
                  <div class="dist-rating">{{ item.rating }}星</div>
                  <div class="dist-bar">
                    <div class="dist-fill" :style="{ width: item.percentage + '%' }"></div>
                  </div>
                  <div class="dist-count">{{ item.count }}门</div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 推荐历史 -->
        <el-tab-pane label="推荐历史" name="recommendations">
          <div class="coming-soon">
            <el-icon size="50"><Clock /></el-icon>
            <h3>功能开发中</h3>
            <p>推荐历史记录功能即将上线</p>
            <p class="hint-text">您可以先查看当前的个性化推荐</p>
            <el-button type="primary" @click="goToRecommend" style="margin-top: 20px;">
              <el-icon><Star /></el-icon>
              查看个性化推荐
            </el-button>
          </div>
        </el-tab-pane>

        <!-- 学习统计 -->
        <el-tab-pane label="学习统计" name="statistics">
          <div class="coming-soon">
            <el-icon size="50"><DataAnalysis /></el-icon>
            <h3>功能开发中</h3>
            <p>学习统计功能即将上线</p>
            <p class="hint-text">我们将为您提供详细的学习数据分析</p>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserRatedCourses } from '~/api/manager'  // 需要创建这个API
import { ElMessage } from 'element-plus'
import {
  Loading,
  Star,
  Collection,
  Clock,
  DataAnalysis
} from '@element-plus/icons-vue'

const router = useRouter()
const activeTab = ref('rated')

// 数据
const ratedCourses = ref([])
const loading = ref(false)

// 加载评分记录
const loadRatedCourses = async () => {
  // console.log('🚀 loadRatedCourses开始执行')
  loading.value = true
  try {
    const res = await getUserRatedCourses()
    // console.log('API完整响应',JSON.stringify( res.data,null,2))

    if(res.data&&res.data.code === 200){
      ratedCourses.value = res.data.data || []
      // console.log('第一条记录完整结构', ratedCourses.value[0]?JSON.stringify(ratedCourses.value[0],null,2):'无')
      if(ratedCourses.value.length>0){
        // const firstRecord = ratedCourses.value[0]
        // console.log("createdAt字段值", firstRecord.ratedAt)
        // console.log("所有字段",Object.keys(firstRecord))
        ElMessage.success(`加载了${ratedCourses.value.length}条评分记录`)
      }
    }else{
      ElMessage.error(res.data?.msg ||'加载失败')
    }
  }catch ( error){
    console.error('加载评分记录失败:', error)
    ElMessage.error('加载评分记录失败')
    ratedCourses.value = []
  }finally {
    loading.value = false
  }
    // TODO: 需要创建 getUserRatedCourses API
    // 暂时使用模拟数据
    // const mockData = [
    //   {
    //     courseId: 1,
    //     courseName: 'Java基础入门',
    //     score: 4,
    //     credit: 2,
    //     ratedAt: '2025-11-25T10:30:00'
    //   },
    //   {
    //     courseId: 3,
    //     courseName: 'Spring Boot实战',
    //     score: 5,
    //     credit: 3,
    //     ratedAt: '2025-11-24T14:20:00'
    //   },
    //   {
    //     courseId: 5,
    //     courseName: 'MySQL数据库',
    //     score: 3,
    //     credit: 2,
    //     ratedAt: '2025-11-23T09:15:00'
    //   }
    // ]
    //   ratedCourses.value = mockData
    //   ElMessage.success('加载评分记录成功')
    // } catch (error) {
    //   console.error('加载评分记录失败:', error)
    //   ElMessage.error('加载评分记录失败')
    //   // 如果API未实现，使用空数组
    //   ratedCourses.value = []
    // } finally {
    //   loading.value = false
    // }
    // 详细调试
    // console.log('=== 评分记录调试信息 ===')
    // console.log('1. 完整响应对象:', res)
    // console.log('2. response.data:', res.data)
    // console.log('3. response.data.code:', res.data?.code)
    // console.log('4. response.data.msg:', res.data?.msg)
    // console.log('5. response.data.data:', res.data?.data)
    // console.log('6. 数据类型:', typeof res.data?.data)
    // console.log('7. 是否是数组:', Array.isArray(res.data?.data))

}

// 计算属性
const avgRating = computed(() => {
  if (ratedCourses.value.length === 0) return 0
  const sum = ratedCourses.value.reduce((total, item) => total + item.score, 0)
  return sum / ratedCourses.value.length
})

const highRatingCount = computed(() => {
  return ratedCourses.value.filter(item => item.score >= 4).length
})

const recentRatedCount = computed(() => {
  const sevenDaysAgo = new Date()
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7)

  return ratedCourses.value.filter(item => {
    const ratedDate = new Date(item.createdAt)
    return ratedDate >= sevenDaysAgo
  }).length
})

const ratingDistribution = computed(() => {
  const distribution = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 }
  ratedCourses.value.forEach(item => {
    distribution[item.score]++
  })

  return Object.keys(distribution).map(rating => ({
    rating: parseInt(rating),
    count: distribution[rating],
    percentage: ratedCourses.value.length > 0
        ? (distribution[rating] / ratedCourses.value.length * 100).toFixed(1)
        : 0
  }))
})

// 工具函数
const getRatingText = (score) => {
  const texts = ['很差', '较差', '一般', '较好', '很好']
  return texts[score - 1] || '未知'
}

const formatDate = (dateString) => {
  // console.log('=== 日期格式化调试 ===')
  // console.log('原始数据:', dateString)
  // console.log('数据类型:', typeof dateString)
  //
  // console.log('formatDate输入:', dateString, '类型:', typeof dateString)

  if (!dateString) {
    // console.log('dateString为空:', dateString)
    return '未知时间'
  }

  try {
    // 尝试多种日期格式解析
    let dateObj
    if (typeof dateString === 'string') {
      // 处理 ISO 格式日期字符串
      if (dateString.includes('T')) {
        dateObj = new Date(dateString)
      } else {
        // 处理其他格式的日期字符串
        dateObj = new Date(dateString.replace(/-/g, '/'))
      }
    } else if (typeof dateString === 'number') {
      // 处理时间戳
      dateObj = new Date(dateString)
    } else if (dateString instanceof Date) {
      dateObj = dateString
    }

    // 验证日期有效性
    if (!dateObj || isNaN(dateObj.getTime())) {
      console.log('无效日期:', dateString)
      return '无效时间'
    }

    // 格式化日期
    const year = dateObj.getFullYear()
    const month = String(dateObj.getMonth() + 1).padStart(2, '0')
    const day = String(dateObj.getDate()).padStart(2, '0')
    const hours = String(dateObj.getHours()).padStart(2, '0')
    const minutes = String(dateObj.getMinutes()).padStart(2, '0')

    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch (error) {
    console.error('日期格式化错误:', error)
    return '格式错误'
  }
}

// 导航函数
const goToCourseList = () => {
  router.push('/course/list')
}

const goToRecommend = () => {
  router.push('/study/recommend')
}

const viewCourseDetail = (courseId) => {
  router.push(`/course/detail/${courseId}`)
}

const reRateCourse = (courseId) => {
  viewCourseDetail(courseId)
}

onMounted(() => {
  loadRatedCourses()
})
</script>

<style scoped>
.my-learning {
  position: fixed;
  top: 96px;
  left: v-bind('$store.state.asideWidth');
  right: 0;
  bottom: 0;
  transition: margin-left 0.2s;
  overflow-y: auto;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container {
  text-align: center;
  padding: 50px;
  color: #909399;
}

.loading-container .el-icon {
  margin-right: 10px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.hint-text {
  color: #909399;
  margin-top: 10px;
}

.stats-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
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

.rating-table {
  margin-top: 20px;
}

.course-name-cell {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.course-name {
  font-weight: 500;
}

.rating-display {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rating-stars {
  margin-right: 10px;
}

.rating-text {
  color: #606266;
  min-width: 40px;
}

.rating-value {
  color: #E6A23C;
  font-weight: bold;
}

.rating-distribution {
  margin-top: 40px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
}

.distribution-bars {
  margin-top: 15px;
}

.distribution-item {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.dist-rating {
  width: 50px;
  color: #606266;
}

.dist-bar {
  flex: 1;
  height: 20px;
  background: #e4e7ed;
  border-radius: 10px;
  overflow: hidden;
  margin: 0 15px;
}

.dist-fill {
  height: 100%;
  background: linear-gradient(90deg, #409EFF, #67C23A);
  transition: width 0.5s ease;
}

.dist-count {
  width: 60px;
  text-align: right;
  color: #909399;
}

.coming-soon {
  text-align: center;
  padding: 60px 20px;
  color: #606266;
}

.coming-soon .el-icon {
  margin-bottom: 20px;
}
</style>