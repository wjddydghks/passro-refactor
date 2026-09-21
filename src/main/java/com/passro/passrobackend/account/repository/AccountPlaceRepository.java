package com.passro.passrobackend.account.repository;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountPlaceRepository extends JpaRepository<AccountPlace, Long> {
    // 배송기사 동선 및 출발지/목적지 정보 조회 메서드
    Optional<AccountPlace> findByAccount(Account account);


}
