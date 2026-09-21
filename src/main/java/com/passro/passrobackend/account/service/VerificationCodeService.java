package com.passro.passrobackend.account.service;

import com.passro.passrobackend.account.dto.authDTO.AuthReqDTO;
import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.exception.AccountException;
import com.passro.passrobackend.account.exception.code.AccountErrorCode;
import com.passro.passrobackend.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class VerificationCodeService {

    private final AccountRepository accountRepository;

    private final StringRedisTemplate stringRedisTemplate;

    //인증 코드
    private static final String CODE_PREFIX = "mail:verify:code:";

    //인증 자격
    private static final String VERIFIED_PREFIX = "mail:verify:done:";
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);


    public void confirmUniversityMailCode(AuthReqDTO.ConfirmCode dto, Long accountId) {
        String mail = dto.getMail();
        String code = dto.getCode();

        String savedCode = stringRedisTemplate.opsForValue().get(CODE_PREFIX + mail);

        confirmSavedCode(code, savedCode);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        account.certify();

        accountRepository.save(account);

        stringRedisTemplate.delete(CODE_PREFIX + mail);
    }

    public void confirmCode(AuthReqDTO.ConfirmCode dto) {
        String mail = dto.getMail();
        String code = dto.getCode();

        String savedCode = stringRedisTemplate.opsForValue().get(CODE_PREFIX + mail);

        confirmSavedCode(code, savedCode);

        stringRedisTemplate.delete(CODE_PREFIX + mail);
        stringRedisTemplate.opsForValue().set(VERIFIED_PREFIX + mail, "true", VERIFIED_TTL);

    }

    public void confirmSavedCode(String code, String savedCode) {
        if (savedCode == null)
            throw new AccountException(AccountErrorCode.MAIL_CODE_EXPIRED);
        if (!savedCode.equals(code))
            throw new AccountException(AccountErrorCode.MAIL_CODE_MISMATCH);
    }
}
