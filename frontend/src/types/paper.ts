import type { ReadingStatus } from './enums'
import type { RepoProvider } from './repo'

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
  ccfRank?: string
  jcrQuartile?: string
  impactFactor?: number
  citationCount?: number
  paperUrl?: string
  backupStatus?: 'NOT_BACKED_UP' | 'BACKED_UP' | 'FAILED'
  backupAt?: string
  backupError?: string
}

export interface PaperFilter {
  page: number
  size: number
  sort?: string
  status?: ReadingStatus
  tagId?: number
  starred?: boolean
  keyword?: string
  ccfRank?: string
}

export interface MetadataEnrichResponse {
  title: string
  authors: string[]
  year: number
  venue: string
  doi: string
  abstractText: string
  paperUrl: string
  citationCount: number
  ccfRank?: string
  jcrQuartile?: string
  source: string
}

export interface VenueRankingResponse {
  venue: string
  ccfRank?: string
  jcrQuartile?: string
  impactFactor?: number
  category: string
  type: string
}

export type PaperSummaryStatus = 'PENDING' | 'QUEUED' | 'GENERATING' | 'SUCCESS' | 'FAILED'

export interface PaperSummary {
  paperId: number
  summaryId?: number
  status: PaperSummaryStatus
  provider?: string
  model?: string
  markdown?: string
  generatedAt?: string
  error?: string
  message?: string
}

export interface HomeSummaryItem {
  paperId: number
  summaryId?: number
  paperTitle?: string
  title?: string
  modelName?: string
  oneSentence?: string
  status: PaperSummaryStatus
  generatedAt?: string
  message?: string
}

export type RepoLinkStatus = 'CANDIDATE' | 'APPLIED' | 'REJECTED'
export type RepoLinkProvider = RepoProvider | 'UNKNOWN'

export interface PaperRepoLinkCandidate {
  id: number
  url: string
  provider: RepoLinkProvider
  status?: RepoLinkStatus
  confidence?: number
  sourceText?: string
  pageNo?: number
}

export interface PaperRepoLinkListResponse {
  paperId: number
  candidateCount?: number
  candidates: PaperRepoLinkCandidate[]
  message?: string
}
