package com.passro.passrobackend.delivery.repository;

import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.entity.DeliveryPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryPointRepository extends JpaRepository<DeliveryPoint, Long> {
    Optional<DeliveryPoint> findByDelivery(Delivery delivery);
}
