package com.changye.web.model;

import com.changye.web.model.enums.AiProvider;
import com.changye.web.model.enums.PaperSummaryStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "paper_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_paper_summary_paper"))
    private Paper paper;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaperSummaryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20)
    private AiProvider provider;

    @Column(name = "model_name", length = 120)
    private String modelName;

    @Column(name = "markdown_content", columnDefinition = "TEXT")
    private String markdownContent;

    @Column(name = "one_sentence", length = 600)
    private String oneSentence;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
