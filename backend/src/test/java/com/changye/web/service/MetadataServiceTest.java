package com.changye.web.service;

import com.changye.web.dto.response.MetadataEnrichResponse;
import com.changye.web.dto.response.VenueRankingResponse;
import com.changye.web.model.Paper;
import com.changye.web.repository.PaperRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private VenueRankingService venueRankingService;

    @Mock
    private PaperRepository paperRepository;

    @Mock
    private PdfMetadataExtractorService pdfMetadataExtractorService;

    private MetadataService metadataService;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.setConnectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        metadataService = new MetadataService(
                restTemplateBuilder,
                new ObjectMapper(),
                venueRankingService,
                paperRepository,
                pdfMetadataExtractorService
        );
    }

    @Test
    void enrichByTitleEncodesSpacesInQueryParam() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    URI uri = invocation.getArgument(0);
                    String raw = uri.toString();
                    assertThat(raw.contains("query=Test%20Paper") || raw.contains("query.title=Test%20Paper"))
                            .isTrue();
                    throw new RestClientException("network unavailable");
                });

        MetadataEnrichResponse response = metadataService.enrichByTitle("Test Paper");

        assertThat(response).isNotNull();
        assertThat(response.getSource()).isEqualTo("none");
    }

    @Test
    void applyEnrichmentPersistsCcfAndJcrFromResponse() {
        Paper paper = Paper.builder()
                .id(11L)
                .title("Before")
                .venue("NeurIPS")
                .build();
        MetadataEnrichResponse response = MetadataEnrichResponse.builder()
                .ccfRank("A")
                .jcrQuartile("Q1")
                .build();

        when(paperRepository.findById(11L)).thenReturn(Optional.of(paper));
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        metadataService.applyEnrichment(11L, response);

        assertThat(paper.getCcfRank()).isEqualTo("A");
        assertThat(paper.getJcrQuartile()).isEqualTo("Q1");
        verify(paperRepository).save(paper);
    }

    @Test
    void applyEnrichmentFillsMissingRanksFromVenueLookup() {
        Paper paper = Paper.builder()
                .id(12L)
                .title("Before")
                .venue("ICCV")
                .build();
        MetadataEnrichResponse response = MetadataEnrichResponse.builder()
                .ccfRank(null)
                .jcrQuartile(null)
                .build();
        VenueRankingResponse ranking = VenueRankingResponse.builder()
                .venue("ICCV")
                .ccfRank("A")
                .jcrQuartile("Q1")
                .impactFactor(new BigDecimal("12.3"))
                .build();

        when(paperRepository.findById(12L)).thenReturn(Optional.of(paper));
        when(venueRankingService.lookup("ICCV")).thenReturn(ranking);
        when(paperRepository.save(any(Paper.class))).thenAnswer(invocation -> invocation.getArgument(0));

        metadataService.applyEnrichment(12L, response);

        assertThat(paper.getCcfRank()).isEqualTo("A");
        assertThat(paper.getJcrQuartile()).isEqualTo("Q1");
        assertThat(paper.getImpactFactor()).isEqualByComparingTo("12.3");
        verify(paperRepository).save(paper);
    }
}
