package com.changye.web.controller;

import com.changye.web.model.enums.PaperRepoLinkProvider;
import com.changye.web.model.enums.PaperRepoLinkStatus;
import com.changye.web.dto.response.PaperRepoLinkResponse;
import com.changye.web.service.PaperRepoLinkService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaperRepoLinkController.class)
class PaperRepoLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaperRepoLinkService paperRepoLinkService;

    @Test
    void rejectCandidateReturnsRejectedStatus() throws Exception {
        PaperRepoLinkResponse response = PaperRepoLinkResponse.builder()
                .id(11L)
                .paperId(1L)
                .url("https://github.com/foo/bar")
                .provider(PaperRepoLinkProvider.GITHUB)
                .status(PaperRepoLinkStatus.REJECTED)
                .confidence(BigDecimal.valueOf(0.9))
                .updatedAt(OffsetDateTime.now())
                .build();
        when(paperRepoLinkService.rejectLink(1L, 11L)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/papers/1/repo-links/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}
