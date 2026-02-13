package com.changye.web.model;

import com.changye.web.model.enums.ReadingStatus;
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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "paper_tags",
            joinColumns = @JoinColumn(name = "paper_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
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
