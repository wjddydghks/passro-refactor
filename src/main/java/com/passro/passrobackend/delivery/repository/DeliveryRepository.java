package com.passro.passrobackend.delivery.repository;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.delivery.enums.DeliveryState;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long>
{
    // 알고리즘 판단 및 매칭 대기 배송 목록 조회 메서드
    @Query("""
        SELECT d
        FROM Delivery d
        LEFT JOIN FETCH d.origin
        LEFT JOIN FETCH d.dest
        LEFT JOIN FETCH d.deliveryGoodInfo
        WHERE d.status = :status
    """)
    List<Delivery> findAllByStatus(@Param("status") DeliveryState status);

    // 배송기사별 배정된 배송 목록 조회 메서드

    long countByShipper(Account shipper);

    boolean existsByShipperAndStatus(Account shipper, DeliveryState status);

    @Query("""
        SELECT d
        FROM Delivery d
        LEFT JOIN FETCH d.origin
        LEFT JOIN FETCH d.dest
        LEFT JOIN FETCH d.deliveryGoodInfo
        WHERE d.shipper = :shipper
    """)
    List<Delivery> findAllByShipper(@Param("shipper") Account shipper);

    @Query("""
        SELECT d
        FROM Delivery d
        LEFT JOIN FETCH d.origin
        LEFT JOIN FETCH d.dest
        LEFT JOIN FETCH d.deliveryGoodInfo
        WHERE d.shipper = :shipper
          AND d.status = :status
    """)
    List<Delivery> findAllByShipperAndStatus(
            @Param("shipper") Account shipper,
            @Param("status") DeliveryState status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Delivery d WHERE d.id = :id")
    Optional<Delivery> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT d
        FROM Delivery d
        LEFT JOIN FETCH d.origin
        LEFT JOIN FETCH d.dest
        LEFT JOIN FETCH d.deliveryGoodInfo
        WHERE d.sender = :sender
    """)
    List<Delivery> findAllBySender(@Param("sender") Account sender);

    @Query("""
        SELECT d
        FROM Delivery d
        LEFT JOIN FETCH d.origin
        LEFT JOIN FETCH d.dest
        LEFT JOIN FETCH d.deliveryGoodInfo
        WHERE d.sender = :sender
          AND d.status = :status
    """)
    List<Delivery> findAllBySenderAndStatus(
            @Param("sender") Account sender,
            @Param("status") DeliveryState status);
    long countBySender(Account sender);
}
