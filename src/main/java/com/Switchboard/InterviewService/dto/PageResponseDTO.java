package com.Switchboard.InterviewService.dto;


import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponseDTO {

    private List<InterviewExperienceResponse> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements ;
    private int totalPages;
    private boolean lastPage;

}
