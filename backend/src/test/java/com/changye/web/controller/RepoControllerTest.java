package com.changye.web.controller;

import com.changye.web.dto.request.RepoConfigUpdateRequest;
import com.changye.web.dto.response.RepoConfigResponse;
import com.changye.web.dto.response.RepoSyncStatusResponse;
import com.changye.web.model.enums.RepoProvider;
import com.changye.web.model.enums.RepoSyncMode;
import com.changye.web.model.enums.RepoSyncStatus;
import com.changye.web.service.RepoSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RepoController.class)
class RepoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RepoSyncService repoSyncService;

    @Test
    void getConfigReturnsSuccess() throws Exception {
        RepoConfigResponse response = RepoConfigResponse.builder()
                .provider(RepoProvider.GITHUB)
                .repoUrl("git@github.com:foo/bar.git")
                .branch("main")
                .targetDir("paper-repo")
                .sshKeyPath("***/id_ed25519")
                .configured(true)
                .build();
        when(repoSyncService.getConfig()).thenReturn(response);

        mockMvc.perform(get("/api/v1/repo/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.provider").value("GITHUB"));
    }

    @Test
    void updateConfigReturnsSuccess() throws Exception {
        RepoConfigUpdateRequest request = new RepoConfigUpdateRequest();
        request.setProvider(RepoProvider.GITEE);
        request.setRepoUrl("git@gitee.com:foo/bar.git");
        request.setBranch("master");
        request.setTargetDir("bar-repo");

        RepoConfigResponse response = RepoConfigResponse.builder()
                .provider(RepoProvider.GITEE)
                .repoUrl("git@gitee.com:foo/bar.git")
                .branch("master")
                .targetDir("bar-repo")
                .sshKeyPath("***/id_ed25519")
                .configured(true)
                .build();
        when(repoSyncService.updateConfig(any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/repo/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.provider").value("GITEE"));
    }

    @Test
    void syncReturnsStatus() throws Exception {
        RepoSyncStatusResponse response = RepoSyncStatusResponse.builder()
                .status(RepoSyncStatus.SUCCESS)
                .mode(RepoSyncMode.CLONE)
                .message("同步成功")
                .syncedAt(OffsetDateTime.now())
                .build();
        when(repoSyncService.sync()).thenReturn(response);

        mockMvc.perform(post("/api/v1/repo/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
    }
}
