package com.changye.web.repository;

import com.changye.web.model.Paper;
import com.changye.web.model.enums.ReadingStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaperRepository extends JpaRepository<Paper, Long>, JpaSpecificationExecutor<Paper> {

    Page<Paper> findByReadingStatus(ReadingStatus status, Pageable pageable);

    Page<Paper> findByStarred(Boolean starred, Pageable pageable);

    @Query("select p from Paper p where lower(p.title) like lower(concat('%', :keyword, '%')) "
            + "or lower(p.authors) like lower(concat('%', :keyword, '%')) "
            + "or lower(p.abstractText) like lower(concat('%', :keyword, '%'))")
    Page<Paper> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("select p from Paper p join p.tags t where t.id = :tagId")
    Page<Paper> findByTagId(@Param("tagId") Long tagId, Pageable pageable);

    long countByTags_Id(Long tagId);
}
