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
                </div>
              </div>
            </el-card>
          </el-col>

          <!-- Right: Notes -->
          <el-col :span="16">
            <el-card class="notes-card">
              <template #header>
                <div class="notes-header">
                  <span>阅读笔记 ({{ notes.length }})</span>
                  <el-button type="primary" size="small" :icon="Plus" @click="createNote">新增笔记</el-button>
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
                  <div class="note-preview">
                    {{ note.content.slice(0, 100) }}...
                  </div>
                </el-card>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane :label="`📝 笔记(${notes.length})`" name="notes">
         <!-- Reuse notes list or just show duplicate -->
         <div class="notes-tab-content">
             <div class="notes-header" style="margin-bottom: 20px;">
                <el-button type="primary" :icon="Plus" @click="createNote">新增笔记</el-button>
             </div>
             <el-row :gutter="20">
                <el-col :span="8" v-for="note in notes" :key="note.id">
                   <el-card class="note-item" shadow="hover" @click="editNote(note.id)" style="margin-bottom: 20px; cursor: pointer;">
                      <h3>{{ note.title }}</h3>
                      <p class="note-time">{{ formatDate(note.updatedAt) }}</p>
                      <p class="note-preview">{{ note.content.slice(0, 50) }}...</p>
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
import { getPaperDetail, updatePaperStatus, togglePaperStar, deletePaper } from '../../api/paper'
import { getPaperNotes, createNote as apiCreateNote } from '../../api/note'
import { enrichPaper } from '../../api/metadata'
import { exportPapers } from '../../api/importExport'
import type { Paper, Note } from '../../types/paper'
import { ReadingStatusLabel } from '../../types/enums'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Delete, Star, StarFilled, Plus, Document, Refresh, CopyDocument } from '@element-plus/icons-vue'
import PdfViewer from './components/PdfViewer.vue'

const route = useRoute()
const router = useRouter()
const paperId = Number(route.params.id)

const loading = ref(false)
const enrichLoading = ref(false)
const paper = ref<Paper>({} as Paper)
const notes = ref<Note[]>([])
const activeTab = ref('details')

const pdfUrl = computed(() => `/api/v1/papers/${paperId}/file`)

const fetchData = async () => {
  loading.value = true
  try {
    const [paperRes, notesRes] = await Promise.all([
      getPaperDetail(paperId),
      getPaperNotes(paperId)
    ])
    if (paperRes.code === 200) paper.value = paperRes.data
    if (notesRes.code === 200) notes.value = notesRes.data
  } catch (error) {
    console.error(error)
    ElMessage.error('获取详情失败')
  } finally {
    loading.value = false
  }
}

const handleStatusChange = async (val: string) => {
  await updatePaperStatus(paperId, val)
  ElMessage.success('状态已更新')
}

const toggleStar = async () => {
  const newVal = !paper.value.starred
  await togglePaperStar(paperId, newVal)
  paper.value.starred = newVal
}

const handleDelete = () => {
  ElMessageBox.confirm('确定删除此论文？', '警告', { type: 'warning' })
    .then(async () => {
      await deletePaper(paperId)
      router.push('/papers')
      ElMessage.success('删除成功')
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
  }
}

const editNote = (noteId: number) => {
  router.push(`/papers/${paperId}/notes/${noteId}`)
}

const formatDate = (str: string) => {
  return new Date(str).toLocaleDateString()
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
      paper.value = res.data
      ElMessage.success('元数据已更新')
    }
  } catch (error) {
    console.error(error)
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

.notes-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>

