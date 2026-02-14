package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperCreateRequest;
import com.changye.web.dto.request.PaperUpdateRequest;
import com.changye.web.dto.request.StarUpdateRequest;
import com.changye.web.dto.request.StatusUpdateRequest;
import com.changye.web.dto.response.PaperResponse;
import com.changye.web.dto.response.TagResponse;
import com.changye.web.dto.response.VenueRankingResponse;
import com.changye.web.model.Note;
import com.changye.web.model.Paper;
import com.changye.web.model.Tag;
import com.changye.web.model.enums.ReadingStatus;
import com.changye.web.repository.NoteRepository;
import com.changye.web.repository.PaperRepository;
import com.changye.web.repository.TagRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@Slf4j
@Service
@Transactional
public class PaperService {

    private final PaperRepository paperRepository;
    private final NoteRepository noteRepository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;
    private final VenueRankingService venueRankingService;

    @Value("${app.upload.path}")
    private String uploadPath;

    public PaperService(PaperRepository paperRepository,
                        NoteRepository noteRepository,
                        TagRepository tagRepository,
                        ObjectMapper objectMapper,
                        VenueRankingService venueRankingService) {
        this.paperRepository = paperRepository;
        this.noteRepository = noteRepository;
        this.tagRepository = tagRepository;
        this.objectMapper = objectMapper;
        this.venueRankingService = venueRankingService;
    }

    @Transactional(readOnly = true)
    public Page<PaperResponse> listPapers(String keyword,
                                          ReadingStatus status,
                                          Long tagId,
                                          Boolean starred,
                                          Pageable pageable) {
        Specification<Paper> spec = buildSpecification(keyword, status, tagId, starred);
        Page<Paper> page = paperRepository.findAll(spec, pageable);
        return page.map(paper -> {
            long noteCount = noteRepository.countByPaperId(paper.getId());
            return toPaperResponse(paper, false, Math.toIntExact(noteCount));
        });
    }

    @Transactional(readOnly = true)
    public PaperResponse getPaper(Long id) {
        Paper paper = findPaper(id);
        List<Note> notes = noteRepository.findByPaperIdOrderBySortOrderAsc(id);
        return toPaperResponse(paper, true, notes.size(), notes);
    }

    public PaperResponse createPaper(MultipartFile file, PaperCreateRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        Set<Tag> tags = resolveTags(request.getTagIds());
        Paper paper = Paper.builder()
                .title(request.getTitle())
                .authors(writeAuthors(request.getAuthors()))
                .year(request.getYear())
                .venue(request.getVenue())
                .doi(request.getDoi())
                .abstractText(request.getAbstractText())
                .readingStatus(ReadingStatus.UNREAD)
                .starred(false)
                .tags(tags)
                .build();
        applyVenueRanking(paper);

        Paper saved = paperRepository.save(paper);
        storeFile(saved, file);
        Paper updated = paperRepository.save(saved);
        log.info("Created paper id={} title={}", updated.getId(), updated.getTitle());
        return toPaperResponse(updated, false, 0);
    }

    public List<PaperResponse> createPapersFromImport(List<PaperCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        List<PaperResponse> responses = new ArrayList<>();
        for (PaperCreateRequest request : requests) {
            responses.add(createPaperFromImport(request));
        }
        return responses;
    }

    public PaperResponse updatePaper(Long id, PaperUpdateRequest request) {
        Paper paper = findPaper(id);
        paper.setTitle(request.getTitle());
        paper.setAuthors(writeAuthors(request.getAuthors()));
        paper.setYear(request.getYear());
        paper.setVenue(request.getVenue());
        paper.setDoi(request.getDoi());
        paper.setAbstractText(request.getAbstractText());

        if (request.getTagIds() != null) {
            paper.setTags(resolveTags(request.getTagIds()));
        }

        Paper updated = paperRepository.save(paper);
        log.info("Updated paper id={}", updated.getId());
        List<Note> notes = noteRepository.findByPaperIdOrderBySortOrderAsc(id);
        return toPaperResponse(updated, true, notes.size(), notes);
    }

    public PaperResponse updateStatus(Long id, StatusUpdateRequest request) {
        Paper paper = findPaper(id);
        paper.setReadingStatus(request.getReadingStatus());
        Paper updated = paperRepository.save(paper);
        log.info("Updated paper status id={} status={}", id, request.getReadingStatus());
        List<Note> notes = noteRepository.findByPaperIdOrderBySortOrderAsc(id);
        return toPaperResponse(updated, true, notes.size(), notes);
    }

    public PaperResponse toggleStar(Long id, StarUpdateRequest request) {
        Paper paper = findPaper(id);
        paper.setStarred(request.getStarred());
        Paper updated = paperRepository.save(paper);
        log.info("Updated paper star id={} starred={}", id, request.getStarred());
        List<Note> notes = noteRepository.findByPaperIdOrderBySortOrderAsc(id);
        return toPaperResponse(updated, true, notes.size(), notes);
    }

    public void deletePaper(Long id) {
        Paper paper = findPaper(id);
        deleteFile(paper);
        paperRepository.delete(paper);
        log.info("Deleted paper id={}", id);
    }

    @Transactional(readOnly = true)
    public Resource getPaperFile(Long id) {
        Paper paper = findPaper(id);
        if (!StringUtils.hasText(paper.getFilePath())) {
            throw new BusinessException(404, "文件不存在");
        }

        Path path = Paths.get(paper.getFilePath());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(404, "文件不存在");
            }
            return resource;
        } catch (IOException ex) {
            throw new BusinessException(500, "文件读取失败");
        }
    }

    private Paper findPaper(Long id) {
        return paperRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
    }

    private PaperResponse createPaperFromImport(PaperCreateRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(400, "论文标题不能为空");
        }
        Set<Tag> tags = resolveTags(request.getTagIds());
        Paper paper = Paper.builder()
                .title(request.getTitle())
                .authors(writeAuthors(request.getAuthors()))
                .year(request.getYear())
                .venue(request.getVenue())
                .doi(request.getDoi())
                .abstractText(request.getAbstractText())
                .readingStatus(ReadingStatus.UNREAD)
                .starred(false)
                .tags(tags)
                .build();
        applyVenueRanking(paper);
        Paper saved = paperRepository.save(paper);
        log.info("Imported paper id={} title={}", saved.getId(), saved.getTitle());
        return toPaperResponse(saved, false, 0);
    }

    private void storeFile(Paper paper, MultipartFile file) {
        try {
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            String originalName = sanitizeFileName(file.getOriginalFilename());
            String storedName = paper.getId() + "_" + originalName;
            Path target = uploadDir.resolve(storedName);
            file.transferTo(target);
            paper.setFilePath(target.toString());
            paper.setFileName(originalName);
            paper.setFileSize(file.getSize());
        } catch (IOException ex) {
            log.error("Failed to store file for paper id={}", paper.getId(), ex);
            throw new BusinessException(500, "文件保存失败");
        }
    }

    private void deleteFile(Paper paper) {
        if (!StringUtils.hasText(paper.getFilePath())) {
            return;
        }
        try {
            Path path = Paths.get(paper.getFilePath());
            if (Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException ex) {
            log.error("Failed to delete file for paper id={}", paper.getId(), ex);
            throw new BusinessException(500, "文件删除失败");
        }
    }

    private Specification<Paper> buildSpecification(String keyword,
                                                    ReadingStatus status,
                                                    Long tagId,
                                                    Boolean starred) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(builder.equal(root.get("readingStatus"), status));
            }
            if (starred != null) {
                predicates.add(builder.equal(root.get("starred"), starred));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                Predicate titleLike = builder.like(builder.lower(root.get("title")), like.toLowerCase());
                Predicate authorsLike = builder.like(builder.lower(root.get("authors")), like.toLowerCase());
                Predicate abstractLike = builder.like(builder.lower(root.get("abstractText")), like.toLowerCase());
                predicates.add(builder.or(titleLike, authorsLike, abstractLike));
            }
            if (tagId != null) {
                Join<Paper, Tag> tagJoin = root.join("tags", JoinType.LEFT);
                predicates.add(builder.equal(tagJoin.get("id"), tagId));
                query.distinct(true);
            }

            return predicates.isEmpty()
                    ? builder.conjunction()
                    : builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Set<Tag> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }
        List<Tag> tags = tagRepository.findAllById(tagIds);
        if (tags.size() != new HashSet<>(tagIds).size()) {
            throw new BusinessException(404, "标签不存在");
        }
        return new HashSet<>(tags);
    }

    private String writeAuthors(List<String> authors) {
        if (authors == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(authors);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "作者信息序列化失败");
        }
    }

    private List<String> readAuthors(String authorsJson) {
        if (!StringUtils.hasText(authorsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(authorsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "作者信息解析失败");
        }
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "paper.pdf";
        }
        return Paths.get(fileName).getFileName().toString();
    }

    private void applyVenueRanking(Paper paper) {
        if (!StringUtils.hasText(paper.getVenue())) {
            return;
        }
        VenueRankingResponse ranking = venueRankingService.lookup(paper.getVenue());
        if (ranking == null) {
            return;
        }
        paper.setCcfRank(ranking.getCcfRank());
        paper.setJcrQuartile(ranking.getJcrQuartile());
        paper.setImpactFactor(ranking.getImpactFactor());
    }

    private PaperResponse toPaperResponse(Paper paper, boolean includeNotes, int noteCount) {
        return toPaperResponse(paper, includeNotes, noteCount, List.of());
    }

    private PaperResponse toPaperResponse(Paper paper,
                                          boolean includeNotes,
                                          int noteCount,
                                          List<Note> notes) {
        List<TagResponse> tagResponses = paper.getTags().stream()
                .map(tag -> TagResponse.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .color(tag.getColor())
                        .build())
                .toList();

        List<PaperResponse.NoteSummaryResponse> noteSummaries = includeNotes
                ? notes.stream()
                .map(note -> PaperResponse.NoteSummaryResponse.builder()
                        .id(note.getId())
                        .title(note.getTitle())
                        .sortOrder(note.getSortOrder())
                        .createdAt(note.getCreatedAt())
                        .updatedAt(note.getUpdatedAt())
                        .build())
                .toList()
                : null;

        return PaperResponse.builder()
                .id(paper.getId())
                .title(paper.getTitle())
                .authors(readAuthors(paper.getAuthors()))
                .year(paper.getYear())
                .venue(paper.getVenue())
                .doi(paper.getDoi())
                .fileName(paper.getFileName())
                .fileSize(paper.getFileSize())
                .filePath("/api/v1/papers/" + paper.getId() + "/file")
                .readingStatus(paper.getReadingStatus())
                .starred(paper.getStarred())
                .abstractText(paper.getAbstractText())
                .ccfRank(paper.getCcfRank())
                .jcrQuartile(paper.getJcrQuartile())
                .impactFactor(paper.getImpactFactor())
                .citationCount(paper.getCitationCount())
                .paperUrl(paper.getPaperUrl())
                .tags(tagResponses)
                .notes(noteSummaries)
                .noteCount(noteCount)
                .createdAt(paper.getCreatedAt())
                .updatedAt(paper.getUpdatedAt())
                .build();
    }
}
