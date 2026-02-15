export type RepoProvider = 'GITHUB' | 'GITEE'
export type RepoSyncStatus = 'IDLE' | 'SUCCESS' | 'FAILED'
export type RepoSyncMode = 'CLONE' | 'PULL'

export interface RepoConfig {
  provider?: RepoProvider
  repoUrl?: string
  branch?: string
  targetDir?: string
  autoCommitReadme?: boolean
  appBaseUrl?: string
  sshKeyPath?: string
  configured: boolean
}

export interface RepoConfigUpdateRequest {
  provider: RepoProvider
  repoUrl: string
  branch: string
  targetDir: string
  autoCommitReadme?: boolean
  appBaseUrl?: string
}

export interface RepoSyncStatusResponse {
  status: RepoSyncStatus
  mode?: RepoSyncMode
  message?: string
  syncedAt?: string
}

export type PaperCodeSyncItemStatus = 'SUCCESS' | 'FAILED' | 'SKIPPED' | 'PENDING' | string

export interface PaperCodeSyncRequest {
  paperId?: number
}

export interface PaperCodeSyncItemResult {
  paperId?: number
  paperTitle?: string
  repoUrl?: string
  status?: PaperCodeSyncItemStatus
  message?: string
  syncedAt?: string
}

export interface PaperCodeSyncResponse {
  taskId?: string
  status?: string
  message?: string
  totalCount?: number
  successCount?: number
  failedCount?: number
  skippedCount?: number
  startedAt?: string
  finishedAt?: string
  progress?: number
  results?: PaperCodeSyncItemResult[]
}

export interface PaperCodeEntry {
  id: number
  paperId: number
  paperTitle: string
  repoUrl: string
  branch?: string
  provider: RepoProvider
  description?: string
  createdAt?: string
  updatedAt?: string
}

export interface PaperCodeEntryPayload {
  paperId: number
  repoUrl: string
  branch?: string
  provider: RepoProvider
  description?: string
}
