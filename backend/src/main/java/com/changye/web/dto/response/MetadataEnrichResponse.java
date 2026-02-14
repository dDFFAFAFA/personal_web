package com.changye.web.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * Response for metadata enrichment via CrossRef / Semantic Scholar APIs.
 * Used as a "preview" before the user confirms the import.
 */
@Data
@Builder
public class MetadataEnrichResponse {

    private String title;
    private List<String> authors;
    private Integer year;
    private String venue;
    private String doi;
    private String abstractText;
    private String paperUrl; // Link to the original paper
    private Integer citationCount;

    // Venue ranking (auto-looked up)
    private String ccfRank;
    private String jcrQuartile;

    private String source; // "crossref" or "semantic_scholar"
}
