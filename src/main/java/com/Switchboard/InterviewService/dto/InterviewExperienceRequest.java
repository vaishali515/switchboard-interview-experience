package com.Switchboard.InterviewService.dto;

import com.Switchboard.InterviewService.config.ValidImage;
import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewExperienceRequest {

    private String userName;

    @Email(message = "Invalid email format")
    private String userEmail;

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    @Size(min = 10, message = "Content must be at least 10 characters long")
    private String content;

    private String companyTag;
    
    @ValidImage
    private MultipartFile image;
}
