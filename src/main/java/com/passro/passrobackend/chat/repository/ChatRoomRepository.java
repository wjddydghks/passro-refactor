package com.passro.passrobackend.chat.repository;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.chat.entity.ChatRoom;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByDeliveryId(Long deliveryId);

    @EntityGraph(attributePaths = {
            "delivery",
            "delivery.sender",
            "delivery.shipper",
            "delivery.deliveryGoodInfo"
    })
    @Query("""
            SELECT cr
            FROM ChatRoom cr
            WHERE ((cr.delivery.sender = :account AND cr.senderLeftAt IS NULL)
                OR (cr.delivery.shipper = :account AND cr.shipperLeftAt IS NULL))
              AND cr.delivery.status NOT IN :excludedStatuses
            """)
    List<ChatRoom> findAllActiveByAccount(
            @Param("account") Account account,
            @Param("excludedStatuses") Collection<DeliveryState> excludedStatuses);
}
