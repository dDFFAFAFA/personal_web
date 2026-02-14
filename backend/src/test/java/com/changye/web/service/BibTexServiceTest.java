package com.changye.web.service;

import com.changye.web.dto.request.PaperCreateRequest;
import com.changye.web.model.Paper;
import com.changye.web.repository.PaperRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BibTexServiceTest {

    @Mock
    private PaperRepository paperRepository;

    private BibTexService bibTexService;

    @BeforeEach
    void setUp() {
        bibTexService = new BibTexService(paperRepository, new ObjectMapper());
    }

    @Test
    void parseBibTeXReturnsRequests() {
        String bibtex = "@article{vaswani2017,\n"
                + "  title={Attention is all you need},\n"
                + "  author={Vaswani, Ashish and Shazeer, Noam},\n"
                + "  journal={NeurIPS},\n"
                + "  year={2017},\n"
                + "  doi={10.48550/arXiv.1706.03762}\n"
                + "}";

        List<PaperCreateRequest> requests = bibTexService.parseBibTeX(
                new ByteArrayInputStream(bibtex.getBytes(StandardCharsets.UTF_8))
        );

        assertThat(requests).hasSize(1);
        PaperCreateRequest request = requests.get(0);
        assertThat(request.getTitle()).isEqualTo("Attention is all you need");
        assertThat(request.getAuthors()).containsExactly("Vaswani, Ashish", "Shazeer, Noam");
        assertThat(request.getYear()).isEqualTo(2017);
        assertThat(request.getVenue()).isEqualTo("NeurIPS");
        assertThat(request.getDoi()).isEqualTo("10.48550/arXiv.1706.03762");
    }

    @Test
    void parseRisReturnsRequests() {
        String ris = "TY  - JOUR\n"
                + "TI  - Attention is all you need\n"
                + "AU  - Vaswani, Ashish\n"
                + "AU  - Shazeer, Noam\n"
                + "PY  - 2017\n"
                + "JO  - NeurIPS\n"
                + "DO  - 10.48550/arXiv.1706.03762\n"
                + "ER  - \n";

        List<PaperCreateRequest> requests = bibTexService.parseRis(
                new ByteArrayInputStream(ris.getBytes(StandardCharsets.UTF_8))
        );

        assertThat(requests).hasSize(1);
        PaperCreateRequest request = requests.get(0);
        assertThat(request.getTitle()).isEqualTo("Attention is all you need");
        assertThat(request.getAuthors()).containsExactly("Vaswani, Ashish", "Shazeer, Noam");
        assertThat(request.getYear()).isEqualTo(2017);
        assertThat(request.getVenue()).isEqualTo("NeurIPS");
        assertThat(request.getDoi()).isEqualTo("10.48550/arXiv.1706.03762");
    }

    @Test
    void exportBibTeXAndRis() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Paper paper = Paper.builder()
                .id(1L)
                .title("Attention is all you need")
                .authors(mapper.writeValueAsString(List.of("Vaswani, Ashish", "Shazeer, Noam")))
                .venue("NeurIPS")
                .year(2017)
                .doi("10.48550/arXiv.1706.03762")
                .build();

        when(paperRepository.findAllById(List.of(1L))).thenReturn(List.of(paper));

        String bibtex = bibTexService.exportBibTeX(List.of(1L));
        assertThat(bibtex).contains("@article{paper1");
        assertThat(bibtex).contains("title={Attention is all you need}");
        assertThat(bibtex).contains("author={Vaswani, Ashish and Shazeer, Noam}");

        String ris = bibTexService.exportRis(List.of(1L));
        assertThat(ris).contains("TI  - Attention is all you need");
        assertThat(ris).contains("AU  - Vaswani, Ashish");
        assertThat(ris).contains("DO  - 10.48550/arXiv.1706.03762");
    }
}
