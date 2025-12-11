<template>
  <div class="f-header">
    <span class="logo">
      <el-icon class="mr-1"><eleme-filled/></el-icon>
      个性化学习推荐系统
    </span>
    <el-icon class="icon-btn" @click="$store.commit('handleAsideWidth')">
      <fold v-if="$store.state.asideWidth==='250px'"/>
      <Expand v-else/>
    </el-icon>
    <el-tooltip effect="dark" content="刷新" placement="bottom">
      <el-icon class="icon-btn" @click="handleRefresh"><refresh/></el-icon>
    </el-tooltip>
    <div class="ml-auto flex items-center">
      <el-tooltip effect="dark" content="全屏" placement="bottom">
        <el-icon class="icon-btn" @click="toggle">
          <full-screen v-if="!isFullscreen"/>
          <aim v-else/>
        </el-icon>
      </el-tooltip>
      <el-dropdown class="dropdown" @command="handleCommand">
        <span class ="flex items-center text-light-50">
          <el-avatar class="mr-2" :size="25" :src="$store.state.user.avatar"/>
            {{$store.state.user.username}}
            <el-icon class ="el-icon--right">
              <arrow-down/>
            </el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="rePassword">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
<!--  <el-drawer-->
<!--    v-model="showDrawer"-->
<!--    title="修改密码"-->
<!--    size="45%"-->
<!--    :close-on-click-modal="false">-->

<!--  </el-drawer>-->
  <form-drawer ref="formDrawerRef" title="修改密码" destroyOnClose @submit="handleLogin">
    <el-form ref="ruleFormRef" :model="form" :rules="rules" label-width="80px" size="default" @keyup.enter="handleLogin">
      <el-form-item prop="oldpassword" label="旧密码">
        <el-input
            v-model="form.oldpassword"
            placeholder="请输入旧密码"
        />
      </el-form-item>
      <el-form-item prop="password" label="新密码">
        <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
        />
      </el-form-item>
      <el-form-item prop="repassword" label="确认密码">
        <el-input
            v-model="form.repassword"
            type="password"
            placeholder="请确认密码"
            show-password
        />
      </el-form-item>
    </el-form>
<!--    <div  style="height: 1000px"></div>-->
  </form-drawer>
</template>
<script setup>

import {Aim, ArrowDown, ElemeFilled, Expand, Fold, FullScreen, Lock, Refresh, User} from "@element-plus/icons-vue";
import {useFullscreen} from '@vueuse/core'
import FormDrawer from "~/components/FormDrawer.vue";
import {useRePassword,useLogout} from "~/composables/useManager.js";

const {
  //是否全屏
  isFullscreen,
  //切换全屏
  toggle
} = useFullscreen()
const {
  form,
  formDrawerRef,
  ruleFormRef,
  handleLogin,
  openRePasswordForm
}= useRePassword()

const {
  handeLogout
} = useLogout()

const validatePass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}
//表单验证逻辑
const rules = {
  oldpassword: [
    {
      required: true,
      message: '旧密码不能为空',
      trigger: 'blur'
    },
  ],
  password: [
    {
      required: true,
      message: '新密码不能为空',
      trigger: 'blur'
    },
  ],
  repassword: [
    {
      required: true,
      validator: validatePass,
      trigger: 'blur'
    },
  ]
}



const handleCommand = (command) => {
  switch ( command){
    case 'logout':
      handeLogout();
      break;
    case 'rePassword':
      // showDrawer.value = true;
        openRePasswordForm()
      break;
  }
};

//刷新
const handleRefresh = () => {
  location.reload()
  //或者 router.go()
};

</script>
<style>
  .f-header{
    @apply flex items-center bg-indigo-700 text-light-50 fixed top-0 left-0
    right-0
    height:64px
  }
  .logo{
    width: 250px;
    @apply flex justify-center items-center text-xl font-thin;
  }
  .icon-btn{
    @apply flex justify-center items-center;
    width: 42px;
    height: 64px;
    cursor: pointer;
  }
  .icon-btn:hover{
    @apply bg-indigo-600;
  }
  .f-header .dropdown{
    height: 64px;
    cursor: pointer;
    @apply flex justify-center items-center mx-5;
  }
</style>