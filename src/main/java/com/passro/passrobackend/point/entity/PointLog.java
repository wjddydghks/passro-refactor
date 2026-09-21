package com.passro.passrobackend.point.entity;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.global.entity.BaseEntity;
import com.passro.passrobackend.market.entity.Market;
import com.passro.passrobackend.point.enums.PointIncrementReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "point_log",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_point_log_account_delivery_reason",
                columnNames = {"account_id", "delivery_id", "increment_reason"}
        )
)
public class PointLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "delivery_id", nullable = true)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "market_id")
    private Market market;

    @Enumerated(EnumType.STRING)
    @Column(name = "increment_reason", nullable = false, length = 40)
    private PointIncrementReason incrementReason;

    @Column(name = "delta_point", nullable = false)
    private Long deltaPoint;

    @Column(name = "before_point", nullable = false)
    private Long beforePoint;

    @Column(name = "after_point", nullable = false)
    private Long afterPoint;

    @Column(name = "increment_reason_memo", length = 500)
    private String incrementReasonMemo;

    public static PointLog create(
            Account account,
            Delivery delivery,
            PointIncrementReason reason,
            long deltaPoint,
            long beforePoint,
            long afterPoint,
            String memo
    ) {
        return PointLog.builder()
                .account(account)
                .delivery(delivery)
                .incrementReason(reason)
                .deltaPoint(deltaPoint)
                .beforePoint(beforePoint)
                .afterPoint(afterPoint)
                .incrementReasonMemo(memo)
                .build();
    }

    public static PointLog createMarketPurchase(
            Account account,
            Market market,
            long deltaPoint,
            long beforePoint,
            long afterPoint
    ) {
        return PointLog.builder()
                .account(account)
                .market(market)
                .incrementReason(PointIncrementReason.MARKET_PURCHASE)
                .deltaPoint(deltaPoint)
                .beforePoint(beforePoint)
                .afterPoint(afterPoint)
                .incrementReasonMemo("마켓 상품 구매: " + market.getName())
                .build();
    }
}
