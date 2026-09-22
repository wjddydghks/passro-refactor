package com.passro.passrobackend.domain.delivery.repository;

import com.passro.passrobackend.domain.delivery.entity.Delivery;
import com.passro.passrobackend.domain.delivery.entity.DeliveryPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryPointRepository extends JpaRepository<DeliveryPoint, Long> {
    Optional<DeliveryPoint> findByDelivery(Delivery delivery);
}
