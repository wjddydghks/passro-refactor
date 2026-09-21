package com.passro.passrobackend.account.service;

import com.passro.passrobackend.account.dto.accountDTO.AccountReqDTO;
import com.passro.passrobackend.account.dto.accountDTO.AccountResDTO;
import com.passro.passrobackend.account.entity.Account;
import com.passro.passrobackend.account.entity.AccountPlace;
import com.passro.passrobackend.account.entity.WayPoint;
import com.passro.passrobackend.account.exception.AccountException;
import com.passro.passrobackend.account.exception.code.AccountErrorCode;
import com.passro.passrobackend.account.repository.AccountPlaceRepository;
import com.passro.passrobackend.account.repository.AccountRepository;
import com.passro.passrobackend.account.repository.WayPointRepository;
import com.passro.passrobackend.delivery.repository.DeliveryRepository;
import com.passro.passrobackend.file.service.S3Service;
import com.passro.passrobackend.notification.service.NotificationService;
import com.passro.passrobackend.place.entity.Place;
import com.passro.passrobackend.place.repository.PlaceRepository;
import com.passro.passrobackend.review.dto.ReviewAverageResponseDto;
import com.passro.passrobackend.review.service.ReviewService;
import com.passro.passrobackend.subway.dto.SubwayStationResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final DeliveryRepository deliveryRepository;
    private final ReviewService reviewService;
    private final S3Service s3Service;

    private final AccountRepository accountRepository;
    private final AccountPlaceRepository accountPlaceRepository;
    private final WayPointRepository wayPointRepository;
    private final PlaceRepository placeRepository;

    private final PasswordEncoder passwordEncoder;

    private final StringRedisTemplate stringRedisTemplate;

    //닉네임 변경 대기
    private static final String EDIT_INFO_COOLDOWN_PREFIX = "edit:info:verify:code";

    //비밀번호 변경 대기
    private static final String EDIT_PASSWORD_COOLDOWN_PREFIX = "edit:password:verify:code";

    //내 정보 변경 시간
    private static final Duration EDIT_COOLDOWN_TTL = Duration.ofMinutes(5);



    public boolean isNicknameAvailable(String nickname) {
        return !accountRepository.existsByNickname(nickname);
    }

    public boolean isMailAvailable(String mail) {
        return !accountRepository.existsByMail(mail);
    }

    public AccountResDTO.ShipperMyPage myShipperPage(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(()-> new AccountException(AccountErrorCode.NOT_FOUND));
        AccountPlace accountPlace = accountPlaceRepository.findByAccount(account)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        String picture = null;
        if (account.getPicture() != null)
            picture = s3Service.getPresignedDownloadUrl(account.getPicture()).toString();

        String nickname = account.getNickname();
        String name = account.getName();
        var birth = account.getBirth();
        String phoneNumber = account.getPhoneNumber();
        SubwayStationResponseDto startPlace = SubwayStationResponseDto.from(accountPlace.getStartPlace());
        SubwayStationResponseDto destinationPlace = SubwayStationResponseDto.from(accountPlace.getDestinationPlace());
        List<SubwayStationResponseDto> wayPoints = wayPointRepository
                .findAllByAccountPlaceOrderByVisitOrderAsc(accountPlace)
                .stream()
                .map(WayPoint::getPlace)
                .map(SubwayStationResponseDto::from)
                .toList();
        Long deliveryCount = deliveryRepository.countByShipper(account);
        Long point = account.getPoint();

        double rating = 0.0;
        ReviewAverageResponseDto ratingDTO = reviewService.getAverageRating(accountId);
        if (ratingDTO != null)
            rating = ratingDTO.getAverageRating();

        return new AccountResDTO.ShipperMyPage(
                picture, nickname, name, birth, phoneNumber, startPlace, destinationPlace, wayPoints,
                deliveryCount, point, account.getCreatedAt(), rating);
    }

    public AccountResDTO.SenderMyPage mySenderPage(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(()-> new AccountException(AccountErrorCode.NOT_FOUND));

        String picture = null;
        if (account.getPicture() != null) {
            picture = s3Service.getPresignedDownloadUrl(account.getPicture()).toString();
        }

        String nickname = account.getNickname();
        Long deliveryCount = deliveryRepository.countBySender(account);
        Long point = account.getPoint();

        return new AccountResDTO.SenderMyPage(picture, nickname, deliveryCount, point);
    }

    public boolean isStudentCertified(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND));

        return Boolean.TRUE.equals(account.getCertified());
    }

    @Transactional
    public void editMyInfo(AccountReqDTO.EditMyInfo dto, Long accountId) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(EDIT_INFO_COOLDOWN_PREFIX + accountId)))
            throw new AccountException(AccountErrorCode.TOO_FAST);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(()-> new AccountException(AccountErrorCode.NOT_FOUND));

        if (!account.getNickname().equals(dto.getNickname()) && accountRepository.existsByNickname(dto.getNickname()))
            throw new AccountException(AccountErrorCode.DUPLICATE_NICKNAME);

        if (!account.getPhoneNumber().equals(dto.getPhoneNumber()) && accountRepository.existsByPhoneNumber(dto.getPhoneNumber()))
            throw new AccountException(AccountErrorCode.DUPLICATE_PHONE_NUMBER);

        AccountPlace accountPlace = accountPlaceRepository.findByAccount(account)
                .orElseThrow(()-> new AccountException(AccountErrorCode.NOT_FOUND));

        Place startPlace = placeRepository.findById(dto.getStartPlaceId())
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND_SUBWAY));
        Place destinationPlace = placeRepository.findById(dto.getDestinationPlaceId())
                .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND_SUBWAY));

        wayPointRepository.deleteAllByAccountPlace(accountPlace);

        if (dto.getWayPoints() != null) {
            for (int i = 0; i < dto.getWayPoints().size(); i++) {
                Long wayPointPlaceId = dto.getWayPoints().get(i);
                Place wayPointPlace = placeRepository.findById(wayPointPlaceId)
                        .orElseThrow(() -> new AccountException(AccountErrorCode.NOT_FOUND_SUBWAY));

                wayPointRepository.save(WayPoint.builder()
                        .accountPlace(accountPlace)
                        .place(wayPointPlace)
                        .visitOrder(i)
                        .build());

            }
        }

        accountPlace.changePlace(startPlace, destinationPlace);
        account.changeNickname(dto.getNickname());
        account.changeName(dto.getName());
        account.changeBirth(dto.getBirth());
        account.changePhoneNumber(dto.getPhoneNumber());
        if (dto.getPicture() != null && !dto.getPicture().isBlank()
                && !dto.getPicture().equals(account.getPicture())) {
            s3Service.validateUploadedImage(dto.getPicture());
            account.changePicture(dto.getPicture());
        }

        accountRepository.save(account);

        stringRedisTemplate.opsForValue().set(EDIT_INFO_COOLDOWN_PREFIX + accountId, "true", EDIT_COOLDOWN_TTL);
    }

    public void editPassword(AccountReqDTO.EditPassword dto, Long accountId) {

        //변경 시간 텀 검증
        if(Boolean.TRUE.equals(stringRedisTemplate.hasKey(EDIT_PASSWORD_COOLDOWN_PREFIX + accountId)))
            throw new AccountException(AccountErrorCode.TOO_FAST);

        String nowPassword = dto.getNowPassword();
        String editPassword = dto.getEditPassword();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(()-> new AccountException(AccountErrorCode.NOT_FOUND));

        //현재 비밀번호 입력 검증
        if (!passwordEncoder.matches(nowPassword, account.getPassword()))
            throw new AccountException(AccountErrorCode.INVALID_CREDENTIALS);

        //현재 비밀번호와 수정할 비밀번호 같은지 비교
        if(editPassword.equals(nowPassword))
            throw new AccountException(AccountErrorCode.SAME_PASSWORD);

        account.changePassword(passwordEncoder.encode(editPassword));
        accountRepository.save(account);

        stringRedisTemplate.opsForValue().set(EDIT_PASSWORD_COOLDOWN_PREFIX + accountId, "true", EDIT_COOLDOWN_TTL);
    }
}
