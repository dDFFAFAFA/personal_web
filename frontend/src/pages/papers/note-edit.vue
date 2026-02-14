<template>
  <div class="note-edit-container">
    <div class="edit-header">
      <div class="left">
        <el-button icon="ArrowLeft" @click="$router.back()">返回</el-button>
        <el-input 
          v-model="title" 
          placeholder="笔记标题" 
          style="width: 300px; margin-left: 16px; font-size: 18px;" 
          class="title-input"
        />
      </div>
      <div class="right">
        <span class="status-text">{{ statusText }}</span>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </div>
    </div>

    <div class="editor-wrapper">
      <MdEditor 
        v-model="content" 
        :theme="theme as any"
        previewTheme="github"
        codeTheme="a11y"
        :showCodeRowNumber="true"
        :noKatex="false"
        :noMermaid="false"
        :markdownItPlugins="markdownItPlugins"
        @onSave="handleSave" 
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
// @ts-ignore
import markdownItMark from 'markdown-it-mark'
import { getPaperNotes, updateNote } from '../../api/note'
import { useTheme } from '../../composables/useTheme'

const route = useRoute()
const paperId = Number(route.params.id)
const noteId = Number(route.params.noteId)

const { theme } = useTheme()

const title = ref('')
const content = ref('')
const statusText = ref('')
let saveTimer: any = null

const markdownItPlugins = (md: any) => {
  md.use(markdownItMark)
}

const fetchData = async () => {
  try {
    const res = await getPaperNotes(paperId)
    if (res.code === 200) {
      const note = res.data.find((n: any) => n.id === noteId)
      if (note) {
        title.value = note.title
        content.value = note.content
      }
    }
  } catch (error) {
    console.error(error)
  }
}

const handleSave = async () => {
  statusText.value = '保存中...'
  try {
    const res = await updateNote(noteId, { title: title.value, content: content.value })
    if (res.code === 200) {
      statusText.value = '已保存 ' + new Date().toLocaleTimeString()
    }
  } catch (error) {
    statusText.value = '保存失败'
  }
}

// Auto save
watch([title, content], () => {
  statusText.value = '有未保存修改...'
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(() => {
    handleSave()
  }, 3000)
})

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
:deep(mark) {
  background-color: #fff3bf;
  padding: 0.1em 0.3em;
  border-radius: 3px;
  color: #000;
}

:deep(.dark mark) {
  background-color: #5c4d1a;
  color: #e5e7eb;
}

.note-edit-container {
  height: calc(100vh - 80px); /* Adjust for header/padding */
  display: flex;
  flex-direction: column;
}

.edit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  background: #fff;
  padding: 12px;
  border-bottom: 1px solid var(--border-color);
}

.left, .right {
  display: flex;
  align-items: center;
}

.status-text {
  margin-right: 16px;
  font-size: 12px;
  color: #999;
}

.editor-wrapper {
  flex: 1;
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}
</style>
