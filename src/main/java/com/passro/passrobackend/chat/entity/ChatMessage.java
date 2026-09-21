package com.passro.passrobackend.chat.entity;

import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(indexes = {
        @Index(name = "idx_chat_message_delivery_id_id", columnList = "delivery_id, id"),
        @Index(name = "idx_chat_message_unread", columnList = "delivery_id, is_read, sender_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Account sender;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(length = 500)
    private String imageKey;

    @Builder.Default
    @Column(name = "system_message", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean systemMessage = false;

    @Column(nullable = false)
    private boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }
}
