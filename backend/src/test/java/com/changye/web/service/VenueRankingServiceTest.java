package com.changye.web.service;

import com.changye.web.dto.response.VenueRankingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

class VenueRankingServiceTest {

    private VenueRankingService venueRankingService;

    @BeforeEach
    void setUp() {
        venueRankingService = new VenueRankingService(new ObjectMapper(), new DefaultResourceLoader());
        venueRankingService.loadData();
    }

    @Test
    void lookupExactMatchByName() {
        VenueRankingResponse response = venueRankingService.lookup("NeurIPS");
        assertThat(response).isNotNull();
        assertThat(response.getCcfRank()).isEqualTo("A");
        assertThat(response.getType()).isEqualTo("conference");
    }

    @Test
    void lookupFuzzyMatchByFullName() {
        VenueRankingResponse response = venueRankingService.lookup("Neural Information Processing Systems");
        assertThat(response).isNotNull();
        assertThat(response.getVenue()).isEqualTo("NeurIPS");
    }
}
