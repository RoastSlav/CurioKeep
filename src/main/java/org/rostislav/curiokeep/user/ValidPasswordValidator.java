package org.rostislav.curiokeep.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return true; // presence is @NotBlank's job
        return password.codePointCount(0, password.length()) >= ValidPassword.MIN_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= ValidPassword.MAX_BYTES;
    }
}
