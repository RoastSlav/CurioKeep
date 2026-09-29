package org.rostislav.curiokeep.user;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A new local-account password: at least {@value #MIN_LENGTH} characters and at most {@value #MAX_BYTES} UTF-8 bytes. */
@Documented
@Constraint(validatedBy = ValidPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    int MIN_LENGTH = 10;
    /** bcrypt ignores or rejects anything beyond 72 bytes, so a longer password is refused instead of silently truncated. */
    int MAX_BYTES = 72;

    String message() default "Password must be at least 10 characters and at most 72 bytes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
