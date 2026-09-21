package com.passro.passrobackend.delivery.repository;

import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryLogRepository extends JpaRepository<DeliveryLog, Long> {
    List<DeliveryLog> findAllByDeliveryOrderByCreatedAtAsc(Delivery delivery);
}
