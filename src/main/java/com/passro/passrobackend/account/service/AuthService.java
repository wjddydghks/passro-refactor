package com.passro.passrobackend.account.service;

import com.passro.passrobackend.account.dto.authDTO.AuthReqDTO;
import com.passro.passrobackend.account.dto.authDTO.AuthResDTO;
import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.account.entity.WayPoint;
import com.passro.passrobackend.account.enums.AccountRole;
import com.passro.passrobackend.account.exception.AccountException;
import com.passro.passrobackend.account.exception.code.AccountErrorCode;
import com.passro.passrobackend.account.repository.AccountPlaceRepository;
import com.passro.passrobackend.account.repository.AccountRepository;
import com.passro.passrobackend.account.repository.WayPointRepository;
import com.passro.passrobackend.global.jwt.JwtProperties;
import com.passro.passrobackend.global.jwt.JwtProvider;
import com.passro.passrobackend.notification.enums.NotificationType;
import com.passro.passrobackend.notification.enums.ResourceType;
import com.passro.passrobackend.notification.service.NotificationService;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final AsyncMailService asyncMailService;
    private final NotificationService notificationService;

    private final AccountRepository accountRepository;
    private final AccountPlaceRepository accountPlaceRepository;
    private final WayPointRepository wayPointRepository;
    private final PlaceRepository placeRepository;

    private final PasswordEncoder passwordEncoder;

    private final StringRedisTemplate stringRedisTemplate;

    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    //인증 자격
    private static final String VERIFIED_PREFIX = "mail:verify:done:";

    //인증 요청 대기
    private static final String RESEND_COOLDOWN_PREFIX = "mail:verify:cooldown:";

    private static final String TEMP_PASSWORD_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private static final String REFRESH_PREFIX = "refresh:token:";


    @Transactional
    public void signup(AuthReqDTO.Signup dto) {
        String confirmStatus = stringRedisTemplate.opsForValue().get(VERIFIED_PREFIX + dto.getMail());

        if (confirmStatus == null || !confirmStatus.equals("true"))
            throw new AccountException(AccountErrorCode.MAIL_NOT_CONFIRM);


        if (accountRepository.existsByMail(dto.getMail()))
            throw new AccountException(AccountErrorCode.DUPLICATE_MAIL);

        if (accountRepository.existsByNickname(dto.getNickname()))
            throw new AccountException(AccountErrorCode.DUPLICATE_NICKNAME);

        String password = passwordEncoder.encode(dto.getPassword());

        Place sourcePlace = getPlace(dto.getSourceStationId());
        Place destinationPlace = getPlace(dto.getDestinationStationId());


        Account account = accountRepository.save(Account.builder()
                .mail(dto.getMail())
                .password(password)
                .nickname(dto.getNickname())
                .name(dto.getName())
                .phoneNumber(dto.getPhoneNumber())
                .birth(dto.getBirth())
                .certified(false)
                .point(10000L)
                .picture(dto.getPicture())
                .role(AccountRole.USER)
                .build());

        AccountPlace accountPlace = accountPlaceRepository.save(AccountPlace.builder()
                .account(account)
                .startPlace(sourcePlace)
                .destinationPlace(destinationPlace)
                .build());

        saveWayPoints(dto.getWayPoints(), accountPlace);

        notificationService.publish(account, NotificationType.GENERAL, "회원가입을 환영합니다!", "10,000포인트가 지급되었습니다", ResourceType.NONE, null);

        stringRedisTemplate.delete(VERIFIED_PREFIX + dto.getMail());
        stringRedisTemplate.delete(RESEND_COOLDOWN_PREFIX + dto.getMail());
    }

    public AuthResDTO.TokenResponse login(AuthReqDTO.Login dto) {
        Account account = accountRepository.findByMail(dto.getMail())
                .orElseThrow(() -> new AccountException(AccountErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(dto.getPassword(), account.getPassword()))
            throw new AccountException(AccountErrorCode.INVALID_CREDENTIALS);

        return issueTokens(account);
    }

    public void logout(Long accountId) {
        stringRedisTemplate.delete(REFRESH_PREFIX + accountId);
    }


    private AuthResDTO.TokenResponse issueTokens(Account account) {
        String accessToken = jwtProvider.createAccessToken(account.getId(), account.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(account.getId());

        stringRedisTemplate.opsForValue().set(REFRESH_PREFIX + account.getId(), refreshToken,
                Duration.ofMillis(jwtProperties.getRefreshTokenExpiration()));

        return new AuthResDTO.TokenResponse(accessToken, refreshToken);
    }

    public AuthResDTO.TokenResponse reissueToken(AuthReqDTO.ReIssue dto) {
        String refreshToken = dto.getRefreshToken();

        if (!jwtProvider.validateToken(refreshToken))
            throw new AccountException(AccountErrorCode.INVALID_REFRESH_TOKEN);

        Long accountId = jwtProvider.getAccountId(refreshToken);
        String savedToken = stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + accountId);

        if (savedToken == null || !savedToken.equals(refreshToken))
            throw new AccountException(AccountErrorCode.INVALID_REFRESH_TOKEN);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        return issueTokens(account);
    }


    public void findId(AuthReqDTO.FindId dto) {
        accountRepository.findFirstByNameAndPhoneNumber(dto.getName(), dto.getPhoneNumber())
                .ifPresent(account -> {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(account.getMail());
                    message.setSubject("[Passro] 아이디 찾기 안내");
                    message.setText("가입된 아이디(이메일): " + account.getMail());
                    asyncMailService.send(message);
                });
    }

    @Transactional
    public void findPassword(AuthReqDTO.FindPassword dto) {
        accountRepository.findFirstByNameAndPhoneNumberAndMail(
                        dto.getName(), dto.getPhoneNumber(), dto.getMail())
                .ifPresent(account -> {
                    String temporaryPassword = generateTemporaryPassword();
                    account.changePassword(passwordEncoder.encode(temporaryPassword));
                    accountRepository.save(account);

                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(account.getMail());
                    message.setSubject("[Passro] 임시 비밀번호 안내");
                    message.setText("임시 비밀번호: " + temporaryPassword
                            + "\n로그인 후 비밀번호를 변경해주세요.");

                    asyncMailService.send(message);
                });
    }


    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int index = 0; index < TEMP_PASSWORD_LENGTH; index++) {
            password.append(TEMP_PASSWORD_CHARACTERS.charAt(
                    SECURE_RANDOM.nextInt(TEMP_PASSWORD_CHARACTERS.length())));
        }
        return password.toString();
    }

    public Place getPlace(Long stationId) {
        return placeRepository.findById(stationId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND_SUBWAY));
    }

    private void saveWayPoints(List<Long> wayPoints, AccountPlace accountPlace) {
        if (wayPoints != null) {
            for (int i = 0; i < wayPoints.size(); i++) {
                Long wayPointPlaceId = wayPoints.get(i);
                Place wayPointPlace = placeRepository.findById(wayPointPlaceId)
                        .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND_SUBWAY));

                wayPointRepository.save(WayPoint.builder()
                        .accountPlace(accountPlace)
                        .place(wayPointPlace)
                        .visitOrder(i)
                        .build());
            }
        }

    }
}
