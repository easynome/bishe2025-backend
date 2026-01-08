import axios from '~/axios.js'

/*********** 用户相关 ***********/
// 登录
export function login(username, password) {
  return axios.post('/login', {
    username,
    password
  })
}
// 获取用户信息
export function getUserInfo() {
  return axios.get('/user/info')
}
// 注册
export function register(username, password, confirmPassword) {
  return axios.post('/register', {
    username,
    password,
    confirmPassword
  })
}
// 登出
export function logout() {
  return axios.post('/user/logout')
}
// 修改用户密码
export function updatePassword(data) {
  return axios.post('/user/update_password', data)
}

/********** 课程相关 ***********/
// 获取所有课程
export function getAllCourses(params) {
  return axios.get('/course/all', { params })
}
// 获取课程列表
export function getCourseList(params = {}) {
  return axios.get('/course/list', {
    params: {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword
    }
  })
}
// 评价课程
export function rateCourse(id, score) {
  return axios.post(`/course/${id}/rate`, null,
      { params: { score:score } })
}

/*********** 教师管理课程相关 ***********/
// 发布课程
export function publishCourse(courseData) {
  return axios.post('/course/teacher/add', courseData)
}

// 获取教师课程列表
export function getMyCoursesForTeacher(params = {}) {
  return axios.get('/course/teacher/my-courses', {
    params: params  // 🔧 关键：添加params参数
  })
}
// 修改课程状态
export function updateCourseStatusByTeacher(id, status) {
  return axios.post(`/course/${id}/status`, null,
      { params: { status:status } })
}

export function getCourseStudents(id) {
  return axios.get(`/course/${id}/students`)
}
// 获取课程详情
export function getCourseDetailByTeacher(id) {
  return axios.get(`/course/${id}`)
}

/*********** 学生课程相关 ***********/
export function getMyCoursesForStudent() {
  return axios.get('/study/my-courses')
}

// 获取已评分课程
export function getUserRatedCourses() {
  return axios.get('/study/my-ratings')
}

/*********** 管理员相关 ***********/
export function getAdminUsersList(params={}) {
  return axios.get('/admin/users/list', {
    params: {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword,
      roleId: params.roleId
    }
  })
}

// 更新用户角色
export function updateUserRole(id, roleId) {
  return axios.post(`/admin/users/${id}/role`, roleId,{
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

// 更新用户状态
export function updateUserStatus(id, status) {
  return axios.post(`/admin/users/${id}/status`, status,{
    headers: {
      'Content-Type': 'application/json'
    }
  })
}
// 获取课程列表
export function getAdminCoursesList(params={}) {
  return axios.get('/course/admin/courses/list',{
    params: {
      page: params.page || 1,
      size: params.size || 10,
      keyword: params.keyword,
      status: params.status
    }
  })
}
// 获取课程详情
export function getCourseDetailForAdmin(id) {
  return axios.get(`/course/admin/courses/${id}`)
}
// 修改课程状态
export function updateCourseStatusByAdmin(id, status) {
  return axios.post(`/course/admin/courses/${id}/status`, null,{
    params: { status }
  })
}
// 修改课程
export function updateCourseByAdmin(id, courseData) {
  return axios.put(`/course/admin/courses/${id}`, courseData)
}

export function getDashboardData(){
  return axios.get('/course/data')
}
/*********** 推荐课程相关 ***********/
// 获取推荐课程
export function getRecommendations(num = 10) {
  return axios.get('/recommend', {
    params: {num}
  })
}
