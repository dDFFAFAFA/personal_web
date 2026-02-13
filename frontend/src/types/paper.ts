import type { ReadingStatus } from './enums'

export interface Tag {
  id: number
  name: string
  color: string
  paperCount?: number
  createdAt?: string
}

export interface Note {
  id: number
  paperId: number
  title: string
  content: string
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface Paper {
  id: number
  title: string
  authors: string[]
  year?: number
  venue?: string
  doi?: string
  fileName?: string
  fileSize?: number
  filePath?: string
  readingStatus: ReadingStatus
  starred: boolean
  abstractText?: string
  tags: Tag[]
  notes?: Note[]
  noteCount: number
  createdAt: string
  updatedAt: string
}

export interface PaperFilter {
  page: number
  size: number
  sort?: string
  status?: ReadingStatus
  tagId?: number
  starred?: boolean
  keyword?: string
}
