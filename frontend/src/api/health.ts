import request from './request'
import type { ApiResponse } from '../types/api'

export interface HealthStatus {
  status: string
  version: string
  timestamp: string
}

export const getHealth = () => {
  return request.get<any, ApiResponse<HealthStatus>>('/health')
}
