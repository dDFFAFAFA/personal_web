package com.changye.web.controller;

import com.changye.web.common.ApiResponse;
import com.changye.web.dto.request.NoteCreateRequest;
import com.changye.web.dto.request.NoteUpdateRequest;
import com.changye.web.dto.response.NoteResponse;
import com.changye.web.service.NoteService;

import jakarta.validation.Valid;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Validated
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping("/papers/{paperId}/notes")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> listNotes(@PathVariable Long paperId) {
        List<NoteResponse> response = noteService.listNotes(paperId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/papers/{paperId}/notes")
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(
            @PathVariable Long paperId,
            @Valid @RequestBody NoteCreateRequest request) {
        NoteResponse response = noteService.createNote(paperId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/notes/{noteId}")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(
            @PathVariable Long noteId,
            @Valid @RequestBody NoteUpdateRequest request) {
        NoteResponse response = noteService.updateNote(noteId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<ApiResponse<Void>> deleteNote(@PathVariable Long noteId) {
        noteService.deleteNote(noteId);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
