package com.passro.passrobackend.account.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncMailService {

    private final JavaMailSender javaMailSender;

    @Async
    public void send(SimpleMailMessage message) {
        javaMailSender.send(message);
    }
}
