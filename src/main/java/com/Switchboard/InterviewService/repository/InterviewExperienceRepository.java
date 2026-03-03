package com.Switchboard.InterviewService.repository;

import com.Switchboard.InterviewService.model.InterviewExperience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewExperienceRepository extends JpaRepository<InterviewExperience, UUID> {

    List<InterviewExperience> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Page<InterviewExperience> findByCompanyTagContainingIgnoreCase(
            String companyTag,
            Pageable pageable
    );
}
