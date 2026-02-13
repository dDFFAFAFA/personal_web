<template>
  <div class="tags-container" data-testid="tag-list-page">
    <div class="header">
      <h2>标签管理</h2>
      <el-button type="primary" icon="Plus" @click="handleAdd">新增标签</el-button>
    </div>

    <el-table :data="tagStore.tags" style="width: 100%; margin-top: 20px;">
      <el-table-column label="名称" prop="name">
        <template #default="{ row }">
          <el-tag :color="row.color + '20'" :style="{ color: row.color, borderColor: 'transparent' }">
            {{ row.name }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="颜色" prop="color">
        <template #default="{ row }">
          <div style="display: flex; align-items: center;">
            <div :style="{ width: '20px', height: '20px', backgroundColor: row.color, borderRadius: '4px', marginRight: '8px' }"></div>
            {{ row.color }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="关联论文数" prop="paperCount" />
      <el-table-column label="创建时间" prop="createdAt">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleDateString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" icon="Edit" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑标签' : '新增标签'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-color-picker v-model="form.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useTagStore } from '../../stores/tagStore'
import { ElMessageBox } from 'element-plus'

const tagStore = useTagStore()
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref(0)

const form = reactive({
  name: '',
  color: '#409EFF'
})

const handleAdd = () => {
  isEdit.value = false
  form.name = ''
  form.color = '#409EFF'
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  currentId.value = row.id
  form.name = row.name
  form.color = row.color
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定删除该标签？', '警告', { type: 'warning' })
    .then(() => {
      tagStore.removeTag(row.id)
    })
}

const submitForm = async () => {
  if (isEdit.value) {
    await tagStore.editTag(currentId.value, { ...form })
  } else {
    await tagStore.addTag({ ...form })
  }
  dialogVisible.value = false
}

onMounted(() => {
  tagStore.fetchTags()
})
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
