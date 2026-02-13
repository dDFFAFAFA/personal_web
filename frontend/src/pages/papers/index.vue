<template>
  <div class="papers-container" data-testid="paper-list-page">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="left-tools">
        <el-input
          v-model="paperStore.filter.keyword"
          placeholder="搜索论文..."
          prefix-icon="Search"
          clearable
          @change="handleSearch"
          style="width: 240px"
        />
        <el-select
          v-model="paperStore.filter.status"
          placeholder="阅读状态"
          clearable
          style="width: 140px; margin-left: 12px"
          @change="handleSearch"
        >
          <el-option
            v-for="(label, key) in ReadingStatusLabel"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-select
          v-model="paperStore.filter.tagId"
          placeholder="标签筛选"
          clearable
          style="width: 140px; margin-left: 12px"
          @change="handleSearch"
        >
          <el-option
            v-for="tag in tagStore.tags"
            :key="tag.id"
            :label="tag.name"
            :value="tag.id"
          />
        </el-select>
        <el-checkbox
          v-model="paperStore.filter.starred"
          label="仅星标"
          border
          style="margin-left: 12px"
          @change="handleSearch"
        />
      </div>
      <div class="right-tools">
        <el-button type="primary" icon="Plus" round @click="showUploadDialog = true">
          上传论文
        </el-button>
      </div>
    </div>

    <!-- Content -->
    <el-table
      v-loading="paperStore.loading"
      :data="paperStore.papers"
      style="width: 100%; margin-top: 16px; border-radius: 8px;"
      :header-cell-style="{ background: '#f8f9fa', color: '#5f6368' }"
    >
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <div class="title-cell">
            <span class="title-text" @click="goToDetail(row.id)">{{ row.title }}</span>
            <div class="meta-text">
              <span v-if="row.venue" class="venue-tag">{{ row.venue }}</span>
              <span v-if="row.year">{{ row.year }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="作者" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.authors.join(', ') }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :color="getStatusColor(row.readingStatus) + '20'" :style="{ color: getStatusColor(row.readingStatus), borderColor: 'transparent' }">
            {{ ReadingStatusLabel[row.readingStatus as keyof typeof ReadingStatusLabel] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标签" width="200">
        <template #default="{ row }">
          <el-tag
            v-for="tag in row.tags"
            :key="tag.id"
            size="small"
            effect="plain"
            :color="tag.color + '15'"
            :style="{ color: tag.color, borderColor: tag.color + '40', marginRight: '4px' }"
          >
            {{ tag.name }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="right">
        <template #default="{ row }">
          <el-button
            :icon="row.starred ? 'StarFilled' : 'Star'"
            link
            :type="row.starred ? 'warning' : 'info'"
            @click="handleStar(row)"
          />
          <el-button link type="primary" icon="Edit" @click="goToDetail(row.id)"></el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)"></el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="paperStore.total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <PaperUploadDialog v-model="showUploadDialog" @success="handleUploadSuccess" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { usePaperStore } from '../../stores/paperStore'
import { useTagStore } from '../../stores/tagStore'
import { ReadingStatusLabel, ReadingStatusColor } from '../../types/enums'
import { togglePaperStar, deletePaper } from '../../api/paper'
import { ElMessage, ElMessageBox } from 'element-plus'
import PaperUploadDialog from './components/PaperUploadDialog.vue'

const router = useRouter()
const paperStore = usePaperStore()
const tagStore = useTagStore()

const showUploadDialog = ref(false)

const currentPage = computed({
  get: () => paperStore.filter.page + 1,
  set: (val) => paperStore.filter.page = val - 1
})
const pageSize = computed({
  get: () => paperStore.filter.size,
  set: (val) => paperStore.filter.size = val
})

const handleSearch = () => {
  paperStore.filter.page = 0
  paperStore.fetchPapers()
}

const handlePageChange = () => {
  paperStore.fetchPapers()
}

const getStatusColor = (status: string) => {
  return ReadingStatusColor[status as keyof typeof ReadingStatusColor] || '#999'
}

const goToDetail = (id: number) => {
  router.push(`/papers/${id}`)
}

const handleStar = async (row: any) => {
  try {
    const res = await togglePaperStar(row.id, !row.starred)
    if (res.code === 200) {
      row.starred = !row.starred
      ElMessage.success(row.starred ? '已星标' : '已取消星标')
    }
  } catch (error) {
    console.error(error)
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除这篇论文及其笔记吗？', '警告', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deletePaper(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        paperStore.fetchPapers()
      }
    } catch (error) {
      console.error(error)
    }
  })
}

const handleUploadSuccess = () => {
  paperStore.fetchPapers()
}

onMounted(() => {
  tagStore.fetchTags()
  paperStore.fetchPapers()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.left-tools {
  display: flex;
  align-items: center;
}

.title-cell {
  display: flex;
  flex-direction: column;
}

.title-text {
  font-weight: 500;
  color: var(--primary-color);
  cursor: pointer;
}

.title-text:hover {
  text-decoration: underline;
}

.meta-text {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.venue-tag {
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  margin-right: 6px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
