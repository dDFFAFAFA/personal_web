import request from './request'
import type { ApiResponse } from '../types/api'
import type { Note } from '../types/paper'

const USE_MOCK = true

const mockNotes: Note[] = [
  {
    id: 1,
    paperId: 1,
    title: 'Initial Thoughts',
    content: '# Great Paper\n\nReally interesting approach to attention.',
    sortOrder: 1,
    createdAt: '2023-01-16T10:00:00Z',
    updatedAt: '2023-01-16T10:00:00Z'
  },
  {
    id: 2,
    paperId: 1,
    title: 'Key Architecture',
    content: '## Encoder-Decoder\n\nCrucial for understanding.',
    sortOrder: 2,
    createdAt: '2023-01-17T11:00:00Z',
    updatedAt: '2023-01-17T11:00:00Z'
  }
]

export const getPaperNotes = (paperId: number) => {
  if (USE_MOCK) {
    const notes = mockNotes.filter(n => n.paperId === paperId)
    return Promise.resolve({
      code: 200,
      message: 'success',
      data: notes,
      timestamp: new Date().toISOString()
    } as ApiResponse<Note[]>)
  }
  return request.get<any, ApiResponse<Note[]>>(`/papers/${paperId}/notes`)
}

export const createNote = (paperId: number, data: { title: string; content: string }) => {
  if (USE_MOCK) {
    const newNote: Note = {
      id: mockNotes.length + 100,
      paperId,
      title: data.title,
      content: data.content,
      sortOrder: mockNotes.length + 1,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    }
    mockNotes.push(newNote)
    return Promise.resolve({
      code: 200,
      message: 'success',
      data: newNote,
      timestamp: new Date().toISOString()
    } as ApiResponse<Note>)
  }
  return request.post<any, ApiResponse<Note>>(`/papers/${paperId}/notes`, data)
}

export const updateNote = (noteId: number, data: { title: string; content: string; sortOrder?: number }) => {
  if (USE_MOCK) {
    const note = mockNotes.find(n => n.id === noteId)
    if (note) {
      note.title = data.title
      note.content = data.content
      if (data.sortOrder) note.sortOrder = data.sortOrder
      note.updatedAt = new Date().toISOString()
    }
    return Promise.resolve({
      code: 200,
      message: 'success',
      data: note,
      timestamp: new Date().toISOString()
    } as ApiResponse<Note>)
  }
  return request.put<any, ApiResponse<Note>>(`/notes/${noteId}`, data)
}

export const deleteNote = (noteId: number) => {
  if (USE_MOCK) {
    const idx = mockNotes.findIndex(n => n.id === noteId)
    if (idx > -1) mockNotes.splice(idx, 1)
    return Promise.resolve({
      code: 200,
      message: 'success',
      data: null,
      timestamp: new Date().toISOString()
    } as ApiResponse<null>)
  }
  return request.delete<any, ApiResponse<null>>(`/notes/${noteId}`)
}
