export type ReadingStatus = 'UNREAD' | 'SKIMMED' | 'HALF_READ' | 'FINISHED' | 'NEED_REREAD'

export const ReadingStatusLabel: Record<ReadingStatus, string> = {
  UNREAD: '未读',
  SKIMMED: '读了一点',
  HALF_READ: '读了一半',
  FINISHED: '精读完成',
  NEED_REREAD: '需要重读',
}

export const ReadingStatusColor: Record<ReadingStatus, string> = {
  UNREAD: '#9e9e9e',
  SKIMMED: '#2196f3',
  HALF_READ: '#ff9800',
  FINISHED: '#4caf50',
  NEED_REREAD: '#f44336',
}
