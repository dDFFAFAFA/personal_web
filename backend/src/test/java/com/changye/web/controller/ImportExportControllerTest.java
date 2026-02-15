package com.changye.web.controller;

import com.changye.web.service.BibTexService;
import com.changye.web.service.PaperService;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportExportController.class)
class ImportExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BibTexService bibTexService;

    @MockBean
    private PaperService paperService;

    @Test
    void exportWithoutIdsReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/papers/export")
                        .param("format", "bibtex"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("ids 不能为空"));
    }

    @Test
    void exportWithoutFormatReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/papers/export")
                        .param("ids", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("format 不能为空"));
    }

    @Test
    void exportWithIdsReturnsFile() throws Exception {
        when(bibTexService.exportBibTeX(List.of(1L)))
                .thenReturn("@article{paper1,\n  title={Test}\n}");

        mockMvc.perform(get("/api/v1/papers/export")
                        .param("format", "bibtex")
                        .param("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("papers_")))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/x-bibtex"));
    }
}
