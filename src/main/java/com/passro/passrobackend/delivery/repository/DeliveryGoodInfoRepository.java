package com.passro.passrobackend.delivery.repository;

import com.passro.passrobackend.delivery.entity.DeliveryGoodInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.passro.passrobackend.delivery.entity.Delivery;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryGoodInfoRepository extends JpaRepository<DeliveryGoodInfo, Long> {
    List<DeliveryGoodInfo> findByDeliveryIn(List<Delivery> deliveries);

    Optional<DeliveryGoodInfo> findByDelivery(Delivery delivery);
}
