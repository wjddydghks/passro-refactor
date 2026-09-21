package com.passro.passrobackend.delivery.event;

import com.passro.passrobackend.delivery.entity.DeliveryLog;
import com.passro.passrobackend.delivery.repository.DeliveryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryLogEventListener {

    private final DeliveryLogRepository deliveryLogRepository;

    // DeliveryLogEvent를 구독하여 DeliveryLog(배송 내역)를 생성하고 DB에 저장하는 동기 이벤트 리스너
    @EventListener
    public void createDeliveryLog(DeliveryLogEvent event) {
        DeliveryLog deliveryLog = DeliveryLog.builder()
                .delivery(event.getDelivery())
                .type(event.getType())
                .image(event.getImage())
                .build();
        deliveryLogRepository.save(deliveryLog);
    }

/**
 * 사용 예시
 *
 * @Autowired
 * private ApplicationEventPublisher eventPublisher;
 *
 * public void updateDeliveryStatus(Delivery delivery, DeliveryLogType type) {
 *     eventPublisher.publishEvent(new DeliveryLogEvent(delivery, type));
 * }
 */

}
