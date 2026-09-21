package com.passro.passrobackend.account.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordComplexityValidator.class)
public @interface PasswordComplexity {
    String message() default "비밀번호는 영문 대문자, 소문자, 숫자, 특수문자 중 2가지 이상을 조합해야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}