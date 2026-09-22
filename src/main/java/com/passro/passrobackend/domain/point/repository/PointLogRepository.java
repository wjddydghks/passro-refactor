package com.passro.passrobackend.domain.point.repository;

import com.passro.passrobackend.domain.account.entity.Account;
import com.passro.passrobackend.domain.delivery.entity.Delivery;
import com.passro.passrobackend.domain.point.entity.PointLog;
import com.passro.passrobackend.domain.point.enums.PointIncrementReason;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointLogRepository extends JpaRepository<PointLog, Long> {

    boolean existsByAccountAndDeliveryAndIncrementReason(
            Account account,
            Delivery delivery,
            PointIncrementReason incrementReason
    );

    boolean existsByDeliveryAndIncrementReason(
            Delivery delivery,
            PointIncrementReason incrementReason
    );

    @EntityGraph(attributePaths = {
            "delivery",
            "delivery.origin",
            "delivery.dest",
            "delivery.deliveryGoodInfo",
            "market"
    })
    List<PointLog> findAllByAccountOrderByCreatedAtDesc(Account account);
}
