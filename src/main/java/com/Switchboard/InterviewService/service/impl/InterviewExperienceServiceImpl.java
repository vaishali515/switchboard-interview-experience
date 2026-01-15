package com.Switchboard.InterviewService.service.impl;

import com.Switchboard.InterviewService.config.AppConstants;
import com.Switchboard.InterviewService.dto.InterviewExperienceRequest;
import com.Switchboard.InterviewService.dto.InterviewExperienceResponse;
import com.Switchboard.InterviewService.dto.PageResponseDTO;
import com.Switchboard.InterviewService.model.InterviewExperience;
import com.Switchboard.InterviewService.repository.InterviewExperienceRepository;
import com.Switchboard.InterviewService.service.FileService;
import com.Switchboard.InterviewService.service.InterviewExperienceService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewExperienceServiceImpl implements InterviewExperienceService {
    private static final Logger log = LoggerFactory.getLogger(InterviewExperienceServiceImpl.class);

    private final InterviewExperienceRepository repository;
    private final FileService fileService;

    private final ModelMapper modelMapper;

    @Override
    public InterviewExperienceResponse createInterviewExperience(InterviewExperienceRequest request, String imageUrl) {
        log.info("InterviewExperienceServiceImpl :: createInterviewExperience :: mapping :: request to entity");
        InterviewExperience experience = modelMapper.map(request, InterviewExperience.class);
        
        // Set image URL if provided
        if (imageUrl != null) {
            experience.setImageName(imageUrl);
        }

        log.info("InterviewExperienceServiceImpl :: createInterviewExperience :: saving :: interview experience");
        InterviewExperience newExperience = repository.save(experience);

        log.info("InterviewExperienceServiceImpl :: createInterviewExperience :: mapping :: entity to response");
        return modelMapper.map(newExperience, InterviewExperienceResponse.class);

    }

    @Override
    public List<InterviewExperienceResponse> searchByEmail(String userEmail) {
        log.info("InterviewExperienceServiceImpl :: searchByEmail :: searching :: experiences for email: {}", userEmail);
        List<InterviewExperience> experiences = repository.findByUserEmailOrderByCreatedAtDesc(userEmail);

        log.info("InterviewExperienceServiceImpl :: searchByEmail :: found :: {} experiences", experiences.size());
        return experiences.stream()
                .map(experience -> {
                    log.debug("InterviewExperienceServiceImpl :: searchByEmail :: mapping :: experience to response");
                    return modelMapper.map(experience, InterviewExperienceResponse.class);
                })
                .collect(Collectors.toList());
    }


    @Override
    public List<InterviewExperienceResponse> searchByCompany(String companyTag) {
        log.info("InterviewExperienceServiceImpl :: searchByCompany :: searching :: experiences for company: {}", companyTag);
        List<InterviewExperience> experiences = repository.findByCompanyTagOrderByCreatedAtDesc(companyTag);

        log.info("InterviewExperienceServiceImpl :: searchByCompany :: found :: {} experiences", experiences.size());
        return experiences.stream()
                .map(experience -> modelMapper.map(experience, InterviewExperienceResponse.class))
                .collect(Collectors.toList());
    }


    @Override
    public PageResponseDTO getAllInterviews(Integer pageNumber, Integer pageSize, String sortBy, String sortDir) {
        log.info("InterviewExperienceServiceImpl :: getAllInterviews :: fetching :: page {} with size {}", pageNumber, pageSize);
        Sort sort = (sortDir.equalsIgnoreCase("asc")) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable p = PageRequest.of(pageNumber, pageSize, sort);

        Page<InterviewExperience> experiences = repository.findAll(p);
        List<InterviewExperience> experienceList = experiences.getContent();
        log.info("InterviewExperienceServiceImpl :: getAllInterviews :: found :: {} experiences", experienceList.size());

        List<InterviewExperienceResponse> res = experienceList.stream()
                .map(experience -> modelMapper.map(experience, InterviewExperienceResponse.class))
                .collect(Collectors.toList());

        return PageResponseDTO.builder()
                .content(res)
                .pageNumber(experiences.getNumber())
                .pageSize(experiences.getSize())
                .totalElements(experiences.getTotalElements())
                .totalPages(experiences.getTotalPages())
                .lastPage(experiences.isLast())
                .build();
    }

    @Override
    public InterviewExperienceResponse getInterviewById(UUID id) {
        log.info("InterviewExperienceServiceImpl :: getInterviewById :: fetching :: experience with id: {}", id);
        InterviewExperience experience = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("InterviewExperienceServiceImpl :: getInterviewById :: not found :: experience with id: {}", id);
                    return new RuntimeException("Interview Experience not found");
                });

        log.info("InterviewExperienceServiceImpl :: getInterviewById :: mapping :: experience to response");
        return modelMapper.map(experience, InterviewExperienceResponse.class);
    }


    @Override
    public void deleteInterviewExperience(UUID id) {
        log.info("InterviewExperienceServiceImpl :: deleteInterviewExperience :: deleting :: experience with id: {}", id);

        // Fetch existing record
        InterviewExperience experience = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("InterviewExperienceServiceImpl :: deleteInterviewExperience :: not found :: experience with id: {}", id);
                    return new RuntimeException("Interview Experience not found");
                });
        //log.info("Deleting experience: {}, imageName: {}", experience.getId(), experience.getImageName());

        // Delete image from S3 if exists
        if (experience.getImageName() != null && !experience.getImageName().isEmpty()) {
            try {
                log.info("InterviewExperienceServiceImpl :: deleting image from S3: {}", experience.getImageName());
                fileService.deleteImage(experience.getImageName());
                log.info("InterviewExperienceServiceImpl :: deleted image from S3: {}", experience.getImageName());
            } catch (Exception e) {
                log.error("InterviewExperienceServiceImpl :: failed to delete image from S3: {}", e.getMessage());
                // Optional: you can throw exception if you want to fail delete if image deletion fails
            }
        }

        // Delete DB record
        repository.delete(experience);
        log.info("InterviewExperienceServiceImpl :: deleteInterviewExperience :: deleted DB record with id: {}", id);
    }

    @Override
    public InterviewExperienceResponse updateInterviewExperience(UUID id, InterviewExperienceRequest request, MultipartFile newImage) throws IOException {
        log.info("InterviewExperienceServiceImpl :: updateInterviewExperience :: updating :: experience with id: {}", id);

        // Fetch existing record
        InterviewExperience experience = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("InterviewExperienceServiceImpl :: updateInterviewExperience :: not found :: experience with id: {}", id);
                    return new RuntimeException("Interview Experience not found");
                });

        // Handle new image upload
        if (newImage != null && !newImage.isEmpty()) {
            // Delete old image from S3 if exists
            if (experience.getImageName() != null && !experience.getImageName().isEmpty()) {
                log.info("InterviewExperienceServiceImpl :: deleting old image from S3: {}", experience.getImageName());
                fileService.deleteImage(experience.getImageName());
            }

            // Upload new image
            String newImageUrl = fileService.uploadImage(AppConstants.PATH_VARIABLE, newImage);
            log.info( "InterviewExperienceServiceImpl :: uploaded new image to S3: {}", newImageUrl);
            experience.setImageName(newImageUrl);
        }

        // Update other fields
        experience.setUserName(request.getUserName());
        experience.setTitle(request.getTitle());
        experience.setCompanyTag(request.getCompanyTag());
        experience.setUserEmail(request.getUserEmail());
        experience.setContent(request.getContent());
        log.info("InterviewExperienceServiceImpl :: updateInterviewExperience :: updated fields from request {}" ,experience);

        // Save updated entity
        InterviewExperience updatedExperience = repository.save(experience);

        log.info("InterviewExperienceServiceImpl :: updateInterviewExperience :: saved :: updated experience");

        return modelMapper.map(updatedExperience, InterviewExperienceResponse.class);
    }

}
