package com.passro.passrobackend.account.repository;

import com.passro.passrobackend.account.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UniversityRepository extends JpaRepository<University, Long> {
}
