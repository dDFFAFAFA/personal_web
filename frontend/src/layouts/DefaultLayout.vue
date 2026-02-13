<template>
  <el-container>
    <el-aside :width="isCollapse ? '64px' : '220px'">
      <div class="logo">
        <span v-if="!isCollapse">ChangYe Research</span>
        <span v-else>CY</span>
      </div>
      <el-menu
        active-text-color="#409EFF"
        background-color="#16213e"
        class="el-menu-vertical-demo"
        default-active="/"
        text-color="#fff"
        :collapse="isCollapse"
        router
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-menu-item index="/papers" disabled>
          <el-icon><Document /></el-icon>
          <template #title>论文管理 (Phase 1)</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-left">
          <el-button @click="toggleCollapse" text>
            <el-icon :size="20"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <span style="margin-left: 10px; font-weight: 600;">ChangYe 科研工具台</span>
        </div>
        <div class="header-right">
          <!-- User info or actions could go here -->
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { HomeFilled, Document, Fold, Expand } from '@element-plus/icons-vue'

const isCollapse = ref(false)
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}
</script>

<style scoped>
.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 220px;
  min-height: 400px;
}
</style>
