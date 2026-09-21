package com.passro.passrobackend.account.repository;

import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.account.entity.WayPoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WayPointRepository extends JpaRepository<WayPoint, Long> {
    // 배송기사의 방문 순서별 경유역 목록 조회 메서드
    List<WayPoint> findAllByAccountPlaceOrderByVisitOrderAsc(AccountPlace accountPlace);

    void deleteAllByAccountPlace(AccountPlace accountPlace);
}
