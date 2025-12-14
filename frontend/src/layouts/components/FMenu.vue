<template>
  <div class="f-menu" :style="{width:$store.state.asideWidth}">
    <el-menu :default-active="defaultActive" unique-opened :collapse="isCollapse" default-active="2"
             class="border-0" @select="handleSelect" :collapse-transition="false">

      <!--首页（所有用户）-->
      <el-menu-item index="/">
        <el-icon><HomeFilled /></el-icon>
        <span>首页</span>
      </el-menu-item>

      <!--动态生成菜单-->
      <template v-for="(item,index) in filteredMenus" :key="index">
        <el-sub-menu v-if="item.children &&item.children.length>0"
                     :index="item.name">
          <template #title>
            <el-icon>
              <component :is="item.icon"></component>
            </el-icon>
            <span>{{item.name}}</span>
          </template>
          <el-menu-item v-for="(item2,index2) in item.children"
                       :key="index2"
                       :index="item2.path">
            <el-icon>
              <component :is="item2.icon"></component>
            </el-icon>
            <span>{{item2.name}}</span>
          </el-menu-item>
        </el-sub-menu>
        <el-menu-item v-else
                      :index="item.path">
          <el-icon>
            <component :is="item.icon"></component>
          </el-icon>
          <span>{{item.name}}</span>
        </el-menu-item>
      </template>
    </el-menu>
  </div>
</template>
<script setup>
import {useStore} from 'vuex'
import {useRouter,useRoute} from "vue-router";
import {computed,ref} from "vue";

const store = useStore()
const router = useRouter()
const route = useRoute()

//默认选中
const defaultActive = computed(() => {
  return route.path
})

//是否折叠
const isCollapse = computed(() => !(store.state.asideWidth==='250px'))
const handleSelect = (index) => {
  router.push( index)
}
//获取用户角色
const userRole =computed(()=>{
  return store.state.user?.roleId || ''
})
//菜单配置
const allMenus= {
  //学生菜单
  '1': [
    {
      "name": "学习管理",
      "icon": "promotion",
      "children": [
        {
          "name": "课程列表", "icon": "list", "path": "/course/list"
        },
        {
          "name": "推荐课程", "icon": "StarFilled", "path": "/study/recommend"
        },
        {
          "name": "我的学习", "icon": "reading", "path": "/study/my-learning"
        }
      ]
    }
  ],
  //教师菜单
  '2': [
    {
      "name": "教学管理",
      "icon": "grid",
      "children": [
        {
          "name": "发布课程", "icon": "list", "path": "/teach/publish-course"
        },
        {
          "name": "我的课程", "icon": "reading", "path": "/teach/my-course"
        }
      ]
    }
  ],
  //管理员菜单
  '3': [
    {
      "name": "系统管理",
      "icon": "operation",
      "children": [
        {
          "name": "用户管理", "icon": "user", "path": "/admin/users/list"
        },
        {
          "name": "课程管理", "icon": "document", "path": "/admin/courses/list"
        }
      ]
    }
  ]
}
// 修改过滤菜单逻辑 - 直接使用 roleId
const filteredMenus = computed(() => {
  const roleId = store.state.user?.roleId
  if (!roleId) return []

  return allMenus[roleId] || []
})

import { onMounted } from 'vue'
import {HomeFilled} from "@element-plus/icons-vue";

onMounted(() => {
  console.log('=== FMenu 详细调试 ===')
  console.log('1. store.state:', JSON.parse(JSON.stringify(store.state)))
  console.log('2. store.state.user:', store.state.user)
  console.log('3. userRole.value:', userRole.value)
  console.log('4. typeof userRole.value:', typeof userRole.value)
  console.log('5. allMenus keys:', Object.keys(allMenus))
  console.log('6. allMenos for role:', allMenus[userRole.value])
  console.log('7. filteredMenus.value:', filteredMenus.value)
})

</script>
<style scoped>
.f-menu{
  transition: all 0.2s;
  width:250px;
  top:64px;
  bottom: 0;
  left: 0;
  overflow-y: auto;
  overflow-x: hidden;
  @apply shadow-md fixed bg-light-50;
}
.f-menu::-webkit-scrollbar{
  width: 0;
}
</style>