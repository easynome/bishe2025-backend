<template>
    <el-container>
      <el-header>
        <f-header></f-header>
      </el-header>
      <el-container>
        <el-aside :width="$store.state.asideWidth">
          <f-menu></f-menu>
        </el-aside>
        <el-main>
          <f-tag-list></f-tag-list>
          <router-view v-slot="{ Component }">
            <keep-alive :max="10">
              <component :is="Component"></component>
            </keep-alive>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
</template>
<script setup>
import FHeader from "~/layouts/components/FHeader.vue"
import FTagList from "~/layouts/components/FTagList.vue"
import FMenu from "~/layouts/components/FMenu.vue"
</script>
<style scoped>
.el-aside{
  transition: all 0.2s;
}

</style>