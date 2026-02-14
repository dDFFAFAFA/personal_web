<template>
  <div class="paper-detail-container" v-loading="loading">
    <div class="header-actions">
      <el-button @click="$router.push('/papers')" :icon="ArrowLeft">返回列表</el-button>
      <div class="right-actions">
        <el-button type="danger" plain :icon="Delete" @click="handleDelete">删除论文</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="detail-tabs" style="margin-top: 20px;">
      <el-tab-pane label="📋 详情" name="details">
        <el-row :gutter="24">
          <!-- Left: Metadata -->
          <el-col :span="8">
            <el-card class="meta-card">
              <template #header>
                <div class="meta-header">
                  <span class="meta-title">论文信息</span>
                  <div class="meta-actions">
                    <el-button 
                      :icon="paper.starred ? StarFilled : Star" 
                      :type="paper.starred ? 'warning' : 'default'" 
                      circle 
                      @click="toggleStar"
                    />
                  </div>
                </div>
              </template>
              
              <div class="meta-content">
                <h2 class="paper-title">{{ paper.title }}</h2>
                <p class="paper-authors">{{ paper.authors?.join(', ') }}</p>
                
                <div class="meta-grid">
                  <div class="meta-row">
                    <span class="label">年份:</span>
                    <span>{{ paper.year }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="label">来源:</span>
                    <span>{{ paper.venue || '-' }}</span>
                  </div>
                  <div class="meta-row">
                    <span class="label">DOI:</span>
                    <span>{{ paper.doi || '-' }}</span>
                  </div>
                   <div class="meta-row" v-if="paper.ccfRank">
                    <span class="label">CCF:</span>
                    <el-tag size="small" :color="getCcfColor(paper.ccfRank)" effect="dark" style="border: none">
                      CCF-{{ paper.ccfRank }}
                    </el-tag>
                  </div>
                  <div class="meta-row" v-if="paper.jcrQuartile">
                    <span class="label">JCR:</span>
                    <el-tag size="small" type="info">{{ paper.jcrQuartile }}</el-tag>
                  </div>
                  <div class="meta-row" v-if="paper.citationCount !== undefined">
                    <span class="label">引用:</span>
                    <span>{{ paper.citationCount }}</span>
                  </div>
                  <div class="meta-row" v-if="paper.paperUrl">
                    <span class="label">链接:</span>
                    <a :href="paper.paperUrl" target="_blank">Link</a>
                  </div>
                </div>
                
                <div class="meta-row status-row" style="margin-top: 12px;">
                  <span class="label">状态:</span>
                  <el-select v-model="paper.readingStatus" size="small" @change="handleStatusChange" style="width: 120px;">
                    <el-option
                      v-for="(label, key) in ReadingStatusLabel"
                      :key="key"
                      :label="label"
                      :value="key"
                    />
                  </el-select>
                </div>

                <div class="tags-section">
                  <span class="label">标签:</span>
                  <div class="tags-list">
                    <el-tag 
                      v-for="tag in paper.tags" 
                      :key="tag.id" 
                      size="small" 
                      :color="tag.color + '20'" 
                      :style="{ color: tag.color, borderColor: 'transparent' }"
                    >
                      {{ tag.name }}
                    </el-tag>
                    <el-button size="small" :icon="Plus" circle style="margin-left: 5px;" />
                  </div>
                </div>

                <div class="abstract-section" v-if="paper.abstractText">
                  <span class="label">摘要:</span>
                  <p class="abstract-text">{{ paper.abstractText }}</p>
                </div>

                <div class="file-action">
                  <el-button type="primary" plain style="width: 100%; margin-bottom: 8px;" :icon="Refresh" @click="handleUpdateMetadata" :loading="enrichLoading">
                    更新元数据
                  </el-button>
                   <el-button style="width: 100%; margin-left: 0;" :icon="CopyDocument" @click="handleCopyBibtex">
                    复制 BibTeX
                  </el-button>
                  <el-button type="success" plain style="width: 100%; margin-left: 0; margin-top: 8px;" :icon="Document" @click="activeTab = 'pdf'">
                    阅读 PDF
                  </el-button>
                  <div class="backup-row">
                    <span class="label">备份状态:</span>
                    <el-tag :type="backupStatusType" size="small">{{ backupStatusLabel }}</el-tag>
                  </div>
                  <div class="backup-time" v-if="backupStatusHint">
                    {{ backupStatusHint }}
                  </div>
                  <div class="backup-time" v-if="backupState?.backupAt || paper.backupAt">
                    最近备份: {{ formatDate(backupState?.backupAt || paper.backupAt || '') }}
                  </div>
                  <div class="backup-time error" v-if="backupErrorMessage">
                    失败原因：{{ backupErrorMessage }}
                  </div>
                  <el-button
                    type="warning"
                    plain
                    style="width: 100%; margin-left: 0; margin-top: 8px;"
                    @click="handleBackupToOss"
                    :loading="backupLoading"
                    :disabled="backupDisabled"
                  >
                    备份到 OSS
                  </el-button>
                  <div class="action-tip" v-if="backupDisabledReason">
                    {{ backupDisabledReason }}
                  </div>
                  <el-button
                    plain
                    style="width: 100%; margin-left: 0; margin-top: 8px;"
                    @click="handleRestoreFromOss"
                    :loading="restoreLoading"
                    :disabled="restoreDisabled"
                  >
                    从 OSS 恢复
                  </el-button>
                  <div class="action-tip" v-if="restoreDisabledReason">
                    {{ restoreDisabledReason }}
                  </div>
                  <el-button
                    plain
                    style="width: 100%; margin-left: 0; margin-top: 8px;"
                    @click="handleSyncRepo"
                    :loading="syncRepoLoading"
                    :disabled="syncRepoDisabled"
                  >
                    同步仓库（Clone/Pull）
                  </el-button>
                  <div class="action-tip" v-if="syncRepoDisabledReason">
                    {{ syncRepoDisabledReason }}
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- Right: Notes -->
          <el-col :span="16" class="notes-theme-scope" :style="noteThemeStyle">
            <el-card class="notes-card">
              <template #header>
                <div class="notes-header">
                  <span>阅读笔记 ({{ notes.length }})</span>
                  <div class="notes-header-actions">
                    <el-select
                      v-model="selectedNoteTheme"
                      size="small"
                      class="notes-theme-select"
                      @change="handleNoteThemeChange"
                    >
                      <el-option
                        v-for="theme in noteThemeOptions"
                        :key="theme.value"
                        :label="theme.label"
                        :value="theme.value"
                      />
                    </el-select>
                    <el-button type="primary" size="small" :icon="Plus" @click="createNote">新增笔记</el-button>
                  </div>
                </div>
              </template>

              <div v-if="notes.length === 0" class="empty-notes">
                <el-empty description="暂无笔记" />
              </div>

              <div v-else class="notes-list">
                <el-card 
                  v-for="note in notes" 
                  :key="note.id" 
                  class="note-item" 
                  shadow="hover"
                  @click="editNote(note.id)"
                >
                  <div class="note-item-header">
                    <span class="note-title">{{ note.title }}</span>
                    <span class="note-time">{{ formatDate(note.updatedAt) }}</span>
                  </div>
                  <div class="note-preview markdown-note-preview" v-html="renderNotePreview(note.content)" />
                </el-card>
              </div>
            </el-card>

            <el-card class="panel-card">
              <template #header>
                <div class="panel-header">
                  <span>结构化概要</span>
                  <div class="panel-actions">
                    <el-button size="small" :loading="summaryLoading" @click="fetchSummary">刷新</el-button>
                    <el-button type="primary" size="small" :loading="summaryGenerating" @click="handleGenerateSummary">
                      生成概要
                    </el-button>
                    <el-button
                      size="small"
                      :loading="summaryDownloadLoading"
                      :disabled="!summaryMarkdown"
                      @click="handleDownloadSummary"
                    >
                      下载 .md
                    </el-button>
                  </div>
                </div>
              </template>

              <el-alert
                v-if="summaryError"
                :title="summaryError"
                type="error"
                :closable="false"
                show-icon
                class="panel-alert"
              />

              <div v-if="summaryLoading" class="panel-loading">
                <el-skeleton :rows="4" animated />
              </div>

              <div v-else-if="summaryError" class="panel-retry">
                <el-button type="danger" plain @click="fetchSummary">重试加载概要</el-button>
              </div>

              <div v-else>
                <div class="summary-meta-row">
                  <span>状态：</span>
                  <el-tag :type="summaryStatusType">{{ summaryStatusLabel }}</el-tag>
                  <span class="summary-meta-time">更新时间：{{ formatDateTime(summaryState?.generatedAt) }}</span>
                </div>
                <div class="backup-time error" v-if="summaryState?.error">
                  失败原因：{{ summaryState?.error }}
                </div>
                <el-empty v-if="!summaryMarkdown" description="暂无概要内容，点击“生成概要”开始。" />
                <div v-else class="summary-markdown">
                  <MdPreview
                    :editorId="summaryPreviewId"
                    :modelValue="summaryMarkdown"
                    previewTheme="github"
                    codeTheme="a11y"
                  />
                </div>
              </div>
            </el-card>

            <el-card class="panel-card">
              <template #header>
                <div class="panel-header">
                  <span>仓库链接提取</span>
                  <div class="panel-actions">
                    <el-button size="small" :loading="repoLinksLoading" @click="fetchRepoLinks">刷新</el-button>
                    <el-button type="primary" size="small" :loading="repoLinksExtracting" @click="handleExtractRepoLinks">
                      从 PDF 提取链接
                    </el-button>
                    <el-button
                      size="small"
                      :disabled="applyRepoLinksDisabled"
                      :loading="repoLinksApplying"
                      @click="handleApplyRepoLinks"
                    >
                      应用到仓库索引
                    </el-button>
                  </div>
                </div>
              </template>

              <el-alert
                v-if="repoLinksError"
                :title="repoLinksError"
                type="error"
                :closable="false"
                show-icon
                class="panel-alert"
              />

              <div v-if="repoLinksLoading" class="panel-loading">
                <el-skeleton :rows="4" animated />
              </div>

              <div v-else-if="repoLinksError" class="panel-retry">
                <el-button type="danger" plain @click="fetchRepoLinks">重试加载链接</el-button>
              </div>

              <el-empty
                v-else-if="repoLinkCandidates.length === 0"
                description="暂无候选链接，点击“从 PDF 提取链接”进行扫描。"
              />

              <div v-else class="repo-links-list">
                <div class="repo-meta-row">
                  共 {{ repoLinkCandidates.length }} 条候选，已选择 {{ selectedRepoLinkIds.length }} 条
                </div>
                <el-checkbox-group v-model="selectedRepoLinkIds" class="repo-checkbox-group">
                  <div class="repo-link-item" v-for="item in repoLinkCandidates" :key="item.id">
                    <el-checkbox :label="item.id">
                      <span class="repo-link-url">{{ item.url }}</span>
                    </el-checkbox>
                    <div class="repo-link-meta">
                      <el-tag size="small" type="info">{{ item.provider }}</el-tag>
                      <el-tag v-if="item.status" size="small" :type="item.status === 'APPLIED' ? 'success' : 'warning'">
                        {{ item.status }}
                      </el-tag>
                      <span v-if="item.confidence !== undefined" class="repo-confidence">
                        置信度：{{ Number(item.confidence).toFixed(2) }}
                      </span>
                      <span v-if="item.pageNo !== undefined" class="repo-confidence">
                        页码：{{ item.pageNo }}
                      </span>
                      <el-link :href="item.url" target="_blank" rel="noopener noreferrer" type="primary">打开链接</el-link>
                    </div>
                    <div class="repo-source" v-if="item.sourceText">
                      片段：{{ item.sourceText }}
                    </div>
                  </div>
                </el-checkbox-group>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane :label="`📝 笔记(${notes.length})`" name="notes">
         <!-- Reuse notes list or just show duplicate -->
         <div class="notes-tab-content">
             <div class="notes-header" style="margin-bottom: 20px;">
                <div class="notes-header-actions">
                  <el-select
                    v-model="selectedNoteTheme"
                    size="small"
                    class="notes-theme-select"
                    @change="handleNoteThemeChange"
                  >
                    <el-option
                      v-for="theme in noteThemeOptions"
                      :key="theme.value"
                      :label="theme.label"
                      :value="theme.value"
                    />
                  </el-select>
                  <el-button type="primary" :icon="Plus" @click="createNote">新增笔记</el-button>
                </div>
             </div>
             <el-row :gutter="20">
                <el-col :span="8" v-for="note in notes" :key="note.id">
                   <el-card class="note-item" shadow="hover" @click="editNote(note.id)" style="margin-bottom: 20px; cursor: pointer;">
                      <h3>{{ note.title }}</h3>
                      <p class="note-time">{{ formatDate(note.updatedAt) }}</p>
                      <div class="note-preview markdown-note-preview" v-html="renderNotePreview(note.content)" />
                   </el-card>
                </el-col>
             </el-row>
         </div>
      </el-tab-pane>

      <el-tab-pane label="📄 PDF 阅读" name="pdf">
        <div style="height: calc(100vh - 200px);">
          <PdfViewer :source="pdfUrl" />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import MarkdownIt from 'markdown-it'
// @ts-ignore
import markdownItMark from 'markdown-it-mark'
import {
  applyPaperRepoLinks,
  getPaperDetail,
  getPaperRepoLinks,
  getPaperSummary,
  updatePaperStatus,
  togglePaperStar,
  deletePaper,
  backupPaperToOss,
  getPaperBackupStatus,
  restorePaperFromOss,
  extractPaperRepoLinks,
  generatePaperSummary,
  downloadPaperSummary,
  type PaperBackupStatus
} from '../../api/paper'
import { getPaperNotes, createNote as apiCreateNote } from '../../api/note'
import { enrichPaper } from '../../api/metadata'
import { exportPapers } from '../../api/importExport'
import { syncRepo } from '../../api/repo'
import type { Paper, Note, PaperRepoLinkCandidate, PaperSummary, PaperSummaryStatus } from '../../types/paper'
import { ReadingStatusLabel } from '../../types/enums'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Star, StarFilled, Plus, Document, Refresh, CopyDocument } from '@element-plus/icons-vue'
import PdfViewer from './components/PdfViewer.vue'

const route = useRoute()
const router = useRouter()
const paperId = Number(route.params.id)
const summaryPreviewId = `paper-summary-preview-${paperId}`
const loading = ref(false)
const enrichLoading = ref(false)
const backupLoading = ref(false)
const restoreLoading = ref(false)
const syncRepoLoading = ref(false)
const paper = ref<Paper>({} as Paper)
const notes = ref<Note[]>([])
const activeTab = ref('details')
const backupState = ref<PaperBackupStatus | null>(null)
const lastReadingStatus = ref<Paper['readingStatus']>('UNREAD')
const summaryState = ref<PaperSummary | null>(null)
const summaryLoading = ref(false)
const summaryGenerating = ref(false)
const summaryDownloadLoading = ref(false)
const summaryError = ref('')
const repoLinksLoading = ref(false)
const repoLinksExtracting = ref(false)
const repoLinksApplying = ref(false)
const repoLinksError = ref('')
const repoLinkCandidates = ref<PaperRepoLinkCandidate[]>([])
const selectedRepoLinkIds = ref<number[]>([])

const NOTE_THEME_STORAGE_KEY = "notes-highlight-theme"
const noteThemeOptions = [
  { value: "morandi-neutral", label: "雾灰米白" },
  { value: "morandi-sage", label: "鼠尾草绿" },
  { value: "morandi-blue", label: "雾霭蓝" },
  { value: "morandi-rose", label: "陶土玫瑰" },
  { value: "morandi-lilac", label: "灰紫暮色" }
] as const

const noteThemeTokens: Record<string, { markBg: string; markText: string }> = {
  "morandi-neutral": { markBg: "#d8d2ca", markText: "#594f46" },
  "morandi-sage": { markBg: "#c6d0c2", markText: "#405046" },
  "morandi-blue": { markBg: "#c8d2d8", markText: "#3f4a55" },
  "morandi-rose": { markBg: "#d7c6c2", markText: "#5a4545" },
  "morandi-lilac": { markBg: "#d1cad7", markText: "#4b4458" }
}

const resolveInitialNoteTheme = () => {
  if (typeof window === "undefined") return "morandi-neutral"
  const stored = window.localStorage.getItem(NOTE_THEME_STORAGE_KEY)
  if (stored && noteThemeTokens[stored]) return stored
  return "morandi-neutral"
}

const selectedNoteTheme = ref(resolveInitialNoteTheme())
const noteThemeStyle = computed(() => {
  const theme = (noteThemeTokens[selectedNoteTheme.value] ?? noteThemeTokens["morandi-neutral"]) as {
    markBg: string
    markText: string
  }
  return {
    "--note-mark-bg": theme.markBg,
    "--note-mark-text": theme.markText
  }
})

const handleNoteThemeChange = (value: string) => {
  selectedNoteTheme.value = noteThemeTokens[value] ? value : "morandi-neutral"
  if (typeof window !== "undefined") {
    window.localStorage.setItem(NOTE_THEME_STORAGE_KEY, selectedNoteTheme.value)
  }
}

const noteMarkdownRenderer = new MarkdownIt({ html: false, linkify: true, breaks: true })
noteMarkdownRenderer.use(markdownItMark)
const renderNotePreview = (content: string) => noteMarkdownRenderer.render(content || "")

const pdfUrl = computed(() => `/api/v1/papers/${paperId}/file`)

const fetchData = async () => {
  loading.value = true
  try {
    const [paperRes, notesRes, backupRes] = await Promise.all([
      getPaperDetail(paperId),
      getPaperNotes(paperId),
      getPaperBackupStatus(paperId)
    ])
    if (paperRes.code === 200) {
      paper.value = paperRes.data
      lastReadingStatus.value = paperRes.data.readingStatus
    }
    if (notesRes.code === 200) notes.value = notesRes.data
    if (backupRes.code === 200) backupState.value = backupRes.data
  } catch (error) {
    console.error(error)
    ElMessage.error(getErrorMessage(error, '获取详情失败，请刷新后重试'))
  } finally {
    loading.value = false
  }

  await Promise.allSettled([fetchSummary(false), fetchRepoLinks(false)])
}

const getErrorMessage = (error: unknown, fallback: string) => {
  const apiMsg = (error as any)?.response?.data?.message
  const msg = (typeof apiMsg === 'string' && apiMsg.trim()) || (error as any)?.message
  return typeof msg === 'string' && msg.trim() ? msg.trim() : fallback
}

const refreshBackupStatus = async (showMessageOnError = false) => {
  try {
    const res = await getPaperBackupStatus(paperId)
    if (res.code === 200) {
      backupState.value = res.data
      paper.value.backupStatus = res.data.backupStatus
      paper.value.backupAt = res.data.backupAt
      paper.value.backupError = res.data.backupError
    }
  } catch (error) {
    if (showMessageOnError) {
      ElMessage.error(getErrorMessage(error, '刷新备份状态失败，请重试'))
    }
  }
}

const handleStatusChange = async (val: string) => {
  try {
    await updatePaperStatus(paperId, val)
    lastReadingStatus.value = val as Paper['readingStatus']
    ElMessage.success('状态已更新')
  } catch (error) {
    paper.value.readingStatus = lastReadingStatus.value
    ElMessage.error(getErrorMessage(error, '状态更新失败，请重试'))
  }
}

const toggleStar = async () => {
  const newVal = !paper.value.starred
  try {
    await togglePaperStar(paperId, newVal)
    paper.value.starred = newVal
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '收藏状态更新失败，请稍后重试'))
  }
}

const handleDelete = () => {
  ElMessageBox.confirm('确定删除此论文？', '警告', { type: 'warning' })
    .then(async () => {
      await deletePaper(paperId)
      router.push('/papers')
      ElMessage.success('删除成功')
    })
    .catch((error) => {
      if (error !== 'cancel' && error !== 'close') {
        ElMessage.error(getErrorMessage(error, '删除失败，请重试'))
      }
    })
}

const createNote = async () => {
  try {
    const res = await apiCreateNote(paperId, { title: '新笔记', content: '# New Note' })
    if (res.code === 200) {
      router.push(`/papers/${paperId}/notes/${res.data.id}`)
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(getErrorMessage(error, '创建笔记失败，请稍后重试'))
  }
}

const editNote = (noteId: number) => {
  router.push(`/papers/${paperId}/notes/${noteId}`)
}

const formatDate = (str: string) => {
  if (!str) return '-'
  return new Date(str).toLocaleDateString()
}

const formatDateTime = (str?: string) => {
  if (!str) return '-'
  return new Date(str).toLocaleString()
}

const getCcfColor = (rank: string) => {
  switch (rank) {
    case 'A': return '#F56C6C'
    case 'B': return '#E6A23C'
    case 'C': return '#409EFF'
    default: return '#909399'
  }
}

const handleUpdateMetadata = async () => {
  enrichLoading.value = true
  try {
    const res = await enrichPaper(paperId)
    if (res.code === 200) {
      const paperRes = await getPaperDetail(paperId)
      if (paperRes.code === 200) {
        paper.value = paperRes.data
        lastReadingStatus.value = paperRes.data.readingStatus
      }
      ElMessage.success("元数据已更新")
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(getErrorMessage(error, "更新元数据失败，请稍后重试"))
  } finally {
    enrichLoading.value = false
  }
}

const handleCopyBibtex = async () => {
  try {
    const res = await exportPapers('bibtex', [paperId])
    // res is Blob.
    const text = await (res as any).text()
    await navigator.clipboard.writeText(text)
    ElMessage.success('BibTeX 已复制到剪贴板')
  } catch (error) {
    console.error(error)
    ElMessage.error('复制失败')
  }
}

const summaryMarkdown = computed(() => summaryState.value?.markdown || '')

const summaryStatusLabel = computed(() => {
  const status = summaryState.value?.status
  if (!status) return '未生成'
  if (status === 'SUCCESS') return '生成成功'
  if (status === 'FAILED') return '生成失败'
  if (status === 'QUEUED' || status === 'PENDING' || status === 'GENERATING') return '生成中'
  return status
})

const summaryStatusType = computed(() => {
  const status = summaryState.value?.status
  if (!status) return 'info'
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'QUEUED' || status === 'PENDING' || status === 'GENERATING') return 'warning'
  return 'info'
})

const fetchSummary = async (showErrorToast = true) => {
  summaryLoading.value = true
  summaryError.value = ''
  try {
    const res = await getPaperSummary(paperId)
    if (res.code === 200) {
      summaryState.value = res.data
    }
  } catch (error) {
    summaryState.value = null
    summaryError.value = getErrorMessage(error, '获取概要失败，请稍后重试')
    if (showErrorToast) {
      ElMessage.error(summaryError.value)
    }
  } finally {
    summaryLoading.value = false
  }
}

const handleGenerateSummary = async () => {
  summaryGenerating.value = true
  summaryError.value = ''
  try {
    const res = await generatePaperSummary(paperId)
    if (res.code === 200) {
      summaryState.value = res.data
      const status = res.data.status as PaperSummaryStatus
      if (status === 'FAILED') {
        ElMessage.error(res.data.error || res.data.message || '概要生成失败')
      } else if (status === 'SUCCESS') {
        ElMessage.success('概要生成成功')
      } else {
        ElMessage.info(res.data.message || '已提交概要生成任务，请稍后刷新查看结果')
      }
    }
  } catch (error) {
    summaryError.value = getErrorMessage(error, '生成概要失败，请检查 AI 配置后重试')
    ElMessage.error(summaryError.value)
  } finally {
    summaryGenerating.value = false
    await fetchSummary(false)
  }
}

const handleDownloadSummary = async () => {
  if (!summaryMarkdown.value) {
    ElMessage.warning('暂无可下载的概要内容')
    return
  }
  summaryDownloadLoading.value = true
  try {
    const blob = await downloadPaperSummary(paperId)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `paper_${paperId}_summary.md`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('概要已下载')
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '下载概要失败，请稍后重试'))
  } finally {
    summaryDownloadLoading.value = false
  }
}

const applyRepoLinksDisabled = computed(() => {
  if (repoLinksLoading.value || repoLinksApplying.value || repoLinksExtracting.value) return true
  return selectedRepoLinkIds.value.length === 0
})

const fetchRepoLinks = async (showErrorToast = true) => {
  repoLinksLoading.value = true
  repoLinksError.value = ''
  try {
    const res = await getPaperRepoLinks(paperId)
    if (res.code === 200) {
      repoLinkCandidates.value = res.data.candidates || []
      selectedRepoLinkIds.value = []
    }
  } catch (error) {
    repoLinkCandidates.value = []
    selectedRepoLinkIds.value = []
    repoLinksError.value = getErrorMessage(error, '获取仓库候选链接失败，请稍后重试')
    if (showErrorToast) {
      ElMessage.error(repoLinksError.value)
    }
  } finally {
    repoLinksLoading.value = false
  }
}

const handleExtractRepoLinks = async () => {
  repoLinksExtracting.value = true
  repoLinksError.value = ''
  try {
    const res = await extractPaperRepoLinks(paperId)
    if (res.code === 200) {
      repoLinkCandidates.value = res.data.candidates || []
      selectedRepoLinkIds.value = (res.data.candidates || []).map((item) => item.id)
      ElMessage.success(res.data.message || `提取完成，共 ${repoLinkCandidates.value.length} 条候选链接`)
    }
  } catch (error) {
    repoLinksError.value = getErrorMessage(error, '提取仓库链接失败，请检查 PDF 可读性后重试')
    ElMessage.error(repoLinksError.value)
  } finally {
    repoLinksExtracting.value = false
  }
}

const handleApplyRepoLinks = async () => {
  if (applyRepoLinksDisabled.value) {
    ElMessage.warning('请先选择至少一个候选链接')
    return
  }
  repoLinksApplying.value = true
  try {
    const res = await applyPaperRepoLinks(paperId, {
      candidateIds: selectedRepoLinkIds.value,
      autoRebuildReadme: true
    })
    if (res.code === 200) {
      ElMessage.success(res.data.message || '候选链接已应用到仓库索引')
      repoLinkCandidates.value = res.data.candidates || repoLinkCandidates.value
      await fetchRepoLinks(false)
    }
  } catch (error) {
    ElMessage.error(getErrorMessage(error, '应用候选链接失败，请稍后重试'))
  } finally {
    repoLinksApplying.value = false
  }
}

const currentBackupStatus = computed(() => {
  if (backupLoading.value || restoreLoading.value) return 'IN_PROGRESS'
  return backupState.value?.backupStatus || paper.value.backupStatus || 'NOT_BACKED_UP'
})

const backupStatusLabel = computed(() => {
  const status = currentBackupStatus.value
  if (status === 'IN_PROGRESS') return '进行中'
  if (status === 'BACKED_UP') return '已备份'
  if (status === 'FAILED') return '备份失败'
  return '未备份'
})

const backupStatusType = computed(() => {
  const status = currentBackupStatus.value
  if (status === 'IN_PROGRESS') return 'warning'
  if (status === 'BACKED_UP') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'info'
})

const backupStatusHint = computed(() => {
  if (backupLoading.value) return '备份请求已提交，请等待完成。'
  if (restoreLoading.value) return '恢复中，完成后会自动刷新备份状态。'
  if (currentBackupStatus.value === 'FAILED') return '请先检查失败原因，修复后再重试。'
  if (currentBackupStatus.value === 'NOT_BACKED_UP') return '建议先执行一次备份，确保可恢复。'
  return ''
})

const backupErrorMessage = computed(() => {
  const err = backupState.value?.backupError || paper.value.backupError
  return typeof err === 'string' ? err.trim() : ''
})

const backupDisabledReason = computed(() => {
  if (loading.value) return '详情加载中，请稍后。'
  if (backupLoading.value) return '备份进行中，请勿重复点击。'
  if (restoreLoading.value) return '正在恢复文件，暂不可备份。'
  if (syncRepoLoading.value) return '仓库同步中，请稍后再备份。'
  if (!paper.value.fileName && !paper.value.filePath) return '未检测到论文文件，无法执行备份。'
  return ''
})
const backupDisabled = computed(() => Boolean(backupDisabledReason.value))

const restoreDisabledReason = computed(() => {
  if (loading.value) return '详情加载中，请稍后。'
  if (restoreLoading.value) return '恢复进行中，请勿重复点击。'
  if (backupLoading.value) return '备份进行中，暂不可恢复。'
  if (syncRepoLoading.value) return '仓库同步中，请稍后再恢复。'
  if (currentBackupStatus.value !== 'BACKED_UP') return '暂无可恢复备份，请先完成一次成功备份。'
  return ''
})
const restoreDisabled = computed(() => Boolean(restoreDisabledReason.value))

const syncRepoDisabledReason = computed(() => {
  if (loading.value) return '详情加载中，请稍后。'
  if (syncRepoLoading.value) return '仓库同步进行中，请勿重复点击。'
  if (backupLoading.value || restoreLoading.value) return '备份或恢复进行中，稍后再同步仓库。'
  return ''
})
const syncRepoDisabled = computed(() => Boolean(syncRepoDisabledReason.value))

const handleBackupToOss = async () => {
  if (backupDisabled.value) {
    ElMessage.warning(backupDisabledReason.value)
    return
  }
  backupLoading.value = true
  try {
    const res = await backupPaperToOss(paperId)
    if (res.code === 200) {
      backupState.value = res.data
      paper.value.backupStatus = res.data.backupStatus
      paper.value.backupAt = res.data.backupAt
      paper.value.backupError = res.data.backupError
      ElMessage.success('已备份到 OSS')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(getErrorMessage(error, '备份失败，请检查 OSS 配置后重试'))
  } finally {
    backupLoading.value = false
    await refreshBackupStatus(true)
  }
}

const handleRestoreFromOss = async () => {
  if (restoreDisabled.value) {
    ElMessage.warning(restoreDisabledReason.value)
    return
  }
  restoreLoading.value = true
  try {
    const res = await restorePaperFromOss(paperId)
    if (res.code === 200) {
      backupState.value = res.data
      paper.value.backupStatus = res.data.backupStatus
      paper.value.backupAt = res.data.backupAt
      paper.value.backupError = res.data.backupError
      ElMessage.success('已从 OSS 恢复本地文件')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(getErrorMessage(error, '恢复失败，请先确认备份存在且 OSS 可访问'))
  } finally {
    restoreLoading.value = false
    await refreshBackupStatus(true)
  }
}

const handleSyncRepo = async () => {
  if (syncRepoDisabled.value) {
    ElMessage.warning(syncRepoDisabledReason.value)
    return
  }
  syncRepoLoading.value = true
  try {
    const res = await syncRepo()
    if (res.code === 200) {
      ElMessage.success(res.data.message || '仓库同步成功')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(getErrorMessage(error, '仓库同步失败，请先检查设置页仓库配置'))
  } finally {
    syncRepoLoading.value = false
  }
}

onMounted(() => {
  if (paperId) fetchData()
})
</script>

<style scoped>
.header-actions {
  display: flex;
  justify-content: space-between;
}

.meta-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta-title {
  font-size: 16px;
  font-weight: 500;
}

.paper-title {
  font-size: 20px;
  margin: 0 0 8px 0;
  color: var(--text-primary);
}

.paper-authors {
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 16px;
}

.meta-row {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
  align-items: center;
}

.meta-row .label {
  width: 60px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.tags-section {
  margin-top: 16px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.abstract-section {
  margin-top: 16px;
}

.abstract-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-top: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.file-action {
  margin-top: 24px;
}

.backup-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}

.backup-time {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.backup-time.error {
  color: #f56c6c;
}

.action-tip {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.notes-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notes-theme-select {
  width: 140px;
}

.notes-theme-scope {
  --note-mark-bg: #d8d2ca;
  --note-mark-text: #594f46;
}

.panel-card {
  margin-top: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-actions {
  display: flex;
  gap: 8px;
}

.panel-alert {
  margin-bottom: 10px;
}

.panel-loading,
.panel-retry {
  margin-top: 8px;
}

.summary-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.summary-meta-time {
  font-size: 12px;
  color: var(--text-secondary);
}

.summary-markdown {
  max-height: 360px;
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 12px;
  background: #fafafa;
}

.summary-markdown :deep(.md-editor-preview-wrapper) {
  padding: 0;
  background: transparent;
}

.summary-markdown :deep(p),
.summary-markdown :deep(li),
.summary-markdown :deep(blockquote) {
  line-height: 1.7;
}

.repo-links-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.repo-meta-row {
  font-size: 12px;
  color: var(--text-secondary);
}

.repo-checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.repo-link-item {
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 10px;
}

.repo-link-url {
  word-break: break-all;
}

.repo-link-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.repo-confidence {
  font-size: 12px;
  color: var(--text-secondary);
}

.repo-source {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
}

.note-item {
  margin-bottom: 12px;
  cursor: pointer;
  border: 1px solid var(--border-color);
}

.note-item:hover {
  border-color: var(--primary-color);
}

.note-item-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.note-title {
  font-weight: 500;
}

.note-time {
  font-size: 12px;
  color: #999;
}

.note-preview {
  font-size: 13px;
  color: var(--text-secondary);
}

.markdown-note-preview {
  max-height: 120px;
  overflow: hidden;
}

.markdown-note-preview :deep(p),
.markdown-note-preview :deep(li),
.markdown-note-preview :deep(blockquote) {
  margin: 0 0 6px;
  line-height: 1.6;
}

.markdown-note-preview :deep(mark) {
  background-color: var(--note-mark-bg);
  color: var(--note-mark-text);
  padding: 0.1em 0.3em;
  border-radius: 4px;
}

.markdown-note-preview :deep(ul),
.markdown-note-preview :deep(ol) {
  padding-left: 18px;
}

.markdown-note-preview :deep(h1),
.markdown-note-preview :deep(h2),
.markdown-note-preview :deep(h3),
.markdown-note-preview :deep(h4) {
  margin: 0 0 6px;
  font-size: 13px;
}
</style>
