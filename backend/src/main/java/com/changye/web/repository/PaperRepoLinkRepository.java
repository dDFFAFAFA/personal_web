package com.changye.web.repository;

import com.changye.web.model.PaperRepoLink;
import com.changye.web.model.enums.PaperRepoLinkStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperRepoLinkRepository extends JpaRepository<PaperRepoLink, Long> {
    Optional<PaperRepoLink> findByPaperIdAndUrl(Long paperId, String url);

    Optional<PaperRepoLink> findByPaperIdAndId(Long paperId, Long id);

    List<PaperRepoLink> findByPaperIdOrderByUpdatedAtDesc(Long paperId);

    List<PaperRepoLink> findByPaperIdAndStatusOrderByUpdatedAtDesc(Long paperId, PaperRepoLinkStatus status);

    List<PaperRepoLink> findByPaperIdAndIdIn(Long paperId, Collection<Long> ids);
}
