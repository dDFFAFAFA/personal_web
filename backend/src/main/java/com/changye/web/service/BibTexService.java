package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.request.PaperCreateRequest;
import com.changye.web.model.Paper;
import com.changye.web.repository.PaperRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class BibTexService {

    private final PaperRepository paperRepository;
    private final ObjectMapper objectMapper;

    public BibTexService(PaperRepository paperRepository, ObjectMapper objectMapper) {
        this.paperRepository = paperRepository;
        this.objectMapper = objectMapper;
    }

    public String exportBibTeX(List<Long> paperIds) {
        List<Paper> papers = fetchPapers(paperIds);
        StringBuilder builder = new StringBuilder();
        for (Paper paper : papers) {
            builder.append(toBibEntry(paper))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    public String exportRis(List<Long> paperIds) {
        List<Paper> papers = fetchPapers(paperIds);
        StringBuilder builder = new StringBuilder();
        for (Paper paper : papers) {
            builder.append("TY  - JOUR").append(System.lineSeparator());
            appendIfPresent(builder, "TI", paper.getTitle());
            for (String author : readAuthors(paper.getAuthors())) {
                appendIfPresent(builder, "AU", author);
            }
            if (paper.getYear() != null) {
                appendIfPresent(builder, "PY", paper.getYear().toString());
            }
            appendIfPresent(builder, "JO", paper.getVenue());
            appendIfPresent(builder, "DO", paper.getDoi());
            builder.append("ER  -").append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        return builder.toString().trim();
    }

    public List<PaperCreateRequest> parseBibTeX(InputStream input) {
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            BibTeXParser parser = new BibTeXParser();
            BibTeXDatabase database = parser.parse(reader);
            List<PaperCreateRequest> requests = new ArrayList<>();
            for (BibTeXEntry entry : database.getEntries().values()) {
                String title = readField(entry, "title");
                if (!StringUtils.hasText(title)) {
                    throw new BusinessException(400, "BibTeX 缺少 title");
                }
                PaperCreateRequest request = new PaperCreateRequest();
                request.setTitle(title);
                request.setAuthors(parseAuthors(readField(entry, "author")));
                request.setYear(parseYear(readField(entry, "year")));
                request.setVenue(firstText(readField(entry, "journal"), readField(entry, "booktitle")));
                request.setDoi(readField(entry, "doi"));
                request.setAbstractText(readField(entry, "abstract"));
                requests.add(request);
            }
            return requests;
        } catch (Exception ex) {
            log.warn("Failed to parse BibTeX file", ex);
            throw new BusinessException(400, "BibTeX 解析失败");
        }
    }

    public List<PaperCreateRequest> parseRis(InputStream input) {
        List<PaperCreateRequest> results = new ArrayList<>();
        Map<String, List<String>> fields = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                if (line.startsWith("ER")) {
                    addRisEntry(results, fields);
                    fields.clear();
                    continue;
                }
                if (line.length() < 6) {
                    continue;
                }
                String tag = line.substring(0, 2).trim().toUpperCase(Locale.ROOT);
                String value = line.substring(6).trim();
                fields.computeIfAbsent(tag, key -> new ArrayList<>()).add(value);
            }
            if (!fields.isEmpty()) {
                addRisEntry(results, fields);
            }
        } catch (IOException ex) {
            log.warn("Failed to parse RIS file", ex);
            throw new BusinessException(400, "RIS 解析失败");
        }
        return results;
    }

    private List<Paper> fetchPapers(List<Long> paperIds) {
        if (paperIds == null || paperIds.isEmpty()) {
            throw new BusinessException(400, "论文 ID 不能为空");
        }
        return paperRepository.findAllById(paperIds);
    }

    private void addRisEntry(List<PaperCreateRequest> results, Map<String, List<String>> fields) {
        String title = firstText(fields.get("TI"), fields.get("T1"));
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(400, "RIS 缺少 title");
        }
        PaperCreateRequest request = new PaperCreateRequest();
        request.setTitle(title);
        request.setAuthors(fields.getOrDefault("AU", List.of()));
        request.setYear(parseYear(firstText(fields.get("PY"), fields.get("Y1"))));
        request.setVenue(firstText(fields.get("JO"), fields.get("T2")));
        request.setDoi(firstText(fields.get("DO"), null));
        request.setAbstractText(firstText(fields.get("AB"), null));
        results.add(request);
    }

    private String toBibEntry(Paper paper) {
        StringBuilder builder = new StringBuilder();
        String key = "paper" + (paper.getId() == null ? "" : paper.getId());
        builder.append("@article{").append(key).append(",").append(System.lineSeparator());
        appendBibField(builder, "title", paper.getTitle());
        String authors = String.join(" and ", readAuthors(paper.getAuthors()));
        if (StringUtils.hasText(authors)) {
            appendBibField(builder, "author", authors);
        }
        appendBibField(builder, "journal", paper.getVenue());
        if (paper.getYear() != null) {
            appendBibField(builder, "year", paper.getYear().toString());
        }
        appendBibField(builder, "doi", paper.getDoi());
        trimTrailingComma(builder);
        builder.append("}");
        return builder.toString();
    }

    private void appendBibField(StringBuilder builder, String key, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append("  ").append(key).append("={").append(value).append("},")
                .append(System.lineSeparator());
    }

    private void trimTrailingComma(StringBuilder builder) {
        int index = builder.lastIndexOf("," + System.lineSeparator());
        if (index > 0 && index == builder.length() - System.lineSeparator().length() - 1) {
            builder.deleteCharAt(index);
        }
    }

    private void appendIfPresent(StringBuilder builder, String tag, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(tag).append("  - ").append(value).append(System.lineSeparator());
    }

    private List<String> parseAuthors(String authors) {
        if (!StringUtils.hasText(authors)) {
            return List.of();
        }
        return Arrays.stream(authors.split("\\s+and\\s+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    private Integer parseYear(String yearText) {
        if (!StringUtils.hasText(yearText)) {
            return null;
        }
        String trimmed = yearText.trim();
        if (trimmed.length() >= 4) {
            trimmed = trimmed.substring(0, 4);
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String readField(BibTeXEntry entry, String field) {
        Value value = entry.getField(new Key(field));
        return value == null ? null : value.toUserString();
    }

    private String firstText(List<String> values, List<String> fallback) {
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        if (fallback != null && !fallback.isEmpty()) {
            return fallback.get(0);
        }
        return null;
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private List<String> readAuthors(String authorsJson) {
        if (!StringUtils.hasText(authorsJson)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(authorsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse authors JSON", ex);
            return List.of();
        }
    }
}
