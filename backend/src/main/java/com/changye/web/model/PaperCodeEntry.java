package com.changye.web.model;

import com.changye.web.model.enums.RepoProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "paper_code_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperCodeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false, foreignKey = @ForeignKey(name = "fk_code_entry_paper"))
    private Paper paper;

    @Column(name = "repo_url", nullable = false, length = 500)
    private String repoUrl;

    @Column(name = "branch_name", length = 100)
    private String branchName;

    @Column(name = "provider", length = 20)
    private RepoProvider provider;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
