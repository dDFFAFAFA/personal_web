package com.changye.web.controller;

import com.changye.web.common.exception.BusinessException;
import com.changye.web.dto.response.MetadataEnrichResponse;
import com.changye.web.dto.response.PaperResponse;
import com.changye.web.model.enums.ReadingStatus;
import com.changye.web.service.MetadataService;
import com.changye.web.service.PaperService;
import com.changye.web.service.VenueRankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MetadataController.class)
class MetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataService metadataService;

    @MockBean
    private VenueRankingService venueRankingService;

    @MockBean
    private PaperService paperService;

    @Test
    void enrichExistingReturnsPaperResponseWithRanks() throws Exception {
        PaperResponse paper = PaperResponse.builder()
                .id(9L)
                .title("Ranked Paper")
                .readingStatus(ReadingStatus.UNREAD)
                .ccfRank("A")
                .jcrQuartile("Q1")
                .build();
        when(metadataService.enrichAndApply(9L)).thenReturn(MetadataEnrichResponse.builder().build());
        when(paperService.getPaper(9L)).thenReturn(paper);

        mockMvc.perform(post("/api/v1/papers/9/enrich"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(9))
                .andExpect(jsonPath("$.data.ccfRank").value("A"))
                .andExpect(jsonPath("$.data.jcrQuartile").value("Q1"));
    }

    @Test
    void enrichExistingReturnsNotFoundWhenPaperMissing() throws Exception {
        when(metadataService.enrichAndApply(99L)).thenThrow(new BusinessException(404, "论文不存在"));

        mockMvc.perform(post("/api/v1/papers/99/enrich"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("论文不存在"));
    }
}
