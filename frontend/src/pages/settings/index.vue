<template>
  <div class="settings-page" v-loading="loading">
    <el-card class="card">
      <template #header>
        <div class="card-header">
          <span>总仓库配置</span>
          <el-tag size="small" :type="repoForm.configured ? 'success' : 'info'">
            {{ repoForm.configured ? '已配置' : '未配置' }}
          </el-tag>
        </div>
      </template>

      <el-form label-width="120px" class="repo-form">
        <el-form-item label="平台">
          <el-select v-model="repoForm.provider" style="width: 220px">
            <el-option label="GitHub" value="GITHUB" />
            <el-option label="Gitee" value="GITEE" />
          </el-select>
        </el-form-item>

        <el-form-item label="仓库地址">
          <el-input v-model="repoForm.repoUrl" placeholder="git@github.com:dDFFAFAFA/paper-code.git" />
        </el-form-item>

        <el-form-item label="分支">
          <el-input v-model="repoForm.branch" placeholder="main" style="max-width: 220px" />
        </el-form-item>

        <el-form-item label="目标目录">
          <el-input v-model="repoForm.targetDir" placeholder="paper-code" />
        </el-form-item>

        <el-form-item label="应用地址">
          <el-input v-model="repoForm.appBaseUrl" placeholder="https://your-domain.com" />
        </el-form-item>

        <el-form-item label="README 自动提交">
          <el-switch v-model="repoForm.autoCommitReadme" />
        </el-form-item>

        <el-form-item label="SSH Key">
          <el-input :model-value="repoForm.sshKeyPath" disabled />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="repoSaving" @click="handleSaveRepo">保存配置</el-button>
          <el-button :loading="syncing" @click="handleSync">同步仓库（Clone/Pull）</el-button>
          <el-button :loading="readmeRebuilding" @click="handleRebuildReadme">重建 README</el-button>
          <el-button @click="refreshSyncStatus">刷新状态</el-button>
        </el-form-item>
      </el-form>

      <el-divider />
      <div class="sync-status">
        <div class="status-title">最近同步状态</div>
        <el-tag :type="syncStatusType" size="small">{{ syncStatus.status || 'IDLE' }}</el-tag>
        <div class="status-line">模式：{{ syncStatus.mode || '-' }}</div>
        <div class="status-line">时间：{{ formatTime(syncStatus.syncedAt) }}</div>
        <div class="status-line" v-if="syncStatus.message">信息：{{ syncStatus.message }}</div>
      </div>
    </el-card>

    <el-card class="card">
      <template #header>
        <div class="card-header">
          <span>论文代码条目</span>
          <el-button type="primary" size="small" @click="openEntryDialog()">新增条目</el-button>
        </div>
      </template>

      <el-table :data="codeEntries" size="small" border>
        <el-table-column prop="paperTitle" label="论文" min-width="220" />
        <el-table-column label="仓库" min-width="260">
          <template #default="scope">
            <a :href="scope.row.repoUrl" target="_blank">{{ scope.row.repoUrl }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="平台" width="90" />
        <el-table-column prop="branch" label="分支" width="90" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="160">
          <template #default="scope">{{ formatTime(scope.row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button text type="primary" @click="openEntryDialog(scope.row)">编辑</el-button>
            <el-button text type="danger" @click="handleDeleteEntry(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="card">
      <template #header>
        <div class="card-header">
          <span>AI 提供商配置（DeepSeek / Qwen）</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="12" v-for="provider in aiConfigs" :key="provider.provider">
          <div class="ai-provider-box">
            <div class="provider-title">{{ provider.provider }}</div>
            <el-form label-width="90px">
              <el-form-item label="启用">
                <el-switch v-model="provider.enabled" />
              </el-form-item>
              <el-form-item label="Base URL">
                <el-input v-model="provider.baseUrl" placeholder="https://..." />
              </el-form-item>
              <el-form-item label="Model">
                <el-input v-model="provider.model" placeholder="model name" />
              </el-form-item>
              <el-form-item label="API Key">
                <el-input v-model="provider.inputApiKey" type="password" show-password placeholder="不填则保持原值" />
                <div class="api-key-mask" v-if="provider.apiKeyMasked">当前：{{ provider.apiKeyMasked }}</div>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="small" :loading="provider.saving" @click="saveAiProvider(provider)">保存</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog v-model="entryDialogVisible" title="代码条目" width="620px">
      <el-form label-width="100px">
        <el-form-item label="论文">
          <el-select v-model="entryForm.paperId" filterable style="width: 100%">
            <el-option v-for="paper in paperOptions" :key="paper.id" :label="paper.title" :value="paper.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库地址">
          <el-input v-model="entryForm.repoUrl" placeholder="https://github.com/... 或 git@..." />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="entryForm.provider" style="width: 180px">
            <el-option label="GitHub" value="GITHUB" />
            <el-option label="Gitee" value="GITEE" />
          </el-select>
        </el-form-item>
        <el-form-item label="分支">
          <el-input v-model="entryForm.branch" placeholder="main" style="max-width: 180px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="entryForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="entryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="entrySaving" @click="saveEntry">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createCodeEntry,
  deleteCodeEntry,
  getRepoConfig,
  getRepoSyncStatus,
  listCodeEntries,
  rebuildRepoReadme,
  syncRepo,
  updateCodeEntry,
  updateRepoConfig
} from '../../api/repo'
import { getAiConfigs, updateAiConfig } from '../../api/ai'
import { getPapers } from '../../api/paper'
import type {
  PaperCodeEntry,
  PaperCodeEntryPayload,
  RepoConfig,
  RepoConfigUpdateRequest,
  RepoSyncStatusResponse
} from '../../types/repo'
import type { AiProviderConfig } from '../../types/ai'

interface AiFormItem extends AiProviderConfig {
  inputApiKey?: string
  saving?: boolean
}

const loading = ref(false)
const repoSaving = ref(false)
const syncing = ref(false)
const readmeRebuilding = ref(false)
const entrySaving = ref(false)
const entryDialogVisible = ref(false)
const editingEntryId = ref<number | null>(null)

const repoForm = reactive<RepoConfig>({
  provider: 'GITHUB',
  repoUrl: '',
  branch: 'main',
  targetDir: '',
  autoCommitReadme: true,
  appBaseUrl: '',
  sshKeyPath: '',
  configured: false
})

const syncStatus = reactive<RepoSyncStatusResponse>({
  status: 'IDLE'
})

const codeEntries = ref<PaperCodeEntry[]>([])
const paperOptions = ref<Array<{ id: number; title: string }>>([])
const aiConfigs = ref<AiFormItem[]>([])

const entryForm = reactive<PaperCodeEntryPayload>({
  paperId: 0,
  repoUrl: '',
  provider: 'GITHUB',
  branch: 'main',
  description: ''
})

const syncStatusType = computed(() => {
  if (syncStatus.status === 'SUCCESS') return 'success'
  if (syncStatus.status === 'FAILED') return 'danger'
  return 'info'
})

const loadAll = async () => {
  loading.value = true
  try {
    const [cfgRes, statusRes, entriesRes, aiRes, papersRes] = await Promise.all([
      getRepoConfig(),
      getRepoSyncStatus(),
      listCodeEntries(),
      getAiConfigs(),
      getPapers({ page: 0, size: 500 })
    ])

    if (cfgRes.code === 200) {
      Object.assign(repoForm, cfgRes.data)
      if (!repoForm.branch) repoForm.branch = 'main'
      if (!repoForm.provider) repoForm.provider = 'GITHUB'
      if (repoForm.autoCommitReadme === undefined) repoForm.autoCommitReadme = true
    }

    if (statusRes.code === 200) {
      Object.assign(syncStatus, statusRes.data)
    }

    if (entriesRes.code === 200) {
      codeEntries.value = entriesRes.data
    }

    if (aiRes.code === 200) {
      aiConfigs.value = aiRes.data.map((item) => ({ ...item, inputApiKey: '', saving: false }))
    }

    if (papersRes.code === 200) {
      paperOptions.value = papersRes.data.content.map((paper) => ({ id: paper.id, title: paper.title }))
    }
  } finally {
    loading.value = false
  }
}

const handleSaveRepo = async () => {
  if (!repoForm.provider || !repoForm.repoUrl || !repoForm.branch || !repoForm.targetDir) {
    ElMessage.warning('请填写完整仓库配置')
    return
  }

  const payload: RepoConfigUpdateRequest = {
    provider: repoForm.provider,
    repoUrl: repoForm.repoUrl,
    branch: repoForm.branch,
    targetDir: repoForm.targetDir,
    autoCommitReadme: repoForm.autoCommitReadme,
    appBaseUrl: repoForm.appBaseUrl
  }

  repoSaving.value = true
  try {
    const res = await updateRepoConfig(payload)
    if (res.code === 200) {
      Object.assign(repoForm, res.data)
      ElMessage.success('仓库配置已保存')
    }
  } finally {
    repoSaving.value = false
  }
}

const handleSync = async () => {
  syncing.value = true
  try {
    const res = await syncRepo()
    if (res.code === 200) {
      Object.assign(syncStatus, res.data)
      ElMessage.success(res.data.message || '同步成功')
    }
  } finally {
    syncing.value = false
  }
}

const handleRebuildReadme = async () => {
  readmeRebuilding.value = true
  try {
    const res = await rebuildRepoReadme()
    if (res.code === 200) {
      ElMessage.success('README 已重建')
    }
  } finally {
    readmeRebuilding.value = false
  }
}

const refreshSyncStatus = async () => {
  const res = await getRepoSyncStatus()
  if (res.code === 200) {
    Object.assign(syncStatus, res.data)
  }
}

const reloadCodeEntries = async () => {
  const res = await listCodeEntries()
  if (res.code === 200) {
    codeEntries.value = res.data
  }
}

const openEntryDialog = (entry?: PaperCodeEntry) => {
  if (entry) {
    editingEntryId.value = entry.id
    Object.assign(entryForm, {
      paperId: entry.paperId,
      repoUrl: entry.repoUrl,
      provider: entry.provider,
      branch: entry.branch || 'main',
      description: entry.description || ''
    })
  } else {
    editingEntryId.value = null
    Object.assign(entryForm, {
      paperId: paperOptions.value[0]?.id || 0,
      repoUrl: '',
      provider: 'GITHUB',
      branch: 'main',
      description: ''
    })
  }
  entryDialogVisible.value = true
}

const saveEntry = async () => {
  if (!entryForm.paperId || !entryForm.repoUrl || !entryForm.provider) {
    ElMessage.warning('请完整填写条目信息')
    return
  }
  entrySaving.value = true
  try {
    if (editingEntryId.value) {
      await updateCodeEntry(editingEntryId.value, entryForm)
      ElMessage.success('条目已更新')
    } else {
      await createCodeEntry(entryForm)
      ElMessage.success('条目已创建')
    }
    entryDialogVisible.value = false
    await reloadCodeEntries()
  } finally {
    entrySaving.value = false
  }
}

const handleDeleteEntry = async (id: number) => {
  await ElMessageBox.confirm('确认删除该代码条目？', '提示', { type: 'warning' })
  await deleteCodeEntry(id)
  ElMessage.success('已删除')
  await reloadCodeEntries()
}

const saveAiProvider = async (provider: AiFormItem) => {
  provider.saving = true
  try {
    const res = await updateAiConfig(provider.provider, {
      enabled: provider.enabled,
      baseUrl: provider.baseUrl,
      model: provider.model,
      apiKey: provider.inputApiKey || undefined
    })
    if (res.code === 200) {
      Object.assign(provider, { ...res.data, inputApiKey: '' })
      ElMessage.success(`${provider.provider} 配置已保存`)
    }
  } finally {
    provider.saving = false
  }
}

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

onMounted(loadAll)
</script>

<style scoped>
.settings-page {
  max-width: 1100px;
  margin: 0 auto;
}

.card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.repo-form {
  margin-top: 8px;
}

.sync-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.status-title {
  font-size: 14px;
  font-weight: 600;
}

.status-line {
  font-size: 13px;
  color: var(--text-secondary);
}

.ai-provider-box {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
}

.provider-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.api-key-mask {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
</style>
