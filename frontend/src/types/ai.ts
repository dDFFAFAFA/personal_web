export type AiProvider = 'DEEPSEEK' | 'QWEN'

export interface AiProviderConfig {
  provider: AiProvider
  enabled: boolean
  baseUrl?: string
  model?: string
  apiKeyMasked?: string
  updatedAt?: string
}

export interface AiProviderConfigPayload {
  enabled?: boolean
  baseUrl?: string
  model?: string
  apiKey?: string
}
