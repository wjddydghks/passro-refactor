package com.passro.passrobackend.inquiry.entity;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.global.entity.BaseEntity;
import com.passro.passrobackend.inquiry.enums.InquiryCategory;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "inquiry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Inquiry extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryCategory category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 첨부 이미지 S3 키 (사진 없으면 null)
    @Column(length = 512)
    private String imageKey;
}
