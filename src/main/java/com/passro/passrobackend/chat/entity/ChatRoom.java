package com.passro.passrobackend.chat.entity;

import com.passro.passrobackend.delivery.entity.Delivery;
import com.passro.passrobackend.global.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_room",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_room_delivery",
                columnNames = "delivery_id"))
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false, unique = true)
    private Delivery delivery;

    private LocalDateTime senderLeftAt;

    private LocalDateTime shipperLeftAt;

    public boolean hasLeft(Long accountId) {
        if (isSender(accountId)) {
            return senderLeftAt != null;
        }
        if (isShipper(accountId)) {
            return shipperLeftAt != null;
        }
        return false;
    }

    public void leave(Long accountId) {
        if (hasLeft(accountId)) {
            return;
        }
        if (isSender(accountId)) {
            senderLeftAt = LocalDateTime.now();
        } else if (isShipper(accountId)) {
            shipperLeftAt = LocalDateTime.now();
        }
    }

    private boolean isSender(Long accountId) {
        return delivery.getSender() != null && delivery.getSender().getId().equals(accountId);
    }

    private boolean isShipper(Long accountId) {
        return delivery.getShipper() != null && delivery.getShipper().getId().equals(accountId);
    }
}
