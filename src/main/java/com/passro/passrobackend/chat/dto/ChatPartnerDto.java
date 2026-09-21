package com.passro.passrobackend.chat.dto;

import com.passro.passrobackend.account.entity.Account;
import java.util.function.Function;

public record ChatPartnerDto(
        Long id,
        String nickname,
        String picture
) {
    public static ChatPartnerDto from(
            Account account, Function<String, String> imageUrlResolver) {
        return new ChatPartnerDto(
                account.getId(),
                account.getNickname(),
                imageUrlResolver.apply(account.getPicture())
        );
    }
}
