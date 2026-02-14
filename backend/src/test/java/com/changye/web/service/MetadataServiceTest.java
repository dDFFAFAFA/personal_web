package com.changye.web.service;

import com.changye.web.dto.response.MetadataEnrichResponse;
import com.changye.web.repository.PaperRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;

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

    private MetadataService metadataService;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.setConnectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        metadataService = new MetadataService(restTemplateBuilder, new ObjectMapper(), venueRankingService, paperRepository);
    }

    @Test
    void enrichByTitleEncodesSpacesInQueryParam() {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenAnswer(invocation -> {
                    URI uri = invocation.getArgument(0);
                    assertThat(uri.toString()).contains("query=Test%20Paper");
                    throw new RestClientException("network unavailable");
                });

        MetadataEnrichResponse response = metadataService.enrichByTitle("Test Paper");

        assertThat(response).isNotNull();
        assertThat(response.getSource()).isEqualTo("none");
    }
}
