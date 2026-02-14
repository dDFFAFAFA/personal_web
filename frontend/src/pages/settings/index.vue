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
          <el-button :loading="syncing" :disabled="syncDisabled" @click="handleSync">同步仓库（Clone/Pull）</el-button>
          <el-button :loading="readmeRebuilding" :disabled="readmeDisabled" @click="handleRebuildReadme">重建 README</el-button>
          <el-button :loading="syncStatusLoading" :disabled="refreshStatusDisabled" @click="refreshSyncStatus">刷新状态</el-button>
        </el-form-item>
        <div class="status-line" v-if="repoActionHint">{{ repoActionHint }}</div>
      </el-form>

      <el-divider />
      <el-alert
        v-if="repoFeedback.message"
        :title="repoFeedback.title"
        :description="repoFeedback.message"
        :type="repoFeedback.type"
        :closable="false"
        show-icon
        class="repo-feedback"
      />
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
            <el-link
              v-if="toBrowserRepoUrl(scope.row.repoUrl)"
              :href="toBrowserRepoUrl(scope.row.repoUrl)"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ scope.row.repoUrl }}
            </el-link>
            <span v-else class="invalid-link-text">{{ scope.row.repoUrl }}</span>
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
        <el-form-item label="论文" :error="entryPaperError">
          <el-select v-model="entryForm.paperId" filterable style="width: 100%">
            <el-option v-for="paper in paperOptions" :key="paper.id" :label="paper.title" :value="paper.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库地址" :error="entryRepoUrlError">
          <el-input v-model="entryForm.repoUrl" placeholder="https://github.com/... 或 git@..." />
        </el-form-item>
        <el-form-item label="平台">
          <el-select v-model="entryForm.provider" style="width: 180px">
            <el-option label="GitHub" value="GITHUB" />
            <el-option label="Gitee" value="GITEE" />
          </el-select>
        </el-form-item>
        <el-form-item label="分支" :error="entryBranchError">
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
  RepoProvider,
  RepoConfigUpdateRequest,
  RepoSyncStatusResponse
} from '../../types/repo'
import type { AiProviderConfig } from '../../types/ai'

interface AiFormItem extends AiProviderConfig {
  inputApiKey?: string
  saving?: boolean
}

type FeedbackType = 'success' | 'warning' | 'info' | 'error'

const loading = ref(false)
const repoSaving = ref(false)
const syncing = ref(false)
const readmeRebuilding = ref(false)
const syncStatusLoading = ref(false)
const entrySaving = ref(false)
const entryDialogVisible = ref(false)
const editingEntryId = ref<number | null>(null)
const entryValidationTriggered = ref(false)

const repoFeedback = reactive<{
  type: FeedbackType
  title: string
  message: string
}>({
  type: 'info',
  title: '',
  message: ''
})

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

const repoActionHint = computed(() => {
  if (repoSaving.value) return '配置保存中，完成后可继续操作。'
  if (syncing.value) return '仓库同步进行中，请等待完成。'
  if (readmeRebuilding.value) return 'README 重建进行中，请稍候。'
  if (!repoForm.configured) return '请先保存并启用仓库配置，再执行同步或重建。'
  return ''
})

const syncDisabled = computed(() => Boolean(repoActionHint.value) || syncStatusLoading.value)
const readmeDisabled = computed(() => Boolean(repoActionHint.value) || syncStatusLoading.value)
const refreshStatusDisabled = computed(() => syncing.value || readmeRebuilding.value || syncStatusLoading.value)

const getErrorMessage = (error: unknown, fallback: string) => {
  const apiMsg = (error as any)?.response?.data?.message
  const msg = (typeof apiMsg === 'string' && apiMsg.trim()) || (error as any)?.message
  return typeof msg === 'string' && msg.trim() ? msg.trim() : fallback
}

const normalizeText = (value?: string) => (value || '').trim()

const isValidHttpUrl = (value: string) => {
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

const toBrowserRepoUrl = (value?: string) => {
  const raw = normalizeText(value)
  if (!raw) return ''
  if (isValidHttpUrl(raw)) return raw

  const sshMatch = raw.match(/^git@(github\.com|gitee\.com):(.+)$/i)
  if (sshMatch?.[1] && sshMatch?.[2]) {
    return `https://${sshMatch[1]}/${sshMatch[2].replace(/\.git$/i, '')}`
  }

  const sshSchemeMatch = raw.match(/^ssh:\/\/git@(github\.com|gitee\.com)\/(.+)$/i)
  if (sshSchemeMatch?.[1] && sshSchemeMatch?.[2]) {
    return `https://${sshSchemeMatch[1]}/${sshSchemeMatch[2].replace(/\.git$/i, '')}`
  }

  const gitSchemeMatch = raw.match(/^git:\/\/(github\.com|gitee\.com)\/(.+)$/i)
  if (gitSchemeMatch?.[1] && gitSchemeMatch?.[2]) {
    return `https://${gitSchemeMatch[1]}/${gitSchemeMatch[2].replace(/\.git$/i, '')}`
  }
  return ''
}

const inferProviderFromRepoUrl = (value?: string): RepoProvider | null => {
  const raw = normalizeText(value).toLowerCase()
  if (!raw) return null
  if (raw.includes('github.com')) return 'GITHUB'
  if (raw.includes('gitee.com')) return 'GITEE'
  return null
}

const isValidRepoUrl = (value?: string) => {
  const raw = normalizeText(value)
  if (!raw) return false
  if (isValidHttpUrl(raw)) return true
  return /^(git@|ssh:\/\/git@|git:\/\/)/i.test(raw)
}

const isValidBranchName = (value?: string) => {
  const raw = normalizeText(value)
  return Boolean(raw) && !/\s/.test(raw)
}

const getRepoConfigError = () => {
  if (!repoForm.provider) return '请选择仓库平台'
  if (!isValidRepoUrl(repoForm.repoUrl)) return '仓库地址格式不正确，请使用 https:// 或 git@...'
  const inferredProvider = inferProviderFromRepoUrl(repoForm.repoUrl)
  if (inferredProvider && inferredProvider !== repoForm.provider) return '仓库地址与平台不一致，请检查平台选择'
  if (!isValidBranchName(repoForm.branch)) return '分支名称不能为空且不能包含空格'
  if (!normalizeText(repoForm.targetDir)) return '目标目录不能为空'
  const appBaseUrl = normalizeText(repoForm.appBaseUrl)
  if (appBaseUrl && !isValidHttpUrl(appBaseUrl)) return '应用地址需为 http(s) URL'
  return ''
}

const setRepoFeedback = (type: FeedbackType, title: string, message: string) => {
  repoFeedback.type = type
  repoFeedback.title = title
  repoFeedback.message = message
}

const getEntryPaperErrorValue = () => {
  if (!entryForm.paperId) return '请选择论文'
  return ''
}

const getEntryRepoUrlErrorValue = () => {
  if (!normalizeText(entryForm.repoUrl)) return '仓库地址不能为空'
  if (!isValidRepoUrl(entryForm.repoUrl)) return '仓库地址格式不正确，请使用 https:// 或 git@...'
  const inferredProvider = inferProviderFromRepoUrl(entryForm.repoUrl)
  if (inferredProvider && inferredProvider !== entryForm.provider) return '仓库地址与平台不一致，请修正后再保存'
  return ''
}

const getEntryBranchErrorValue = () => {
  if (entryForm.branch && !isValidBranchName(entryForm.branch)) return '分支名不能包含空格'
  return ''
}

const entryPaperError = computed(() => (entryValidationTriggered.value ? getEntryPaperErrorValue() : ''))
const entryRepoUrlError = computed(() => (entryValidationTriggered.value ? getEntryRepoUrlErrorValue() : ''))
const entryBranchError = computed(() => (entryValidationTriggered.value ? getEntryBranchErrorValue() : ''))

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
  } catch (error) {
    const message = getErrorMessage(error, '设置页数据加载失败，请刷新页面后重试')
    setRepoFeedback('error', '加载失败', message)
  } finally {
    loading.value = false
  }
}

const handleSaveRepo = async () => {
  const validateError = getRepoConfigError()
  if (validateError) {
    ElMessage.warning(validateError)
    setRepoFeedback('warning', '配置未保存', validateError)
    return
  }

  const payload: RepoConfigUpdateRequest = {
    provider: repoForm.provider as RepoProvider,
    repoUrl: normalizeText(repoForm.repoUrl),
    branch: normalizeText(repoForm.branch),
    targetDir: normalizeText(repoForm.targetDir),
    autoCommitReadme: repoForm.autoCommitReadme,
    appBaseUrl: normalizeText(repoForm.appBaseUrl) || undefined
  }

  repoSaving.value = true
  try {
    const res = await updateRepoConfig(payload)
    if (res.code === 200) {
      Object.assign(repoForm, res.data)
      ElMessage.success('仓库配置已保存')
      setRepoFeedback('success', '配置已保存', '可继续执行仓库同步或 README 重建。')
    }
  } catch (error) {
    const message = getErrorMessage(error, '保存配置失败，请检查输入后重试')
    setRepoFeedback('error', '配置保存失败', message)
  } finally {
    repoSaving.value = false
  }
}

const handleSync = async () => {
  if (syncDisabled.value) {
    ElMessage.warning(repoActionHint.value || '当前不可执行同步')
    return
  }
  syncing.value = true
  setRepoFeedback('info', '同步进行中', '正在执行仓库 Clone/Pull，请稍候。')
  try {
    const res = await syncRepo()
    if (res.code === 200) {
      Object.assign(syncStatus, res.data)
      const successMessage = res.data.message || '同步成功'
      ElMessage.success(successMessage)
      setRepoFeedback('success', '同步成功', successMessage)
    }
  } catch (error) {
    const message = getErrorMessage(error, '仓库同步失败，请检查仓库地址、SSH Key 和网络后重试')
    setRepoFeedback('error', '同步失败', message)
  } finally {
    syncing.value = false
    await refreshSyncStatus(true)
  }
}

const handleRebuildReadme = async () => {
  if (readmeDisabled.value) {
    ElMessage.warning(repoActionHint.value || '当前不可重建 README')
    return
  }
  readmeRebuilding.value = true
  setRepoFeedback('info', 'README 重建中', '正在重建 README 并尝试同步仓库。')
  try {
    const res = await rebuildRepoReadme()
    if (res.code === 200) {
      const successMessage = res.message || 'README 已重建'
      ElMessage.success(successMessage)
      setRepoFeedback('success', 'README 已重建', successMessage)
    }
  } catch (error) {
    const message = getErrorMessage(error, 'README 重建失败，请先确认仓库已配置并可写入')
    setRepoFeedback('error', 'README 重建失败', message)
  } finally {
    readmeRebuilding.value = false
    await refreshSyncStatus(true)
  }
}

const refreshSyncStatus = async (silent = false) => {
  syncStatusLoading.value = true
  try {
    const res = await getRepoSyncStatus()
    if (res.code === 200) {
      Object.assign(syncStatus, res.data)
      if (!silent) {
        setRepoFeedback('info', '状态已刷新', res.data.message || `当前状态：${res.data.status}`)
      }
    }
  } catch (error) {
    const message = getErrorMessage(error, '刷新同步状态失败，请稍后再试')
    if (!silent) setRepoFeedback('error', '状态刷新失败', message)
  } finally {
    syncStatusLoading.value = false
  }
}

const reloadCodeEntries = async () => {
  try {
    const res = await listCodeEntries()
    if (res.code === 200) {
      codeEntries.value = res.data
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '代码条目刷新失败，请稍后重试'))
  }
}

const openEntryDialog = (entry?: PaperCodeEntry) => {
  entryValidationTriggered.value = false
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
  entryValidationTriggered.value = true
  const validateError = getEntryPaperErrorValue() || getEntryRepoUrlErrorValue() || getEntryBranchErrorValue()
  if (validateError) {
    ElMessage.warning(validateError)
    return
  }

  const payload: PaperCodeEntryPayload = {
    paperId: entryForm.paperId,
    provider: entryForm.provider,
    repoUrl: normalizeText(entryForm.repoUrl),
    branch: normalizeText(entryForm.branch) || 'main',
    description: normalizeText(entryForm.description) || undefined
  }

  entrySaving.value = true
  try {
    if (editingEntryId.value) {
      await updateCodeEntry(editingEntryId.value, payload)
      ElMessage.success('条目已更新')
    } else {
      await createCodeEntry(payload)
      ElMessage.success('条目已创建')
    }
    entryDialogVisible.value = false
    await reloadCodeEntries()
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '条目保存失败，请检查输入后重试'))
  } finally {
    entrySaving.value = false
  }
}

const handleDeleteEntry = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除该代码条目？', '提示', { type: 'warning' })
    await deleteCodeEntry(id)
    ElMessage.success('已删除')
    await reloadCodeEntries()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getErrorMessage(error, '删除条目失败，请重试'))
    }
  }
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
  } catch (error) {
    ElMessage.error(getErrorMessage(error, `${provider.provider} 配置保存失败，请重试`))
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

.repo-feedback {
  margin-bottom: 12px;
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

.invalid-link-text {
  color: var(--text-secondary);
  word-break: break-all;
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
