package com.passro.passrobackend.deliveryinquiry.repository;

import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.deliveryinquiry.entity.DeliveryInquiry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryInquiryRepository extends JpaRepository<DeliveryInquiry, Long> {

    // 특정 배송에 달린 문의 목록 (최신순)
    List<DeliveryInquiry> findAllByDeliveryOrderByCreatedAtDesc(Delivery delivery);
}
