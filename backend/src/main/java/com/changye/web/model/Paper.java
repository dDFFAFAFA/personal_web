package com.changye.web.model;

import com.changye.web.model.enums.ReadingStatus;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "papers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String authors; // JSON array string, e.g. ["Author A", "Author B"]

    @Column(name = "publish_year")
    private Integer year;

    private String venue;

    private String doi;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_status", nullable = false)
    @Builder.Default
    private ReadingStatus readingStatus = ReadingStatus.UNREAD;

    @Builder.Default
    private Boolean starred = false;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    // --- Phase 2A: Metadata Enrichment ---

    @Column(name = "ccf_rank", length = 10)
    private String ccfRank; // "A", "B", "C"

    @Column(name = "jcr_quartile", length = 10)
    private String jcrQuartile; // "Q1", "Q2", "Q3", "Q4"

    @Column(name = "impact_factor", precision = 6, scale = 3)
    private BigDecimal impactFactor;

    @Column(name = "citation_count")
    @Builder.Default
    private Integer citationCount = 0;

    @Column(name = "paper_url", length = 500)
    private String paperUrl; // Original paper link (e.g. arXiv URL)

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "paper_tags", joinColumns = @JoinColumn(name = "paper_id", foreignKey = @ForeignKey(name = "fk_paper_tags_paper")), inverseJoinColumns = @JoinColumn(name = "tag_id", foreignKey = @ForeignKey(name = "fk_paper_tags_tag")))
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Note> notes = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
