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
