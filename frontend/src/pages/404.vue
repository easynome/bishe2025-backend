<template>
    <el-result icon="warning" title="404提示" subTitle="你找到的页面走丢了~">
      <template #extra>
        <el-button type="primary" size="medium" @click="$router.push('/')">回到首页</el-button>
      </template>
    </el-result>
</template>