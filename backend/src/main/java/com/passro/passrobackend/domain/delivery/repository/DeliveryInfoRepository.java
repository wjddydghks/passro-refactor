package com.passro.passrobackend.domain.delivery.repository;

import com.passro.passrobackend.domain.delivery.entity.DeliveryInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryInfoRepository extends JpaRepository<DeliveryInfo, Long>
{
}
