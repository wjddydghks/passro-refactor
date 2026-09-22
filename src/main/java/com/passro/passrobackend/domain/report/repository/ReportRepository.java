package com.passro.passrobackend.domain.report.repository;

import com.passro.passrobackend.domain.report.entity.Report;
import com.passro.passrobackend.domain.report.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporter_IdAndTargetTypeAndTargetId(
            Long reporterId,
            ReportTargetType targetType,
            Long targetId
    );

    @EntityGraph(attributePaths = {"reportedAccount", "delivery", "chatMessage", "images"})
    Page<Report> findAllByReporter_IdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);
}
