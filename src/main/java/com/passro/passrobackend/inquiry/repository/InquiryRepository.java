package com.passro.passrobackend.inquiry.repository;

import com.passro.passrobackend.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    // 조회 API는 프론트 스펙 확정 후 별도 이슈에서 추가
}
