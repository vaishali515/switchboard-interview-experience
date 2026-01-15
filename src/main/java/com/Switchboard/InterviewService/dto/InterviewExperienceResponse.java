package com.Switchboard.InterviewService.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewExperienceResponse {
    private UUID id;
    private String userName;
    private String userEmail;
    private String title;
    private String imageName;
    private String content;
    private String companyTag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
