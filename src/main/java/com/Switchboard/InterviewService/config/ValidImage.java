package com.Switchboard.InterviewService.config;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ImageValidator.class)
@Documented
public @interface ValidImage {

    String message() default "Invalid image type. Allowed types are JPG, JPEG, PNG, GIF, PNB and WebP.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
