import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getTags, createTag, updateTag, deleteTag } from '../api/tag'
import type { Tag } from '../types/paper'
import { ElMessage } from 'element-plus'

export const useTagStore = defineStore('tag', () => {
  const tags = ref<Tag[]>([])
  const loading = ref(false)

  const fetchTags = async () => {
    loading.value = true
    try {
      const res = await getTags()
      if (res.code === 200) {
        tags.value = res.data
      }
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  }

  const addTag = async (data: { name: string; color: string }) => {
    try {
      const res = await createTag(data)
      if (res.code === 200) {
        tags.value.push(res.data)
        ElMessage.success('标签创建成功')
      }
    } catch (error) {
      console.error(error)
    }
  }

  const editTag = async (id: number, data: { name: string; color: string }) => {
    try {
      const res = await updateTag(id, data)
      if (res.code === 200) {
        const index = tags.value.findIndex(t => t.id === id)
        if (index > -1) tags.value[index] = res.data
        ElMessage.success('标签更新成功')
      }
    } catch (error) {
      console.error(error)
    }
  }

  const removeTag = async (id: number) => {
    try {
      const res = await deleteTag(id)
      if (res.code === 200) {
        tags.value = tags.value.filter(t => t.id !== id)
        ElMessage.success('标签删除成功')
      }
    } catch (error) {
      console.error(error)
    }
  }

  return {
    tags,
    loading,
    fetchTags,
    addTag,
    editTag,
    removeTag
  }
})
