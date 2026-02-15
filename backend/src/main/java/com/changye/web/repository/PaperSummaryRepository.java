package com.changye.web.repository;

import com.changye.web.model.PaperSummary;
import com.changye.web.model.enums.PaperSummaryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperSummaryRepository extends JpaRepository<PaperSummary, Long> {
    Optional<PaperSummary> findByPaperId(Long paperId);

    @EntityGraph(attributePaths = "paper")
    List<PaperSummary> findByStatusOrderByGeneratedAtDesc(PaperSummaryStatus status, Pageable pageable);
}
