package com.passro.passrobackend.domain.account.service;

import com.passro.passrobackend.domain.account.dto.authDTO.AuthReqDTO;
import com.passro.passrobackend.domain.account.entity.Account;
import com.passro.passrobackend.domain.account.exception.AccountException;
import com.passro.passrobackend.domain.account.exception.code.AccountErrorCode;
import com.passro.passrobackend.domain.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class VerificationCodeService {

    private final StringRedisTemplate stringRedisTemplate;

    //인증 코드
    private static final String CODE_PREFIX = "mail:verify:code:";

    //인증 요청 시도 카운트
    private static final String ATTEMPTS_PREFIX = "mail:verify:attempts:";
    private static final Duration ATTEMPTS_TTL = Duration.ofMinutes(5);
    private static final long MAX_ATTEMPTS = 5;

    //인증 자격
    private static final String VERIFIED_PREFIX = "mail:verify:done:";
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);

  
    public void confirmCode(AuthReqDTO.ConfirmCode dto) {
        String mail = dto.getMail();
        String code = dto.getCode();

        String savedCode = stringRedisTemplate.opsForValue().get(CODE_PREFIX + mail);

        if (savedCode == null)
            throw new AccountException(AccountErrorCode.MAIL_CODE_EXPIRED);

        confirmAttempts(mail);
        confirmSavedCode(code, savedCode);

        stringRedisTemplate.delete(CODE_PREFIX + mail);
        stringRedisTemplate.delete(ATTEMPTS_PREFIX + mail);
        stringRedisTemplate.opsForValue().set(VERIFIED_PREFIX + mail, "true", VERIFIED_TTL);

    }

    private void confirmSavedCode(String code, String savedCode) {
        if (!savedCode.equals(code)) {
            throw new AccountException(AccountErrorCode.MAIL_CODE_MISMATCH);
        }
    }

    private void confirmAttempts(String mail)
    {
        Long attempts = stringRedisTemplate.opsForValue().increment(ATTEMPTS_PREFIX + mail);
        if(attempts !=null && attempts ==1L) {
            stringRedisTemplate.expire(ATTEMPTS_PREFIX + mail, ATTEMPTS_TTL);
        }
        if(attempts != null && attempts > MAX_ATTEMPTS) {
            stringRedisTemplate.delete(CODE_PREFIX + mail);
            stringRedisTemplate.delete(ATTEMPTS_PREFIX + mail);
            throw new AccountException(AccountErrorCode.TOO_FAST);
        }
    }
}
