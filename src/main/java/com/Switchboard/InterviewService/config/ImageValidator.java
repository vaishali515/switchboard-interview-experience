package com.Switchboard.InterviewService.config;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        // Null or empty is allowed — other annotations like @NotNull should handle required check
        if (file == null || file.isEmpty()) {
            return true;
        }

        String contentType = file.getContentType();
        return contentType != null && ALLOWED_TYPES.contains(contentType.toLowerCase());
    }
}
