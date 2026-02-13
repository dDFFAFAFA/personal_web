import request from './request'
import type { ApiResponse, PageResponse } from '../types/api'
import type { Paper, PaperFilter } from '../types/paper'

const USE_MOCK = true

// Initial mock data
const mockPapers: Paper[] = [
  {
    id: 1,
    title: 'Attention Is All You Need',
    authors: ['Vaswani, A.', 'Shazeer, N.', 'Parmar, N.', 'Uszkoreit, J.'],
    year: 2017,
    venue: 'NeurIPS',
    doi: '10.48550/arXiv.1706.03762',
    fileName: 'attention.pdf',
    readingStatus: 'FINISHED',
    starred: true,
    tags: [
      { id: 1, name: 'NLP', color: '#409EFF' },
      { id: 4, name: 'Transformer', color: '#F56C6C' }
    ],
    noteCount: 2,
    createdAt: '2023-01-15T10:00:00Z',
    updatedAt: '2023-01-15T10:00:00Z'
  },
  {
    id: 2,
    title: 'BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding',
    authors: ['Devlin, J.', 'Chang, M.', 'Lee, K.', 'Toutanova, K.'],
    year: 2019,
    venue: 'NAACL',
    readingStatus: 'HALF_READ',
    starred: false,
    tags: [
      { id: 1, name: 'NLP', color: '#409EFF' },
      { id: 3, name: 'LLM', color: '#E6A23C' }
    ],
    noteCount: 0,
    createdAt: '2023-02-20T14:30:00Z',
    updatedAt: '2023-02-20T14:30:00Z'
  },
  {
    id: 3,
    title: 'Deep Residual Learning for Image Recognition',
    authors: ['He, K.', 'Zhang, X.', 'Ren, S.', 'Sun, J.'],
    year: 2016,
    venue: 'CVPR',
    readingStatus: 'UNREAD',
    starred: false,
    tags: [
      { id: 2, name: 'CV', color: '#67C23A' }
    ],
    noteCount: 0,
    createdAt: '2023-03-05T09:15:00Z',
    updatedAt: '2023-03-05T09:15:00Z'
  },
  {
    id: 4,
    title: 'An Image is Worth 16x16 Words: Transformers for Image Recognition at Scale',
    authors: ['Dosovitskiy, A.', 'Beyer, L.', 'Kolesnikov, A.'],
    year: 2021,
    venue: 'ICLR',
    readingStatus: 'SKIMMED',
    starred: true,
    tags: [
      { id: 2, name: 'CV', color: '#67C23A' },
      { id: 4, name: 'Transformer', color: '#F56C6C' }
    ],
    noteCount: 1,
    createdAt: '2023-04-10T16:45:00Z',
    updatedAt: '2023-04-10T16:45:00Z'
  }
]

export const getPapers = (params: PaperFilter) => {
  if (USE_MOCK) {
    let content = [...mockPapers]
    
    // Simple filtering logic
    if (params.keyword) {
      const k = params.keyword.toLowerCase()
      content = content.filter(p => p.title.toLowerCase().includes(k) || p.authors.some(a => a.toLowerCase().includes(k)))
    }
    if (params.status) {
      content = content.filter(p => p.readingStatus === params.status)
    }
    if (params.tagId) {
      content = content.filter(p => p.tags.some(t => t.id === params.tagId))
    }
    if (params.starred) {
      content = content.filter(p => p.starred)
    }

    return Promise.resolve({
      code: 200,
      message: 'success',
      data: {
        content,
        page: params.page,
        size: params.size,
        totalElements: content.length,
        totalPages: 1
      },
      timestamp: new Date().toISOString()
    } as ApiResponse<PageResponse<Paper>>)
  }
  return request.get<any, ApiResponse<PageResponse<Paper>>>('/papers', { params })
}

export const getPaperDetail = (id: number) => {
  if (USE_MOCK) {
    const paper = mockPapers.find(p => p.id === id)
    if (paper) {
      // Mock notes addition
      const detail = { ...paper, notes: [], abstractText: 'This is a mock abstract for ' + paper.title }
      return Promise.resolve({
        code: 200,
        message: 'success',
        data: detail,
        timestamp: new Date().toISOString()
      } as ApiResponse<Paper>)
    }
    return Promise.reject(new Error('Paper not found'))
  }
  return request.get<any, ApiResponse<Paper>>(`/papers/${id}`)
}

export const createPaper = (formData: FormData) => {
  if (USE_MOCK) {
    const newPaper: Paper = {
      id: mockPapers.length + 1,
      title: formData.get('title') as string,
      authors: JSON.parse(formData.get('authors') as string || '[]'),
      year: parseInt(formData.get('year') as string) || new Date().getFullYear(),
      readingStatus: 'UNREAD',
      starred: false,
      tags: [], // simplified for mock
      noteCount: 0,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    mockPapers.unshift(newPaper)
    return Promise.resolve({
      code: 200,
      message: 'success',
      data: newPaper,
      timestamp: new Date().toISOString()
    } as ApiResponse<Paper>)
  }
  return request.post<any, ApiResponse<Paper>>('/papers', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const updatePaperStatus = (id: number, status: string) => {
  if (USE_MOCK) {
    const p = mockPapers.find(p => p.id === id)
    if (p) p.readingStatus = status as any
    return Promise.resolve({ code: 200, message: 'success', data: p, timestamp: new Date().toISOString() } as ApiResponse<Paper>)
  }
  return request.patch<any, ApiResponse<Paper>>(`/papers/${id}/status`, { readingStatus: status })
}

export const togglePaperStar = (id: number, starred: boolean) => {
  if (USE_MOCK) {
    const p = mockPapers.find(p => p.id === id)
    if (p) p.starred = starred
    return Promise.resolve({ code: 200, message: 'success', data: p, timestamp: new Date().toISOString() } as ApiResponse<Paper>)
  }
  return request.patch<any, ApiResponse<Paper>>(`/papers/${id}/star`, { starred })
}

export const deletePaper = (id: number) => {
  if (USE_MOCK) {
    const idx = mockPapers.findIndex(p => p.id === id)
    if (idx > -1) mockPapers.splice(idx, 1)
    return Promise.resolve({ code: 200, message: 'success', data: null, timestamp: new Date().toISOString() } as ApiResponse<null>)
  }
  return request.delete<any, ApiResponse<null>>(`/papers/${id}`)
}
