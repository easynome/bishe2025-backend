import axios from 'axios'
import {ElNotification} from "element-plus";
import {
  setToken,
  getToken,
  removeToken
} from "~/composables/auth"

const service = axios.create({
  baseURL:"/api", // api 的 base_url
  timeout: 5000 // request timeout
})
// request 拦截器
service.interceptors.request.use(config => {
  const token=getToken()
  console.log('>>> 拦截器读token', token)   // 裸值
  if (token && token !== 'undefined') {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
}, error => Promise.reject(error))

// response 拦截器
service.interceptors.response.use(function (response){
    return response;
},function (error){
  console.error('登录请求出错:', error)
  const errorMsg = error.response?.data?.msg || "网络异常"
  ElNotification({
    title: '错误',
    message: errorMsg,
    // message: error.response?.data?.msg||"网络异常",
    type: 'error',
    duration: 2000
})
  return Promise.reject(error);

});

export default service