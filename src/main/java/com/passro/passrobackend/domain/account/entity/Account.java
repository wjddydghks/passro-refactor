package com.passro.passrobackend.domain.account.entity;

import com.passro.passrobackend.domain.account.enums.AccountRole;
import com.passro.passrobackend.global.entity.BaseEntity;
import com.passro.passrobackend.domain.place.entity.Place;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Account extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String mail;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birth;

    @ManyToOne
    private Place place_id;

    private Long point;

    private String picture;

    @Enumerated(EnumType.STRING)
    private AccountRole role;

  
    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void changeBirth(LocalDate birth) {
        this.birth = birth;
    }

    public void changePassword(String password){
        this.password = password;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void changePicture(String picture) {
        this.picture = picture;
    }

    public void usePoint(long amount) {
        validatePointAmount(amount);
        long currentPoint = currentPoint();
        if (currentPoint < amount) {
            throw new IllegalStateException("보유 포인트가 부족합니다.");
        }
        this.point = currentPoint - amount;
    }

    public void earnPoint(long amount) {
        validatePointAmount(amount);
        this.point = Math.addExact(currentPoint(), amount);
    }

    public long currentPoint() {
        return point == null ? 0L : point;
    }

    private void validatePointAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("포인트는 0보다 커야 합니다.");
        }
    }
}
