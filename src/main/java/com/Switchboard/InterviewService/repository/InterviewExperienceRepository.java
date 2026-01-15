package com.Switchboard.InterviewService.repository;

import com.Switchboard.InterviewService.model.InterviewExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewExperienceRepository extends JpaRepository<InterviewExperience, UUID> {

    List<InterviewExperience> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    List<InterviewExperience> findByCompanyTagOrderByCreatedAtDesc(String companyTag);
}
