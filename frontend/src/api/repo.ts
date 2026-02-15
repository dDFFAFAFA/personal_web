import request from './request'
import type { ApiResponse } from '../types/api'
import type {
  RepoConfig,
  RepoConfigUpdateRequest,
  RepoSyncStatusResponse,
  PaperCodeEntry,
  PaperCodeEntryPayload,
  PaperCodeSyncRequest,
  PaperCodeSyncResponse
} from '../types/repo'

export const getRepoConfig = () =>
  request.get<any, ApiResponse<RepoConfig>>('/repo/config')

export const updateRepoConfig = (payload: RepoConfigUpdateRequest) =>
  request.put<any, ApiResponse<RepoConfig>>('/repo/config', payload)

export const syncRepo = () =>
  request.post<any, ApiResponse<RepoSyncStatusResponse>>('/repo/sync')

export const getRepoSyncStatus = () =>
  request.get<any, ApiResponse<RepoSyncStatusResponse>>('/repo/sync-status')

export const syncPaperCodeToRepo = (payload: PaperCodeSyncRequest = {}) =>
  request.post<any, ApiResponse<PaperCodeSyncResponse>>('/repo/entries/sync', payload)

export const listCodeEntries = (paperId?: number) =>
  request.get<any, ApiResponse<PaperCodeEntry[]>>('/repo/code-entries', {
    params: paperId ? { paperId } : undefined
  })

export const createCodeEntry = (payload: PaperCodeEntryPayload) =>
  request.post<any, ApiResponse<PaperCodeEntry>>('/repo/code-entries', payload)

export const updateCodeEntry = (id: number, payload: PaperCodeEntryPayload) =>
  request.put<any, ApiResponse<PaperCodeEntry>>(`/repo/code-entries/${id}`, payload)

export const deleteCodeEntry = (id: number) =>
  request.delete<any, ApiResponse<null>>(`/repo/code-entries/${id}`)

export const rebuildRepoReadme = () =>
  request.post<any, ApiResponse<null>>('/repo/readme/rebuild')
