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

      <el-col :span="24">
        <el-card class="box-card summary-card">
          <template #header>
            <div class="card-header">
              <span>结构化概要</span>
              <div class="header-actions">
                <el-button size="small" :loading="summaryLoading" @click="fetchSummaries">刷新</el-button>
              </div>
            </div>
          </template>

          <el-alert
            v-if="summaryError"
            type="error"
            :title="summaryError"
            show-icon
            :closable="false"
            class="summary-alert"
          />

          <div v-if="summaryLoading" class="summary-loading">
            <el-skeleton :rows="4" animated />
          </div>

          <div v-else-if="summaryError" class="summary-retry">
            <el-button type="danger" plain @click="fetchSummaries">重试</el-button>
          </div>

          <el-empty
            v-else-if="summaries.length === 0"
            description="暂无结构化概要，去论文详情页生成第一条概要。"
          />

          <div v-else class="summary-list">
            <div v-for="item in summaries" :key="`${item.paperId}-${item.summaryId || 'latest'}`" class="summary-item">
              <div class="summary-main">
                <div class="summary-title">{{ item.title || item.paperTitle || `论文 #${item.paperId}` }}</div>
                <div class="summary-meta">
                  <el-tag size="small" :type="getSummaryStatusType(item.status)">
                    {{ getSummaryStatusLabel(item.status) }}
                  </el-tag>
                  <span class="summary-time">时间：{{ formatTime(item.generatedAt) }}</span>
                </div>
              </div>
              <div class="summary-item-actions">
                <el-button
                  text
                  type="primary"
                  :loading="summaryDownloadLoading[item.paperId]"
                  :disabled="!canDownloadSummary(item)"
                  @click="handleDownloadSummary(item)"
                >
                  下载 .md
                </el-button>
                <el-button text type="primary" @click="goPaperDetail(item.paperId)">进入详情</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHealth } from '../../api/health'
import { downloadPaperSummary, getHomeSummaries } from '../../api/paper'
import type { HomeSummaryItem, PaperSummaryStatus } from '../../types/paper'
import { ElMessage } from 'element-plus'

const router = useRouter()
const healthStatus = ref('')
const serverTime = ref('')
const summaryLoading = ref(false)
const summaryError = ref('')
const summaries = ref<HomeSummaryItem[]>([])
const summaryDownloadLoading = ref<Record<number, boolean>>({})

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

const getErrorMessage = (error: unknown, fallback: string) => {
  const apiMsg = (error as any)?.response?.data?.message
  const msg = (typeof apiMsg === 'string' && apiMsg.trim()) || (error as any)?.message
  return typeof msg === 'string' && msg.trim() ? msg.trim() : fallback
}

const fetchSummaries = async () => {
  summaryLoading.value = true
  summaryError.value = ''
  try {
    const res = await getHomeSummaries(10)
    if (res.code === 200) {
      summaries.value = res.data || []
    }
  } catch (error) {
    summaryError.value = getErrorMessage(error, '获取结构化概要失败，请稍后重试')
  } finally {
    summaryLoading.value = false
  }
}

const getSummaryStatusLabel = (status: PaperSummaryStatus) => {
  if (status === 'SUCCESS') return '成功'
  if (status === 'FAILED') return '失败'
  if (status === 'GENERATING' || status === 'QUEUED' || status === 'PENDING') return '进行中'
  return status
}

const getSummaryStatusType = (status: PaperSummaryStatus) => {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'GENERATING' || status === 'QUEUED' || status === 'PENDING') return 'warning'
  return 'info'
}

const canDownloadSummary = (item: HomeSummaryItem) => {
  return item.status === 'SUCCESS'
}

const handleDownloadSummary = async (item: HomeSummaryItem) => {
  if (!canDownloadSummary(item)) {
    ElMessage.warning('当前概要尚未生成完成，暂不可下载')
    return
  }

  summaryDownloadLoading.value[item.paperId] = true
  try {
    const blob = await downloadPaperSummary(item.paperId)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `paper_${item.paperId}_summary.md`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('概要已下载')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '下载概要失败，请稍后重试'))
  } finally {
    summaryDownloadLoading.value[item.paperId] = false
  }
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

const goPaperDetail = (id: number) => {
  router.push(`/papers/${id}`)
}

onMounted(() => {
  fetchHealth()
  fetchSummaries()
})
</script>

<style scoped>
.welcome-card {
  margin-bottom: 20px;
}

.summary-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.summary-alert {
  margin-bottom: 12px;
}

.summary-retry {
  margin: 12px 0;
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px 12px;
}

.summary-main {
  min-width: 0;
}

.summary-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 6px;
}

.summary-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary-time {
  color: var(--text-secondary);
  font-size: 12px;
}

.summary-item-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
