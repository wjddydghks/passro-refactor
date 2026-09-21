package com.passro.passrobackend.domain.account.service;

import com.passro.passrobackend.domain.account.dto.authDTO.AuthReqDTO;
import com.passro.passrobackend.domain.account.exception.AccountException;
import com.passro.passrobackend.domain.account.exception.code.AccountErrorCode;
import com.passro.passrobackend.domain.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@RequiredArgsConstructor
@Service
public class MailSenderService {

    private final AsyncMailService asyncMailService;

    private final AccountRepository accountRepository;

    private final StringRedisTemplate stringRedisTemplate;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    //인증 코드
    private static final String CODE_PREFIX = "mail:verify:code:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    //인증 요청 대기
    private static final String RESEND_COOLDOWN_PREFIX = "mail:verify:cooldown:";
    private static final Duration RESEND_COOLDOWN_TTL = Duration.ofSeconds(60);


    public void sendMailMessageSignUpOrShipperSelect(AuthReqDTO.SendMail dto) {
        String mail = dto.getMail();

        sendMail(mail);
    }

    private void sendMail(String mail) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(RESEND_COOLDOWN_PREFIX + mail)))
            throw new AccountException(AccountErrorCode.TOO_FAST);

        String code = generateCode();

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        // 메일을 받을 수신자 설정
        simpleMailMessage.setTo(mail);
        // 메일의 제목 설정
        simpleMailMessage.setSubject("[Passro] 이메일 인증 코드");
        // 메일의 내용 설정
        simpleMailMessage.setText("인증 코드: " + code + "\n5분 이내에 입력해주세요.");

        asyncMailService.send(simpleMailMessage);

        stringRedisTemplate.opsForValue().set(CODE_PREFIX + mail, code, CODE_TTL);
        stringRedisTemplate.opsForValue().set(RESEND_COOLDOWN_PREFIX + mail, "true", RESEND_COOLDOWN_TTL);
    }

    private String generateCode() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
