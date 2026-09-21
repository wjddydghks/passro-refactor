package com.passro.passrobackend.account.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordComplexityValidator implements ConstraintValidator<PasswordComplexity, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return false;

        int typeCount = 0;
        if (password.matches(".*[a-z].*")) typeCount++;       // 영문 소문자
        if (password.matches(".*[A-Z].*")) typeCount++;       // 영문 대문자
        if (password.matches(".*[0-9].*")) typeCount++;       // 숫자
        if (password.matches(".*[^a-zA-Z0-9].*")) typeCount++; // 특수문자

        return typeCount >= 2;
    }
}