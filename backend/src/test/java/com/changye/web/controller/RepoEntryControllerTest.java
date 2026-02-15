package com.changye.web.controller;

import com.changye.web.service.PaperCodeEntryService;
import com.changye.web.service.RepoEntrySyncResult;
import com.changye.web.service.RepoEntrySyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RepoEntryController.class)
class RepoEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaperCodeEntryService paperCodeEntryService;

    @MockBean
    private RepoEntrySyncService repoEntrySyncService;

    @Test
    void syncEntriesReturnsPerEntryResult() throws Exception {
        RepoEntriesSyncRequest request = new RepoEntriesSyncRequest();
        request.setPaperId(9L);

        RepoEntrySyncResult result = RepoEntrySyncResult.builder()
                .entryId(100L)
                .repoUrl("https://github.com/foo/bar.git")
                .localPath("/srv/personal-web/repos/paper-code/bar")
                .action("CLONE")
                .message("clone 成功")
                .build();

        when(repoEntrySyncService.syncEntries(9L, null)).thenReturn(List.of(result));

        mockMvc.perform(post("/api/v1/repo/entries/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].entryId").value(100))
                .andExpect(jsonPath("$.data[0].action").value("CLONE"));
    }
}
