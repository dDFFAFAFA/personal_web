import request from './request'
import type { ApiResponse } from '../types/api'
import type { Paper } from '../types/paper'

export const exportPapers = (format: 'bibtex' | 'ris', ids?: number[]) =>
  request.get<any, Blob>('/papers/export', {
    params: { format, ids: ids?.join(',') },
    responseType: 'blob',
    timeout: 30000
  })

export const importPapers = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, ApiResponse<Paper[]>>('/papers/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000
  })
}
