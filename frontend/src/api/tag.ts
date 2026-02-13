import request from './request'
import type { ApiResponse } from '../types/api'
import type { Tag } from '../types/paper'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

const mockTags: Tag[] = [
  { id: 1, name: 'NLP', color: '#409EFF', paperCount: 12, createdAt: '2026-02-13T00:00:00+08:00' },
  { id: 2, name: 'CV', color: '#67C23A', paperCount: 8, createdAt: '2026-02-13T00:00:00+08:00' },
  { id: 3, name: 'LLM', color: '#E6A23C', paperCount: 5, createdAt: '2026-02-13T00:00:00+08:00' },
  { id: 4, name: 'Transformer', color: '#F56C6C', paperCount: 3, createdAt: '2026-02-13T00:00:00+08:00' },
]

export const getTags = () => {
  if (USE_MOCK) {
    return Promise.resolve({
      code: 200,
      message: 'success',
      data: mockTags,
      timestamp: new Date().toISOString()
    } as ApiResponse<Tag[]>)
  }
  return request.get<any, ApiResponse<Tag[]>>('/tags')
}

export const createTag = (data: { name: string; color: string }) => {
  if (USE_MOCK) {
    const newTag: Tag = {
      id: mockTags.length + 1,
      name: data.name,
      color: data.color,
      paperCount: 0,
      createdAt: new Date().toISOString()
    }
    mockTags.push(newTag)
    return Promise.resolve({
      code: 200,
      message: 'success',
      data: newTag,
      timestamp: new Date().toISOString()
    } as ApiResponse<Tag>)
  }
  return request.post<any, ApiResponse<Tag>>('/tags', data)
}

export const updateTag = (id: number, data: { name: string; color: string }) => {
  if (USE_MOCK) {
    const tag = mockTags.find(t => t.id === id)
    if (tag) {
      tag.name = data.name
      tag.color = data.color
    }
    return Promise.resolve({
      code: 200,
      message: 'success',
      data: tag,
      timestamp: new Date().toISOString()
    } as ApiResponse<Tag>)
  }
  return request.put<any, ApiResponse<Tag>>(`/tags/${id}`, data)
}

export const deleteTag = (id: number) => {
  if (USE_MOCK) {
    const index = mockTags.findIndex(t => t.id === id)
    if (index > -1) mockTags.splice(index, 1)
    return Promise.resolve({
      code: 200,
      message: 'success',
      data: null,
      timestamp: new Date().toISOString()
    } as ApiResponse<null>)
  }
  return request.delete<any, ApiResponse<null>>(`/tags/${id}`)
}
