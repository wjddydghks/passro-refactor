package com.passro.passrobackend.deliveryinquiry.entity;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.deliveryinquiry.enums.DeliveryInquiryCategory;
import com.passro.passrobackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "delivery_inquiry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DeliveryInquiry extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 문의가 달린 배송 (ERD: delivery_id)
    @ManyToOne(fetch = FetchType.LAZY)
    private Delivery delivery;

    // 문의자 (ERD: account_id)
    @ManyToOne(fetch = FetchType.LAZY)
    private Account account;

    @Enumerated(EnumType.STRING)
    private DeliveryInquiryCategory category;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    // 첨부 이미지 S3 키 (사진 없으면 null)
    @Column(length = 512)
    private String imageKey;
}
