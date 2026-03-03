package com.Switchboard.InterviewService.service.impl;

import com.Switchboard.InterviewService.config.AppConstants;
import com.Switchboard.InterviewService.dto.InterviewExperienceRequest;
import com.Switchboard.InterviewService.dto.InterviewExperienceResponse;
import com.Switchboard.InterviewService.dto.PageResponseDTO;
import com.Switchboard.InterviewService.model.InterviewExperience;
import com.Switchboard.InterviewService.repository.InterviewExperienceRepository;
import com.Switchboard.InterviewService.service.FileService;
import com.Switchboard.InterviewService.service.InterviewExperienceService;
import com.Switchboard.InterviewService.service.cache.InterviewCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewExperienceServiceImpl implements InterviewExperienceService {
    private static final Logger log = LoggerFactory.getLogger(InterviewExperienceServiceImpl.class);

    private final InterviewExperienceRepository repository;
    private final FileService fileService;
    private final InterviewCacheService cacheService;
    private final ObjectMapper objectMapper;
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

        InterviewExperienceResponse response =
                modelMapper.map(newExperience, InterviewExperienceResponse.class);

        // save object cache
        try {
            String json = objectMapper.writeValueAsString(response);
            cacheService.save(newExperience.getId(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize interview response", e);
        }

// add to latest index
        cacheService.addToIndex(
                newExperience.getId(),
                newExperience.getUpdatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );
      cacheService.trimIndex(10000); // keep only latest 100 entries in index
        return response;

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
    public PageResponseDTO searchByCompany(String companyTag,
                                           Integer pageNumber,
                                           Integer pageSize,
                                           String sortBy,
                                           String sortDir) {

        log.info("InterviewExperienceServiceImpl :: searchByCompany :: searching company {}", companyTag);

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<InterviewExperience> page =
                repository.findByCompanyTagContainingIgnoreCase(companyTag, pageable);

        List<InterviewExperienceResponse> content = page.getContent()
                .stream()
                .map(exp -> modelMapper.map(exp, InterviewExperienceResponse.class))
                .collect(Collectors.toList());

        return PageResponseDTO.builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .lastPage(page.isLast())
                .build();
    }


    @Override
    public PageResponseDTO getAllInterviews(Integer pageNumber, Integer pageSize, String sortBy, String sortDir) {

        log.info("InterviewExperienceServiceImpl :: getAllInterviews :: page={} size={}", pageNumber, pageSize);

        int start = pageNumber * pageSize;
        int end = start + pageSize - 1;

        log.info("Calculated Redis range :: start={} end={}", start, end);

        Set<String> ids = cacheService.getLatestIds(start, end);

        List<InterviewExperienceResponse> responses = new ArrayList<>();

        if (ids != null && !ids.isEmpty()) {

            log.info("Redis index returned {} interview IDs", ids.size());

            for (String id : ids) {

                String json = cacheService.get(UUID.fromString(id));

                if (json != null) {

                    try {

                        InterviewExperienceResponse response =
                                objectMapper.readValue(json, InterviewExperienceResponse.class);

                        responses.add(response);

                        log.info("CACHE HIT :: {}", id);

                    } catch (Exception e) {

                        log.error("Cache parse error {}", id, e);
                    }

                } else {

                    log.warn("CACHE MISS :: {} → fetching DB", id);

                    InterviewExperience exp = repository.findById(UUID.fromString(id)).orElse(null);

                    if (exp != null) {

                        InterviewExperienceResponse response =
                                modelMapper.map(exp, InterviewExperienceResponse.class);

                        responses.add(response);

                        try {

                            String newJson = objectMapper.writeValueAsString(response);

                            cacheService.save(exp.getId(), newJson);

                            log.info("Repopulated Redis cache {}", id);

                        } catch (Exception e) {

                            log.error("Cache store failed {}", id, e);
                        }
                    }
                }
            }

            log.info("Returning {} interviews from Redis", responses.size());

            return PageResponseDTO.builder()
                    .content(responses)
                    .pageNumber(pageNumber)
                    .pageSize(pageSize)
                    .totalElements(responses.size())
                    .totalPages(1)
                    .lastPage(true)
                    .build();
        }

        // Redis empty → DB fallback

        log.warn("Redis index empty :: DB fallback");

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<InterviewExperience> experiences = repository.findAll(pageable);

        List<InterviewExperienceResponse> res = new ArrayList<>();

        for (InterviewExperience exp : experiences.getContent()) {

            InterviewExperienceResponse response =
                    modelMapper.map(exp, InterviewExperienceResponse.class);

            res.add(response);

            try {

                String json = objectMapper.writeValueAsString(response);

                //cacheService.save(exp.getId(), json);

                cacheService.addToIndex(
                        exp.getId(),
                        exp.getUpdatedAt()
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                );

                log.info("Cached interview {} into Redis", exp.getId());

            } catch (Exception e) {

                log.error("Failed to cache {}", exp.getId(), e);
            }
        }

        log.info("Database returned {} interviews", res.size());

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

        log.info("InterviewExperienceServiceImpl :: getInterviewById :: checking cache :: id: {}", id);

        Object cachedObject = cacheService.get(id);

        if (cachedObject != null) {

            InterviewExperienceResponse cached =
                    objectMapper.convertValue(
                            cachedObject,
                            InterviewExperienceResponse.class
                    );

            log.info("CACHE HIT {}", id);
            return cached;
        }

        log.info("InterviewExperienceServiceImpl :: getInterviewById :: CACHE MISS :: fetching from DB :: id: {}", id);

        InterviewExperience experience = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("InterviewExperienceServiceImpl :: getInterviewById :: not found :: id: {}", id);
                    return new RuntimeException("Interview Experience not found");
                });

        InterviewExperienceResponse response =
                modelMapper.map(experience, InterviewExperienceResponse.class);

        log.info("InterviewExperienceServiceImpl :: getInterviewById :: saving to cache :: id: {}", id);

        try {
            String json = objectMapper.writeValueAsString(response);
            cacheService.save(id, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize interview response", e);
        }

        return response;
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

        cacheService.delete(id);
        cacheService.removeFromIndex(id);

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
        InterviewExperienceResponse response =
                modelMapper.map(updatedExperience, InterviewExperienceResponse.class);

// refresh cache
        try {
            String json = objectMapper.writeValueAsString(response);
            cacheService.save(updatedExperience.getId(), json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize interview response", e);
        }

        cacheService.addToIndex(
                updatedExperience.getId(),
                updatedExperience.getUpdatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );

        return response;
    }

}
