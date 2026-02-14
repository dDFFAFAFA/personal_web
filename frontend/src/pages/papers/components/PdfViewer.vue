<template>
  <div class="pdf-viewer-container" :class="{ 'is-dark': isDark }">
    <div class="pdf-toolbar">
      <div class="left">
        <el-button-group>
          <el-button :icon="ArrowLeft" @click="prevPage" :disabled="currentPage <= 1" />
          <span class="page-indicator">{{ currentPage }} / {{ pageCount }}</span>
          <el-button :icon="ArrowRight" @click="nextPage" :disabled="currentPage >= pageCount" />
        </el-button-group>
      </div>
      <div class="right">
        <el-button-group>
          <el-button :icon="ZoomOut" @click="zoomOut" />
          <span class="scale-indicator">{{ Math.round(scale * 100) }}%</span>
          <el-button :icon="ZoomIn" @click="zoomIn" />
        </el-button-group>
        <el-checkbox v-model="darkModePdf" label="深色模式" border size="small" style="margin-left: 10px" />
      </div>
    </div>

    <div class="pdf-content">
      <VuePdfEmbed
        v-if="source"
        ref="pdfRef"
        :source="source"
        :page="currentPage"
        :width="pdfWidth"
        :text-layer="true"
        class="vue-pdf-embed"
        :class="{ 'dark-mode-filter': darkModePdf }"
        @loaded="handleLoaded"
      />
      <div v-else class="empty-state">暂无 PDF 文件</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import VuePdfEmbed from 'vue-pdf-embed'
import { ArrowLeft, ArrowRight, ZoomIn, ZoomOut } from '@element-plus/icons-vue'
import { useTheme } from '../../../composables/useTheme'

const props = defineProps<{
  source?: string
}>()

const { theme } = useTheme()
const isDark = computed(() => theme.value === 'dark')

const currentPage = ref(1)
const pageCount = ref(0)
const scale = ref(1.0)
const darkModePdf = ref(false)
const containerWidth = ref(800)

const pdfWidth = computed(() => containerWidth.value * scale.value)

const handleLoaded = (pdf: any) => {
  pageCount.value = pdf.numPages
}

const prevPage = () => {
  if (currentPage.value > 1) currentPage.value--
}

const nextPage = () => {
  if (currentPage.value < pageCount.value) currentPage.value++
}

const zoomIn = () => {
  if (scale.value < 3) scale.value += 0.1
}

const zoomOut = () => {
  if (scale.value > 0.5) scale.value -= 0.1
}

watch(isDark, (val) => {
  darkModePdf.value = val
}, { immediate: true })

onMounted(() => {
  // Simple estimation, better with ResizeObserver
  containerWidth.value = 800 
})
</script>

<style scoped>
.pdf-viewer-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--bg-color);
  color: var(--text-color);
}

.pdf-toolbar {
  padding: 8px 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--bg-color-overlay);
}

.page-indicator, .scale-indicator {
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  font-size: 14px;
}

.pdf-content {
  flex: 1;
  overflow: auto;
  padding: 20px;
  display: flex;
  justify-content: center;
  background-color: #525659;
}

.is-dark .pdf-content {
  background-color: #1a1a1a;
}

.vue-pdf-embed {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  background-color: white;
}

.dark-mode-filter {
  filter: invert(0.9) hue-rotate(180deg);
}

.empty-state {
  color: #fff;
  margin-top: 50px;
}
</style>

