package com.changye.web.service;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.response.MetadataEnrichResponse;
import com.changye.web.dto.response.VenueRankingResponse;
import com.changye.web.model.Paper;
import com.changye.web.repository.PaperRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Transactional
public class MetadataService {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT = "personal-web/0.1";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final VenueRankingService venueRankingService;
    private final PaperRepository paperRepository;
    private final PdfMetadataExtractorService pdfMetadataExtractorService;

    @Value("${app.metadata.semantic-scholar-api-key:}")
    private String semanticScholarApiKey;

    public MetadataService(RestTemplateBuilder restTemplateBuilder,
                           ObjectMapper objectMapper,
                           VenueRankingService venueRankingService,
                           PaperRepository paperRepository,
                           PdfMetadataExtractorService pdfMetadataExtractorService) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(TIMEOUT)
                .setReadTimeout(TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
        this.venueRankingService = venueRankingService;
        this.paperRepository = paperRepository;
        this.pdfMetadataExtractorService = pdfMetadataExtractorService;
    }

    @Transactional(readOnly = true)
    public MetadataEnrichResponse enrichByDoi(String doi) {
        String normalizedDoi = normalizeDoi(doi);
        if (!StringUtils.hasText(normalizedDoi)) {
            return emptyResponse("none");
        }
        MetadataEnrichResponse response = fetchFromCrossref(normalizedDoi);
        if (response == null) {
            response = fetchFromSemanticByDoi(normalizedDoi);
        }
        if (response == null && isArxivDoi(normalizedDoi)) {
            response = fetchFromSemanticByArxivId(toArxivId(normalizedDoi));
        }
        if (response != null && !StringUtils.hasText(response.getDoi())) {
            response.setDoi(normalizedDoi);
        }
        return response == null ? emptyResponse("none") : response;
    }

    @Transactional(readOnly = true)
    public MetadataEnrichResponse enrichByTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return emptyResponse("none");
        }
        MetadataEnrichResponse response = fetchFromSemanticByTitle(title);
        if (response == null) {
            response = fetchFromCrossrefByQuery(title, title);
        }
        return response == null ? emptyResponse("none") : response;
    }

    @Transactional(readOnly = true)
    public MetadataEnrichResponse enrichByFile(MultipartFile file, String fallbackTitle) {
        PdfMetadataExtractorService.ExtractedPdfMetadata extracted = pdfMetadataExtractorService.extract(file);
        if (StringUtils.hasText(extracted.doi())) {
            MetadataEnrichResponse byDoi = enrichByDoi(extracted.doi());
            if (hasSubstantialMetadata(byDoi)) {
                return byDoi;
            }
        }

        String title = StringUtils.hasText(extracted.title()) ? extracted.title() : normalizeFileTitle(fallbackTitle);
        if (StringUtils.hasText(title)) {
            MetadataEnrichResponse byTitle = enrichByTitle(title);
            if (hasSubstantialMetadata(byTitle)) {
                return byTitle;
            }
            if (byTitle != null && !StringUtils.hasText(byTitle.getTitle())) {
                byTitle.setTitle(title);
            }
            if (byTitle != null) {
                return byTitle;
            }
        }

        MetadataEnrichResponse empty = emptyResponse("none");
        if (StringUtils.hasText(title)) {
            empty.setTitle(title);
        }
        return empty;
    }

    public MetadataEnrichResponse enrichAndApply(Long paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
        MetadataEnrichResponse response;
        if (StringUtils.hasText(paper.getDoi())) {
            response = enrichByDoi(paper.getDoi());
        } else {
            response = enrichByTitle(paper.getTitle());
        }
        applyEnrichment(paperId, response);
        return response;
    }

    public void applyEnrichment(Long paperId, MetadataEnrichResponse response) {
        if (response == null) {
            return;
        }
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));

        if (StringUtils.hasText(response.getTitle())) {
            paper.setTitle(response.getTitle());
        }
        if (response.getAuthors() != null) {
            paper.setAuthors(writeAuthors(response.getAuthors()));
        }
        if (response.getYear() != null) {
            paper.setYear(response.getYear());
        }
        if (StringUtils.hasText(response.getVenue())) {
            paper.setVenue(response.getVenue());
        }
        if (StringUtils.hasText(response.getDoi())) {
            paper.setDoi(response.getDoi());
        }
        if (StringUtils.hasText(response.getAbstractText())) {
            paper.setAbstractText(response.getAbstractText());
        }
        if (StringUtils.hasText(response.getPaperUrl())) {
            paper.setPaperUrl(response.getPaperUrl());
        }
        if (response.getCitationCount() != null) {
            paper.setCitationCount(response.getCitationCount());
        }
        if (StringUtils.hasText(response.getCcfRank())) {
            paper.setCcfRank(response.getCcfRank());
        }
        if (StringUtils.hasText(response.getJcrQuartile())) {
            paper.setJcrQuartile(response.getJcrQuartile());
        }

        if ((!StringUtils.hasText(paper.getCcfRank()) || !StringUtils.hasText(paper.getJcrQuartile()))
                && StringUtils.hasText(paper.getVenue())) {
            VenueRankingResponse ranking = venueRankingService.lookup(paper.getVenue());
            if (ranking != null) {
                if (!StringUtils.hasText(paper.getCcfRank()) && StringUtils.hasText(ranking.getCcfRank())) {
                    paper.setCcfRank(ranking.getCcfRank());
                }
                if (!StringUtils.hasText(paper.getJcrQuartile()) && StringUtils.hasText(ranking.getJcrQuartile())) {
                    paper.setJcrQuartile(ranking.getJcrQuartile());
                }
                if (paper.getImpactFactor() == null && ranking.getImpactFactor() != null) {
                    paper.setImpactFactor(ranking.getImpactFactor());
                }
            }
        }

        paperRepository.save(paper);
        log.info("Applied metadata enrichment to paper id={}", paperId);
    }

    private MetadataEnrichResponse fetchFromCrossref(String doi) {
        String url = "https://api.crossref.org/works/{doi}";
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        UriComponentsBuilder.fromUriString(url).buildAndExpand(doi).encode().toUri(),
                        HttpMethod.GET,
                        new HttpEntity<>(buildHeaders()),
                        String.class
                );
                MetadataEnrichResponse parsed = parseCrossref(response.getBody());
                if (parsed != null) {
                    return enrichWithVenue(parsed, "crossref");
                }
            } catch (RestClientException ex) {
                log.warn("CrossRef request failed (attempt {}): {}", attempt, ex.getMessage());
            }
        }
        return null;
    }

    private MetadataEnrichResponse fetchFromSemanticByDoi(String doi) {
        String url = "https://api.semanticscholar.org/graph/v1/paper/DOI:{doi}?fields=title,authors,year,venue,abstract,citationCount,externalIds,url";
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        UriComponentsBuilder.fromUriString(url).buildAndExpand(doi).encode().toUri(),
                        HttpMethod.GET,
                        new HttpEntity<>(buildHeaders()),
                        String.class
                );
                MetadataEnrichResponse parsed = parseSemanticPaper(response.getBody());
                if (parsed != null) {
                    return enrichWithVenue(parsed, "semantic_scholar");
                }
            } catch (RestClientException ex) {
                log.warn("Semantic Scholar DOI request failed (attempt {}): {}", attempt, ex.getMessage());
            }
        }
        return null;
    }

    private MetadataEnrichResponse fetchFromSemanticByTitle(String title) {
        String url = "https://api.semanticscholar.org/graph/v1/paper/search";
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        UriComponentsBuilder.fromUriString(url)
                                .queryParam("query", title)
                                .queryParam("fields", "title,authors,year,venue,abstract,citationCount,externalIds,url")
                                .queryParam("limit", 1)
                                .build()
                                .encode()
                                .toUri(),
                        HttpMethod.GET,
                        new HttpEntity<>(buildHeaders()),
                        String.class
                );
                MetadataEnrichResponse parsed = parseSemanticSearch(response.getBody());
                if (parsed != null) {
                    return enrichWithVenue(parsed, "semantic_scholar");
                }
            } catch (RestClientException ex) {
                log.warn("Semantic Scholar title request failed (attempt {}): {}", attempt, ex.getMessage());
            }
        }
        return null;
    }

    private MetadataEnrichResponse fetchFromCrossrefByQuery(String query, String expectedTitle) {
        String url = "https://api.crossref.org/works";
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        UriComponentsBuilder.fromUriString(url)
                                .queryParam("query.title", query)
                                .queryParam("rows", 5)
                                .build()
                                .encode()
                                .toUri(),
                        HttpMethod.GET,
                        new HttpEntity<>(buildHeaders()),
                        String.class
                );
                MetadataEnrichResponse parsed = parseCrossrefSearch(response.getBody(), expectedTitle);
                if (parsed != null) {
                    return enrichWithVenue(parsed, "crossref");
                }
            } catch (RestClientException ex) {
                log.warn("CrossRef search request failed (attempt {}): {}", attempt, ex.getMessage());
            }
        }
        return null;
    }

    private MetadataEnrichResponse fetchFromSemanticByArxivId(String arxivId) {
        if (!StringUtils.hasText(arxivId)) {
            return null;
        }
        String url = "https://api.semanticscholar.org/graph/v1/paper/ARXIV:{arxivId}?fields=title,authors,year,venue,abstract,citationCount,externalIds,url";
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                        UriComponentsBuilder.fromUriString(url).buildAndExpand(arxivId).encode().toUri(),
                        HttpMethod.GET,
                        new HttpEntity<>(buildHeaders()),
                        String.class
                );
                MetadataEnrichResponse parsed = parseSemanticPaper(response.getBody());
                if (parsed != null) {
                    return enrichWithVenue(parsed, "semantic_scholar");
                }
            } catch (RestClientException ex) {
                log.warn("Semantic Scholar arXiv request failed (attempt {}): {}", attempt, ex.getMessage());
            }
        }
        return null;
    }

    private MetadataEnrichResponse parseCrossref(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode message = root.get("message");
            return parseCrossrefMessage(message);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse CrossRef response", ex);
            return null;
        }
    }

    private MetadataEnrichResponse parseCrossrefSearch(String body, String expectedTitle) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("message").path("items");
            if (!items.isArray() || items.isEmpty()) {
                return null;
            }
            MetadataEnrichResponse best = null;
            double bestScore = 0.0;
            for (JsonNode item : items) {
                MetadataEnrichResponse candidate = parseCrossrefMessage(item);
                if (candidate == null) {
                    continue;
                }
                double score = titleSimilarityScore(expectedTitle, candidate.getTitle());
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }
            return bestScore >= 0.8 ? best : null;
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse CrossRef search response", ex);
            return null;
        }
    }

    private MetadataEnrichResponse parseCrossrefMessage(JsonNode message) {
        if (message == null || message.isMissingNode()) {
            return null;
        }
        String title = firstArrayValue(message.get("title"));
        List<String> authors = parseCrossrefAuthors(message.get("author"));
        Integer year = parseYearFromIssued(message.get("issued"));
        String venue = firstArrayValue(message.get("container-title"));
        String abstractText = message.path("abstract").asText(null);
        String doi = message.path("DOI").asText(null);
        String url = message.path("URL").asText(null);
        Integer citationCount = message.has("is-referenced-by-count")
                ? message.get("is-referenced-by-count").asInt()
                : null;
        return MetadataEnrichResponse.builder()
                .title(title)
                .authors(authors)
                .year(year)
                .venue(venue)
                .abstractText(abstractText)
                .doi(doi)
                .paperUrl(url)
                .citationCount(citationCount)
                .build();
    }

    private MetadataEnrichResponse parseSemanticSearch(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (!data.isArray() || data.isEmpty()) {
                return null;
            }
            return parseSemanticPaperNode(data.get(0));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse Semantic Scholar search response", ex);
            return null;
        }
    }

    private MetadataEnrichResponse parseSemanticPaper(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return parseSemanticPaperNode(root);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to parse Semantic Scholar response", ex);
            return null;
        }
    }

    private MetadataEnrichResponse parseSemanticPaperNode(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        String title = node.path("title").asText(null);
        List<String> authors = parseSemanticAuthors(node.path("authors"));
        Integer year = node.hasNonNull("year") ? node.get("year").asInt() : null;
        String venue = node.path("venue").asText(null);
        String abstractText = node.path("abstract").asText(null);
        Integer citationCount = node.hasNonNull("citationCount") ? node.get("citationCount").asInt() : null;
        String url = node.path("url").asText(null);
        String doi = null;
        JsonNode externalIds = node.path("externalIds");
        if (externalIds.isObject()) {
            doi = externalIds.path("DOI").asText(null);
        }
        return MetadataEnrichResponse.builder()
                .title(title)
                .authors(authors)
                .year(year)
                .venue(venue)
                .abstractText(abstractText)
                .doi(doi)
                .paperUrl(url)
                .citationCount(citationCount)
                .build();
    }

    private MetadataEnrichResponse enrichWithVenue(MetadataEnrichResponse response, String source) {
        MetadataEnrichResponse.MetadataEnrichResponseBuilder builder = MetadataEnrichResponse.builder()
                .title(response.getTitle())
                .authors(response.getAuthors())
                .year(response.getYear())
                .venue(response.getVenue())
                .doi(response.getDoi())
                .abstractText(response.getAbstractText())
                .paperUrl(response.getPaperUrl())
                .citationCount(response.getCitationCount())
                .source(source);

        if (StringUtils.hasText(response.getVenue())) {
            VenueRankingResponse ranking = venueRankingService.lookup(response.getVenue());
            if (ranking != null) {
                builder.ccfRank(ranking.getCcfRank())
                        .jcrQuartile(ranking.getJcrQuartile());
            }
        }
        return builder.build();
    }

    private MetadataEnrichResponse emptyResponse(String source) {
        return MetadataEnrichResponse.builder().source(source).build();
    }

    private boolean hasSubstantialMetadata(MetadataEnrichResponse response) {
        if (response == null) {
            return false;
        }
        return (response.getAuthors() != null && !response.getAuthors().isEmpty())
                || response.getYear() != null
                || StringUtils.hasText(response.getVenue())
                || StringUtils.hasText(response.getDoi())
                || StringUtils.hasText(response.getAbstractText())
                || StringUtils.hasText(response.getCcfRank())
                || StringUtils.hasText(response.getJcrQuartile());
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        headers.set(HttpHeaders.ACCEPT, "application/json");
        if (StringUtils.hasText(semanticScholarApiKey)) {
            headers.set("x-api-key", semanticScholarApiKey);
        }
        return headers;
    }

    private List<String> parseCrossrefAuthors(JsonNode authorsNode) {
        if (authorsNode == null || !authorsNode.isArray()) {
            return List.of();
        }
        List<String> authors = new ArrayList<>();
        for (JsonNode author : authorsNode) {
            String given = author.path("given").asText("").trim();
            String family = author.path("family").asText("").trim();
            String name = (given + " " + family).trim();
            if (!StringUtils.hasText(name)) {
                name = family;
            }
            if (StringUtils.hasText(name)) {
                authors.add(name);
            }
        }
        return authors;
    }

    private List<String> parseSemanticAuthors(JsonNode authorsNode) {
        if (authorsNode == null || !authorsNode.isArray()) {
            return List.of();
        }
        List<String> authors = new ArrayList<>();
        for (JsonNode author : authorsNode) {
            String name = author.path("name").asText(null);
            if (StringUtils.hasText(name)) {
                authors.add(name);
            }
        }
        return authors;
    }

    private String firstArrayValue(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return null;
        }
        return node.get(0).asText(null);
    }

    private Integer parseYearFromIssued(JsonNode issued) {
        if (issued == null || issued.isMissingNode()) {
            return null;
        }
        JsonNode dateParts = issued.path("date-parts");
        if (!dateParts.isArray() || dateParts.isEmpty()) {
            return null;
        }
        JsonNode first = dateParts.get(0);
        if (first == null || !first.isArray() || first.isEmpty()) {
            return null;
        }
        JsonNode yearNode = first.get(0);
        if (yearNode == null || !yearNode.canConvertToInt()) {
            return null;
        }
        int year = yearNode.asInt();
        return year > 0 ? year : null;
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

    private String normalizeDoi(String doi) {
        if (!StringUtils.hasText(doi)) {
            return null;
        }
        String normalized = doi.trim();
        normalized = normalized.replaceFirst("(?i)^https?://(dx\\.)?doi\\.org/", "");
        normalized = normalized.replaceFirst("(?i)^doi:\\s*", "");
        normalized = normalized.replaceAll("\\s+", "");
        return normalized;
    }

    private boolean isArxivDoi(String doi) {
        if (!StringUtils.hasText(doi)) {
            return false;
        }
        return doi.toLowerCase(Locale.ROOT).startsWith("10.48550/arxiv.");
    }

    private String toArxivId(String doi) {
        if (!isArxivDoi(doi)) {
            return null;
        }
        return doi.replaceFirst("(?i)^10\\.48550/arxiv\\.", "");
    }

    private boolean isLikelySameTitle(String expectedTitle, String actualTitle) {
        return titleSimilarityScore(expectedTitle, actualTitle) >= 0.8;
    }

    private String normalizeTitle(String title) {
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeFileTitle(String fallbackTitle) {
        if (!StringUtils.hasText(fallbackTitle)) {
            return null;
        }
        return fallbackTitle
                .replaceAll("(?i)\\.pdf$", "")
                .replace('_', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double titleSimilarityScore(String expectedTitle, String actualTitle) {
        if (!StringUtils.hasText(expectedTitle) || !StringUtils.hasText(actualTitle)) {
            return 0.0;
        }
        String expected = normalizeTitle(expectedTitle);
        String actual = normalizeTitle(actualTitle);
        if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual)) {
            return 0.0;
        }
        if (expected.equals(actual)) {
            return 1.0;
        }
        if (expected.contains(actual) || actual.contains(expected)) {
            return 0.9;
        }

        Set<String> expectedTokens = new HashSet<>(Arrays.asList(expected.split(" ")));
        Set<String> actualTokens = new HashSet<>(Arrays.asList(actual.split(" ")));
        expectedTokens.removeIf(token -> token.length() < 2);
        actualTokens.removeIf(token -> token.length() < 2);
        if (expectedTokens.isEmpty() || actualTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(expectedTokens);
        intersection.retainAll(actualTokens);
        if (intersection.isEmpty()) {
            return 0.0;
        }
        Set<String> union = new HashSet<>(expectedTokens);
        union.addAll(actualTokens);
        double jaccard = (double) intersection.size() / (double) union.size();
        double bigramDice = bigramDiceScore(expected, actual);
        return (jaccard * 0.4) + (bigramDice * 0.6);
    }

    private double bigramDiceScore(String a, String b) {
        List<String> aWords = Arrays.asList(a.split(" "));
        List<String> bWords = Arrays.asList(b.split(" "));
        if (aWords.size() < 2 || bWords.size() < 2) {
            return 0.0;
        }
        Set<String> aBigrams = new HashSet<>();
        for (int i = 0; i < aWords.size() - 1; i++) {
            aBigrams.add(aWords.get(i) + " " + aWords.get(i + 1));
        }
        Set<String> bBigrams = new HashSet<>();
        for (int i = 0; i < bWords.size() - 1; i++) {
            bBigrams.add(bWords.get(i) + " " + bWords.get(i + 1));
        }
        Set<String> overlap = new HashSet<>(aBigrams);
        overlap.retainAll(bBigrams);
        return (2.0 * overlap.size()) / (aBigrams.size() + bBigrams.size());
    }
}
