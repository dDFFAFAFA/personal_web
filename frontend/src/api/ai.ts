import request from './request'
import type { ApiResponse } from '../types/api'
import type { AiProvider, AiProviderConfig, AiProviderConfigPayload } from '../types/ai'

export const getAiConfigs = () =>
  request.get<any, ApiResponse<AiProviderConfig[]>>('/ai/providers')

export const updateAiConfig = (provider: AiProvider, payload: AiProviderConfigPayload) =>
  request.put<any, ApiResponse<AiProviderConfig>>(`/ai/providers/${provider}`, payload)
