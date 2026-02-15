package com.changye.web.model;

import com.changye.web.model.enums.PaperRepoLinkProvider;
import com.changye.web.model.enums.PaperRepoLinkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "paper_repo_links", uniqueConstraints = {
        @UniqueConstraint(name = "uk_paper_repo_links_paper_url", columnNames = {"paper_id", "url"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperRepoLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false, foreignKey = @ForeignKey(name = "fk_paper_repo_link_paper"))
    private Paper paper;

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private PaperRepoLinkProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaperRepoLinkStatus status;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "page_no")
    private Integer pageNo;

    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
