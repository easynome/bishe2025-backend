<template>
    <el-button @click="mock">发一个登录请求</el-button>
  </template>
  
  <script setup>
  import axios from 'axios'
  
  async function mock() {
    try {
      const res = await axios.post('http://localhost:8080/api/login', {
        userName: 'u01',   // ② 照抄后端字段
        passWord: '123456'
      })
      console.log('返回结构', res.data)
    } catch (e) {
      console.error('出错', e.response?.data || e.message)
    }
  }
  </script>