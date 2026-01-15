package com.Switchboard.InterviewService.controller;

import com.Switchboard.InterviewService.config.AppConstants;
import com.Switchboard.InterviewService.dto.InterviewExperienceRequest;
import com.Switchboard.InterviewService.dto.InterviewExperienceResponse;
import com.Switchboard.InterviewService.dto.PageResponseDTO;
import com.Switchboard.InterviewService.service.FileService;
import com.Switchboard.InterviewService.service.InterviewExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Interview Experience", description = "APIs for managing interview experiences")
public class InterviewExperienceController {
    private static final Logger log = LoggerFactory.getLogger(InterviewExperienceController.class);

    private final InterviewExperienceService interviewService;
    private final FileService fileService;

    // Create
    @Operation(summary = "Create a new interview experience", description = "Creates a new interview experience with optional image upload")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created interview experience",
                    content = @Content(schema = @Schema(implementation = InterviewExperienceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or image format", content = @Content)
    })
    @PostMapping(value = "/interviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InterviewExperienceResponse> createInterviewExperience(
            @Valid @ModelAttribute InterviewExperienceRequest request,@RequestHeader("X-User-Email") String userEmailHeader) throws IOException {
        log.info("InterviewExperienceController :: createInterviewExperience :: starting request processing");

        try {
            // Handle image upload if provided
            String imageUrl = null;
            if (request.getImage() != null && !request.getImage().isEmpty()) {
                log.info("InterviewExperienceController :: createInterviewExperience :: processing image: {} of type: {}",
                        request.getImage().getOriginalFilename(), request.getImage().getContentType());

                imageUrl = fileService.uploadImage(AppConstants.PATH_VARIABLE, request.getImage());
            }
            request.setUserEmail(userEmailHeader);
            InterviewExperienceResponse response = interviewService.createInterviewExperience(request, imageUrl);
            log.info("InterviewExperienceController :: createInterviewExperience :: completed successfully");
            return ResponseEntity.ok(response);

        } catch (MultipartException e) {
            log.error("InterviewExperienceController :: createInterviewExperience :: multipart error: {}", e.getMessage());
            throw new RuntimeException("Error processing multipart request: " + e.getMessage());
        } catch (Exception e) {
            log.error("InterviewExperienceController :: createInterviewExperience :: error: {}", e.getMessage());
            throw e;
        }
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipartException(MultipartException e) {
        log.error("InterviewExperienceController :: handleMultipartException :: error handling multipart request: {}", e.getMessage());
        return ResponseEntity.badRequest().body("Error processing multipart request: Please ensure the request is properly formatted");
    }

    // Search by email
    @Operation(summary = "Search interviews by email", description = "Retrieves all interview experiences for a specific user email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved interviews",
                    content = @Content(schema = @Schema(implementation = InterviewExperienceResponse.class)))
    })
    @GetMapping("/search/email")
    public ResponseEntity<List<InterviewExperienceResponse>> searchByEmail(
            @Parameter(description = "User email to search for", required = true)
            @RequestParam String email) {
        log.info("InterviewExperienceController :: searchByEmail :: searching :: interviews for email: {}", email);
        List<InterviewExperienceResponse> response = interviewService.searchByEmail(email);
        log.info("InterviewExperienceController :: searchByEmail :: found :: {} interviews", response.size());
        return ResponseEntity.ok(response);
    }

    // Search by email header
    @Operation(summary = "Search interviews by email header", description = "Retrieves all interview experiences for a specific user email received from header")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved interviews",
                    content = @Content(schema = @Schema(implementation = InterviewExperienceResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<List<InterviewExperienceResponse>> searchByEmailHeader(
            @Parameter(description = "User email to search for", required = true)
            @RequestHeader("X-User-Email") String userEmailHeader) {
        log.info("InterviewExperienceController :: searchByEmail :: searching :: interviews for email: {}", userEmailHeader);
        List<InterviewExperienceResponse> response = interviewService.searchByEmail(userEmailHeader);
        log.info("InterviewExperienceController :: searchByEmail :: found :: {} interviews", response.size());
        return ResponseEntity.ok(response);
    }

    // Search by company
    @Operation(summary = "Search interviews by company", description = "Retrieves all interview experiences for a specific company")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved interviews",
                    content = @Content(schema = @Schema(implementation = InterviewExperienceResponse.class)))
    })
    @GetMapping("/search/company")
    public ResponseEntity<List<InterviewExperienceResponse>> searchByCompany(
            @Parameter(description = "Company name to search for", required = true)
            @RequestParam String company) {
        log.info("InterviewExperienceController :: searchByCompany :: searching :: interviews for company: {}", company);
        List<InterviewExperienceResponse> response = interviewService.searchByCompany(company);
        log.info("InterviewExperienceController :: searchByCompany :: found :: {} interviews", response.size());
        return ResponseEntity.ok(interviewService.searchByCompany(company));
    }

    // Get all interviews
    @Operation(summary = "Get all interviews", description = "Retrieves all interview experiences with pagination and sorting")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved interviews",
                    content = @Content(schema = @Schema(implementation = PageResponseDTO.class)))
    })
    @GetMapping("/getAll/interviews")
    public ResponseEntity<PageResponseDTO> getAllInterviews(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(value = "pageNumber" , defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @Parameter(description = "Number of items per page")
            @RequestParam(value = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize ,
            @Parameter(description = "Field to sort by")
            @RequestParam(value = "sortBy" , defaultValue = AppConstants.SORT_BY, required = false) String sortBy,
            @Parameter(description = "Sort direction (asc/desc)")
            @RequestParam(value = "sortDir", defaultValue = AppConstants.SORT_DIR, required = false) String sortDir
    ) {
        log.info("InterviewExperienceController :: getAllInterviews :: fetching :: all interviews");
        PageResponseDTO response = interviewService.getAllInterviews(pageNumber, pageSize, sortBy, sortDir);
        log.info("InterviewExperienceController :: getAllInterviews :: fetched :: {} interviews", response.getContent().size());
        return ResponseEntity.ok(response);
    }

    // Get an interview by ID
    @Operation(summary = "Get interview by ID", description = "Retrieves a specific interview experience by its UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved interview",
                    content = @Content(schema = @Schema(implementation = InterviewExperienceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Interview not found", content = @Content)
    })
    @GetMapping("get/{id}")
    public ResponseEntity<InterviewExperienceResponse> getInterviewById(
            @Parameter(description = "Interview UUID", required = true)
            @PathVariable UUID id) {
        log.info("InterviewExperienceController :: getInterviewById :: fetching :: interview with id: {}", id);
        InterviewExperienceResponse response = interviewService.getInterviewById(id);
        log.info("InterviewExperienceController :: getInterviewById :: fetched :: interview with id: {}", id);
        return ResponseEntity.ok(response);
    }

    // Update an interview
    @Operation(summary = "Update an interview experience", description = "Updates an existing interview experience with optional new image")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated interview",
                    content = @Content(schema = @Schema(implementation = InterviewExperienceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Interview not found", content = @Content)
    })
    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InterviewExperienceResponse> updateInterviewExperience(
            @Parameter(description = "Interview UUID", required = true)
            @PathVariable UUID id,
            @Valid @ModelAttribute InterviewExperienceRequest request) throws IOException {
        log.info("InterviewExperienceController :: updateInterviewExperience :: updating :: interview experience with id: {}", id);

        try {
            InterviewExperienceResponse response = interviewService.updateInterviewExperience(
                    id, request, request.getImage());
            log.info("InterviewExperienceController :: updateInterviewExperience :: updated :: interview experience with id: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("InterviewExperienceController :: updateInterviewExperience :: error :: {}", e.getMessage());
            throw e;
        }
    }

    // Delete an interview
    @Operation(summary = "Delete an interview experience", description = "Deletes an interview experience and its associated image from S3")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted interview"),
            @ApiResponse(responseCode = "404", description = "Interview not found", content = @Content)
    })
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteInterviewExperience(
            @Parameter(description = "Interview UUID", required = true)
            @PathVariable UUID id) {
        log.info("InterviewExperienceController :: deleteInterviewExperience :: deleting :: interview experience with id: {}", id);
        interviewService.deleteInterviewExperience(id);
        log.info("InterviewExperienceController :: deleteInterviewExperience :: deleted :: interview experience with id: {}", id);
        return ResponseEntity.ok("Interview experience deleted successfully, image removed from S3");
    }
}
