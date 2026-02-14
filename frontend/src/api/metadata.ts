import request from './request'
import type { ApiResponse } from '../types/api'
import type { MetadataEnrichResponse, VenueRankingResponse, Paper } from '../types/paper'

export const enrichByDoi = (doi: string) =>
  request.post<any, ApiResponse<MetadataEnrichResponse>>('/papers/enrich/doi', { doi })

export const enrichByTitle = (title: string) =>
  request.post<any, ApiResponse<MetadataEnrichResponse>>('/papers/enrich/title', { title })

export const enrichPaper = (paperId: number) =>
  request.post<any, ApiResponse<Paper>>(`/papers/${paperId}/enrich`)

export const lookupVenue = (name: string) =>
  request.get<any, ApiResponse<VenueRankingResponse>>('/venues/lookup', { params: { name } })
