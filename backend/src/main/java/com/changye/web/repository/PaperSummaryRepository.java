package com.changye.web.repository;

import com.changye.web.model.PaperSummary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperSummaryRepository extends JpaRepository<PaperSummary, Long> {
    Optional<PaperSummary> findByPaperId(Long paperId);
}
