import {createStore} from 'vuex'
import {login,getUserInfo} from "~/api/manager"
import {removeToken, setToken} from "~/composables/auth.js";

const store = createStore({
  state: {
      // 用户信息
      user:{},
      //侧边宽度
      asideWidth: '250px'
  },
  mutations: {
      // 设置用户信息
      SET_USERINFO(state, userResponse){
          console.log('SET_USERINFO原始数据:', userResponse)
          // 只保存data部分到state.user
          if (userResponse && userResponse.data) {
              state.user = userResponse.data  // ✅ 只保存data
          } else {
              state.user = userResponse
          }
          console.log('设置后的state.user:', state.user)
      },
      //展开/收起侧边栏
      handleAsideWidth(state){
          state.asideWidth = state.asideWidth === '250px' ? '64px' : '250px'
      }
  }
  ,
  actions: {
      // 登录
      //     login({commit},{username,password}){
      //         return new Promise((resolve,reject)=>{
      //             login(username,password).then(res=>{
      //                 // 登录成功
      //                 setToken(res.data.data.token)
      //                 resolve(res.data)
      //             }).catch(err=>{reject(err)})
      //         })
      //     },
      login({ commit }, { username, password }) {
          return login(username, password)
              .then(res => {
                  // 只进成功分支
                  if (res.data.code === 200) {
                      setToken(res.data.data.token);   // ✅ 只读成功分支
                      return res.data;                 // ✅ 返回 2 层 data
                  }throw new Error(res.data.msg || '登录失败')
              });
      },
      // 获取用户信息
      getUserInfo(context){
          return new Promise((resolve,reject)=>{
              getUserInfo().then(res=>{
                  context.commit('SET_USERINFO',res.data)
                  resolve(res.data)
              }).catch(err=>{
                  reject(err)
              })
          })
      },

      //退出登录
      logout({commit}){
              // 1. 删除token
          removeToken()
              // 2. 清除当前用户状态vuex
          commit('SET_USERINFO',{})
      }
  },
})
export default store