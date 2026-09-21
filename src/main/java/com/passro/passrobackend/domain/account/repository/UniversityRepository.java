package com.passro.passrobackend.domain.account.repository;

import com.passro.passrobackend.domain.account.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, Long> {
}
