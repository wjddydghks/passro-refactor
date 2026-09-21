package com.passro.passrobackend.report.entity;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.chat.entity.ChatMessage;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.global.entity.BaseEntity;
import com.passro.passrobackend.report.enums.ReportReason;
import com.passro.passrobackend.report.enums.ReportStatus;
import com.passro.passrobackend.report.enums.ReportTargetType;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(
        name = "report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_report_reporter_target",
                        columnNames = {"reporter_id", "target_type", "target_id"}
                )
        }
)
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 신고자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Account reporter;

    // 신고 대상 사용자 (채팅 신고 / 사용자 신고에서 주로 사용)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_account_id")
    private Account reportedAccount;

    // 배송 신고 / 사용자 신고 권한 검증용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    // 채팅 신고용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_message_id")
    private ChatMessage chatMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    // 중복 신고 방지용 정규화 target id
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReportImage> images = new ArrayList<>();

    public void addImage(ReportImage image) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.add(image);
        image.assignReport(this);
    }

    public void updateStatus(ReportStatus status) {
        this.status = status;
    }
}
