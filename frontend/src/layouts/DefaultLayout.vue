<template>
  <el-container>
    <el-aside :width="isCollapse ? '72px' : '256px'">
      <div class="logo">
        <img src="../assets/logo.jpg" alt="Logo" class="logo-img" />
        <span v-if="!isCollapse">ChangYe Research</span>
      </div>
      <el-menu
        :default-active="activePath"
        class="el-menu-vertical-demo"
        :collapse="isCollapse"
        router
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-menu-item index="/papers">
          <el-icon><Document /></el-icon>
          <template #title>论文管理</template>
        </el-menu-item>
        <el-menu-item index="/tags">
          <el-icon><CollectionTag /></el-icon>
          <template #title>标签管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header>
        <div class="header-left">
          <el-button @click="toggleCollapse" text circle>
            <el-icon :size="20" color="#5f6368"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          </el-button>
          <span style="margin-left: 16px; font-size: 18px; color: #5f6368;">科研工具台</span>
        </div>
        <div class="header-right">
          <el-button 
            :icon="theme === 'dark' ? Sunny : Moon" 
            circle 
            text 
            @click="toggleTheme" 
            style="margin-right: 12px;"
          />
          <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Document, CollectionTag, Fold, Expand, Moon, Sunny } from '@element-plus/icons-vue'
import { useTheme } from '../composables/useTheme'

const isCollapse = ref(false)
const route = useRoute()
const { theme, toggleTheme } = useTheme()

const activePath = computed(() => route.path)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}
</script>

<style scoped>
.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 256px;
}
.header-left {
  display: flex;
  align-items: center;
}
.logo-img {
  width: 32px;
  height: 32px;
  margin-right: 8px;
  border-radius: 8px;
  object-fit: cover;
}
</style>
