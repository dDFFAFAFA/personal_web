package com.changye.web.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VenueRankingResponse {

    private String venue; // Matched venue name
    private String ccfRank; // "A", "B", "C", or null
    private String jcrQuartile; // "Q1", "Q2", "Q3", "Q4", or null
    private BigDecimal impactFactor; // Journal IF, or null
    private String category; // e.g. "AI", "网络安全", "计算机体系结构"
    private String type; // "conference" or "journal"
}
