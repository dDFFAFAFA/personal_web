<template>
  <div class="home-container" data-testid="home-page">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="box-card welcome-card">
          <template #header>
            <div class="card-header">
              <span>👋 欢迎回来，ChangYe</span>
            </div>
          </template>
          <div class="text item">
            这里是您的个人科研工具台。当前系统状态：
            <el-tag :type="healthStatus === 'UP' ? 'success' : 'danger'" effect="dark">
              {{ healthStatus || 'Checking...' }}
            </el-tag>
            <div v-if="serverTime" style="margin-top: 10px; font-size: 12px; color: #999;">
              Server Time: {{ serverTime }}
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getHealth } from '../../api/health'
import { ElMessage } from 'element-plus'

const healthStatus = ref('')
const serverTime = ref('')

const fetchHealth = async () => {
  try {
    const res = await getHealth()
    if (res.code === 200) {
      healthStatus.value = res.data.status
      serverTime.value = res.data.timestamp
    }
  } catch (error) {
    console.error('Health check failed', error)
    healthStatus.value = 'DOWN'
    ElMessage.error('无法连接到后端服务')
  }
}

onMounted(() => {
  fetchHealth()
})
</script>

<style scoped>
.welcome-card {
  margin-bottom: 20px;
}
</style>
