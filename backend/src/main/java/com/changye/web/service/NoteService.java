package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.NoteCreateRequest;
import com.changye.web.dto.request.NoteUpdateRequest;
import com.changye.web.dto.response.NoteResponse;
import com.changye.web.model.Note;
import com.changye.web.model.Paper;
import com.changye.web.repository.NoteRepository;
import com.changye.web.repository.PaperRepository;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class NoteService {

    private final NoteRepository noteRepository;
    private final PaperRepository paperRepository;

    public NoteService(NoteRepository noteRepository, PaperRepository paperRepository) {
        this.noteRepository = noteRepository;
        this.paperRepository = paperRepository;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> listNotes(Long paperId) {
        List<Note> notes = noteRepository.findByPaperIdOrderBySortOrderAsc(paperId);
        return notes.stream().map(this::toNoteResponse).toList();
    }

    public NoteResponse createNote(Long paperId, NoteCreateRequest request) {
        Paper paper = findPaper(paperId);
        Note note = Note.builder()
                .paper(paper)
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        Note saved = noteRepository.save(note);
        log.info("Created note id={} for paper id={}", saved.getId(), paperId);
        return toNoteResponse(saved);
    }

    public NoteResponse updateNote(Long noteId, NoteUpdateRequest request) {
        Note note = findNote(noteId);
        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        if (request.getSortOrder() != null) {
            note.setSortOrder(request.getSortOrder());
        }
        Note saved = noteRepository.save(note);
        log.info("Updated note id={}", noteId);
        return toNoteResponse(saved);
    }

    public void deleteNote(Long noteId) {
        Note note = findNote(noteId);
        noteRepository.delete(note);
        log.info("Deleted note id={}", noteId);
    }

    private Paper findPaper(Long paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
    }

    private Note findNote(Long noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new BusinessException(404, "笔记不存在"));
    }

    private NoteResponse toNoteResponse(Note note) {
        return NoteResponse.builder()
                .id(note.getId())
                .paperId(note.getPaper().getId())
                .title(note.getTitle())
                .content(note.getContent())
                .sortOrder(note.getSortOrder())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
