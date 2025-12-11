<template>
  <div class="f-tag-list" :style="{left:$store.state.asideWidth}">
    <el-tabs v-model="activeTabs" type="card" calss="flex-1"
    closable @tab-remove="removeTab" style="min-width: 100px"
    @tab-change="changeTab">
      <el-tab-pane v-for="item in tabList" :key="item.path"
                   :label="item.title" :name="item.path">
<!--        <iframe :src="item.path" frameborder="0" width="100%" height="100%"></iframe>-->
      </el-tab-pane>
    </el-tabs>
    <span class="tag-btn">
      <el-dropdown @command="handleClose">
        <span class="el-dropdown-link">
          <el-icon >
            <arrow-down />
          </el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="clearAll">关闭所有</el-dropdown-item>
            <el-dropdown-item command="closeOther">关闭其他</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </span>
  </div>
</template>
<script setup>
import {useTabList} from "~/composables/useTabList.js";

const {
    ArrowDown,
    tabList,
    activeTabs,
    changeTab,
    removeTab,
    handleClose
} = useTabList()

</script>
<style scoped>
.f-tag-list{
  @apply fixed  flex items-center px-2;
  top: 64px;
  right: 0;
  height: 44px;
  z-index: 100;
}
.tag-btn{
  @apply bg-white rounded ml-auto flex items-center justify-center px-2;
  height: 32px;
}
:deep(.el-tabs__header){
  border: 0!important;
  @apply mb-0;
}
:deep(.el-tabs__nav){
  border: 0!important;
}
:deep(.el-tabs__item){
  @apply bg-gray-100
  border-right: 1px solid #dcdfe6 !important;
  height: 32px;
  line-height: 32px;
  @apply bg-white mx-1 rounded;
}
:deep(.el-tabs__item:last-child) {
  border-right: none !important; /* 最后一个标签不显示竖线 */
}
:deep(.el-tabs__nav-next,.el-tabs__nav-prev){
  line-height: 32px;
  height: 32px;
}

</style>