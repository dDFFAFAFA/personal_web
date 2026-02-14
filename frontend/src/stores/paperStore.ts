import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import { getPapers } from '../api/paper'
import type { Paper, PaperFilter } from '../types/paper'

export const usePaperStore = defineStore('paper', () => {
  const papers = ref<Paper[]>([])
  const total = ref(0)
  const loading = ref(false)

  const filter = reactive<PaperFilter>({
    page: 0,
    size: 20,
    sort: 'createdAt,desc',
    status: undefined,
    tagId: undefined,
    starred: undefined,
    keyword: '',
    ccfRank: undefined
  })

  const fetchPapers = async () => {
    loading.value = true
    try {
      const res = await getPapers(filter)
      if (res.code === 200) {
        papers.value = res.data.content
        total.value = res.data.totalElements
      }
    } catch (error) {
      console.error(error)
    } finally {
      loading.value = false
    }
  }

  const resetFilter = () => {
    filter.page = 0
    filter.keyword = ''
    filter.status = undefined
    filter.tagId = undefined
    filter.starred = undefined
    fetchPapers()
  }

  return {
    papers,
    total,
    loading,
    filter,
    fetchPapers,
    resetFilter
  }
})
