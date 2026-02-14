package com.changye.web.service;

import com.changye.web.dto.response.VenueRankingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class VenueRankingService {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final List<VenueEntry> entries = new ArrayList<>();
    private final Map<String, VenueEntry> nameIndex = new HashMap<>();
    private final Map<String, VenueEntry> fullNameIndex = new HashMap<>();

    public VenueRankingService(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    void loadData() {
        Resource resource = resourceLoader.getResource("classpath:data/ccf_venues.json");
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            loadEntries(root.get("conferences"), "conference");
            loadEntries(root.get("journals"), "journal");
            log.info("Loaded {} CCF venues", entries.size());
        } catch (IOException ex) {
            log.error("Failed to load CCF venue data", ex);
        }
    }

    public VenueRankingResponse lookup(String venue) {
        if (!StringUtils.hasText(venue)) {
            return null;
        }
        String normalized = normalize(venue);
        VenueEntry entry = nameIndex.get(normalized);
        if (entry == null) {
            entry = fullNameIndex.get(normalized);
        }
        if (entry == null) {
            entry = fuzzyMatch(normalized);
        }
        return entry == null ? null : toResponse(entry);
    }

    private void loadEntries(JsonNode node, String type) {
        if (node == null || !node.isArray()) {
            return;
        }
        for (JsonNode item : node) {
            String name = safeText(item, "name");
            String fullName = safeText(item, "fullName");
            String rank = safeText(item, "rank");
            String category = safeText(item, "category");
            VenueEntry entry = new VenueEntry(name, fullName, rank, category, type);
            entries.add(entry);
            if (StringUtils.hasText(name)) {
                nameIndex.put(normalize(name), entry);
            }
            if (StringUtils.hasText(fullName)) {
                fullNameIndex.put(normalize(fullName), entry);
            }
        }
    }

    private VenueEntry fuzzyMatch(String normalizedVenue) {
        for (VenueEntry entry : entries) {
            if (containsMatch(normalizedVenue, entry)) {
                return entry;
            }
        }
        return null;
    }

    private boolean containsMatch(String normalizedVenue, VenueEntry entry) {
        String nameKey = entry.nameKey;
        String fullNameKey = entry.fullNameKey;
        return (StringUtils.hasText(nameKey) && (normalizedVenue.contains(nameKey) || nameKey.contains(normalizedVenue)))
                || (StringUtils.hasText(fullNameKey) && (normalizedVenue.contains(fullNameKey) || fullNameKey.contains(normalizedVenue)));
    }

    private VenueRankingResponse toResponse(VenueEntry entry) {
        return VenueRankingResponse.builder()
                .venue(entry.name)
                .ccfRank(entry.rank)
                .jcrQuartile(null)
                .impactFactor(null)
                .category(entry.category)
                .type(entry.type)
                .build();
    }

    private String safeText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null ? null : value.asText(null);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private static final class VenueEntry {
        private final String name;
        private final String fullName;
        private final String rank;
        private final String category;
        private final String type;
        private final String nameKey;
        private final String fullNameKey;

        private VenueEntry(String name, String fullName, String rank, String category, String type) {
            this.name = name;
            this.fullName = fullName;
            this.rank = rank;
            this.category = category;
            this.type = type;
            this.nameKey = normalizeKey(name);
            this.fullNameKey = normalizeKey(fullName);
        }

        private String normalizeKey(String value) {
            if (!StringUtils.hasText(value)) {
                return "";
            }
            return value.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]", "");
        }
    }
}
