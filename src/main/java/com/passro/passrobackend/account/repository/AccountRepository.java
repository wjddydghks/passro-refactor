package com.passro.passrobackend.account.repository;

import com.passro.passrobackend.account.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByMail(String mail);

    boolean existsByNickname(String nickname);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional <Account> findByMail(String mail);

    Optional<Account> findFirstByNameAndPhoneNumber(String name, String phoneNumber);

    Optional<Account> findFirstByNameAndPhoneNumberAndMail(String name, String phoneNumber, String mail);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
