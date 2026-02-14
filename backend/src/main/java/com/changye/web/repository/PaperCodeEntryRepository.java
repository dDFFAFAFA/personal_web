package com.changye.web.repository;

import com.changye.web.model.PaperCodeEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperCodeEntryRepository extends JpaRepository<PaperCodeEntry, Long> {
    List<PaperCodeEntry> findAllByOrderByUpdatedAtDesc();

    List<PaperCodeEntry> findByPaperIdOrderByUpdatedAtDesc(Long paperId);

    boolean existsByPaperIdAndRepoUrl(Long paperId, String repoUrl);
}
