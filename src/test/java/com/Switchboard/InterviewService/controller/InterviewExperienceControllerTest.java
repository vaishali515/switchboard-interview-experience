package com.Switchboard.InterviewService.controller;

import com.Switchboard.InterviewService.dto.InterviewExperienceRequest;
import com.Switchboard.InterviewService.dto.InterviewExperienceResponse;
import com.Switchboard.InterviewService.dto.PageResponseDTO;
import com.Switchboard.InterviewService.service.FileService;
import com.Switchboard.InterviewService.service.InterviewExperienceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewExperienceControllerTest {

    @Mock
    private InterviewExperienceService interviewService;

    @Mock
    private FileService fileService;

    @InjectMocks
    private InterviewExperienceController controller;

    private InterviewExperienceRequest request;
    private InterviewExperienceResponse response;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        
        request = InterviewExperienceRequest.builder()
                .userName("John Doe")
                .userEmail("john.doe@example.com")
                .title("Amazing Interview Experience at Google")
                .content("This was a great interview experience with multiple rounds...")
                .companyTag("Google")
                .build();

        response = InterviewExperienceResponse.builder()
                .id(testId)
                .userName("John Doe")
                .userEmail("john.doe@example.com")
                .title("Amazing Interview Experience at Google")
                .content("This was a great interview experience with multiple rounds...")
                .companyTag("Google")
                .imageName("https://s3.amazonaws.com/bucket/image.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createInterviewExperience_WithoutImage_ShouldReturnCreatedResponse() throws IOException {
        // Arrange
        when(interviewService.createInterviewExperience(any(InterviewExperienceRequest.class), isNull()))
                .thenReturn(response);

        // Act
        ResponseEntity<InterviewExperienceResponse> result = controller.createInterviewExperience(request, "john.doe@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(interviewService, times(1)).createInterviewExperience(any(InterviewExperienceRequest.class), isNull());
        verify(fileService, never()).uploadImage(anyString(), any());
    }

    @Test
    void createInterviewExperience_WithImage_ShouldUploadImageAndReturnResponse() throws IOException {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
                "image",
                "test-image.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        request.setImage(mockFile);
        String imageUrl = "https://s3.amazonaws.com/bucket/test-image.jpg";

        when(fileService.uploadImage(anyString(), any())).thenReturn(imageUrl);
        when(interviewService.createInterviewExperience(any(InterviewExperienceRequest.class), eq(imageUrl)))
                .thenReturn(response);

        // Act
        ResponseEntity<InterviewExperienceResponse> result = controller.createInterviewExperience(request, "john.doe@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(fileService, times(1)).uploadImage(anyString(), any());
        verify(interviewService, times(1)).createInterviewExperience(any(InterviewExperienceRequest.class), eq(imageUrl));
    }

    @Test
    void createInterviewExperience_WithEmptyImage_ShouldNotUploadImage() throws IOException {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile("image", "", "image/jpeg", new byte[0]);
        request.setImage(emptyFile);

        when(interviewService.createInterviewExperience(any(InterviewExperienceRequest.class), isNull()))
                .thenReturn(response);

        // Act
        ResponseEntity<InterviewExperienceResponse> result = controller.createInterviewExperience(request, "john.doe@example.com");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(fileService, never()).uploadImage(anyString(), any());
        verify(interviewService, times(1)).createInterviewExperience(any(InterviewExperienceRequest.class), isNull());
    }

    @Test
    void createInterviewExperience_WithException_ShouldThrowException() throws IOException {
        // Arrange
        when(interviewService.createInterviewExperience(any(InterviewExperienceRequest.class), isNull()))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            controller.createInterviewExperience(request, "john.doe@example.com");
        });
    }

    @Test
    void handleMultipartException_ShouldReturnBadRequest() {
        // Arrange
        MultipartException exception = new MultipartException("Invalid multipart request");

        // Act
        ResponseEntity<String> result = controller.handleMultipartException(exception);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertTrue(result.getBody().contains("Error processing multipart request"));
    }

    @Test
    void searchByEmail_ShouldReturnListOfExperiences() {
        // Arrange
        String email = "john.doe@example.com";
        List<InterviewExperienceResponse> expectedList = Arrays.asList(response);
        when(interviewService.searchByEmail(email)).thenReturn(expectedList);

        // Act
        ResponseEntity<List<InterviewExperienceResponse>> result = controller.searchByEmail(email);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(expectedList, result.getBody());
        assertEquals(1, result.getBody().size());
        verify(interviewService, times(1)).searchByEmail(email);
    }

    @Test
    void searchByEmailHeader_ShouldReturnListOfExperiences() {
        // Arrange
        String email = "john.doe@example.com";
        List<InterviewExperienceResponse> expectedList = Arrays.asList(response);
        when(interviewService.searchByEmail(email)).thenReturn(expectedList);

        // Act
        ResponseEntity<List<InterviewExperienceResponse>> result = controller.searchByEmailHeader(email);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(expectedList, result.getBody());
        verify(interviewService, times(1)).searchByEmail(email);
    }
    @Test
    void searchByCompany_ShouldReturnPagedExperiences() {

        String company = "Google";

        PageResponseDTO pageResponse = PageResponseDTO.builder()
                .content(Arrays.asList(response))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .lastPage(true)
                .build();

        when(interviewService.searchByCompany(company, 0, 10, "updatedAt", "asc"))
                .thenReturn(pageResponse);

        ResponseEntity<PageResponseDTO> result =
                controller.searchByCompany(company, 0, 10, "updatedAt", "asc");

        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(pageResponse, result.getBody());
        assertEquals(1, result.getBody().getContent().size());

        verify(interviewService, times(1))
                .searchByCompany(company, 0, 10, "updatedAt", "asc");
    }

    @Test
    void getAllInterviews_WithDefaultParameters_ShouldReturnPagedResponse() {
        // Arrange
        PageResponseDTO pageResponse = PageResponseDTO.builder()
                .content(Arrays.asList(response))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .lastPage(true)
                .build();

        when(interviewService.getAllInterviews(0, 10, "updatedAt", "asc")).thenReturn(pageResponse);

        // Act
        ResponseEntity<PageResponseDTO> result = controller.getAllInterviews(0, 10, "updatedAt", "asc");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(pageResponse, result.getBody());
        assertEquals(1, result.getBody().getContent().size());
        verify(interviewService, times(1)).getAllInterviews(0, 10, "updatedAt", "asc");
    }

    @Test
    void getAllInterviews_WithCustomParameters_ShouldReturnPagedResponse() {
        // Arrange
        PageResponseDTO pageResponse = PageResponseDTO.builder()
                .content(Arrays.asList(response))
                .pageNumber(1)
                .pageSize(20)
                .totalElements(25)
                .totalPages(2)
                .lastPage(false)
                .build();

        when(interviewService.getAllInterviews(1, 20, "createdAt", "desc")).thenReturn(pageResponse);

        // Act
        ResponseEntity<PageResponseDTO> result = controller.getAllInterviews(1, 20, "createdAt", "desc");

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(pageResponse, result.getBody());
        assertEquals(1, result.getBody().getPageNumber());
        verify(interviewService, times(1)).getAllInterviews(1, 20, "createdAt", "desc");
    }

    @Test
    void getInterviewById_ShouldReturnExperience() {
        // Arrange
        when(interviewService.getInterviewById(testId)).thenReturn(response);

        // Act
        ResponseEntity<InterviewExperienceResponse> result = controller.getInterviewById(testId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        assertEquals(testId, result.getBody().getId());
        verify(interviewService, times(1)).getInterviewById(testId);
    }

    @Test
    void getInterviewById_NotFound_ShouldThrowException() {
        // Arrange
        when(interviewService.getInterviewById(testId))
                .thenThrow(new RuntimeException("Interview Experience not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            controller.getInterviewById(testId);
        });
        verify(interviewService, times(1)).getInterviewById(testId);
    }

    @Test
    void updateInterviewExperience_WithoutImage_ShouldReturnUpdatedResponse() throws IOException {
        // Arrange
        when(interviewService.updateInterviewExperience(eq(testId), any(InterviewExperienceRequest.class), isNull()))
                .thenReturn(response);

        // Act
        ResponseEntity<InterviewExperienceResponse> result = controller.updateInterviewExperience(testId, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(interviewService, times(1)).updateInterviewExperience(eq(testId), any(InterviewExperienceRequest.class), isNull());
    }

    @Test
    void updateInterviewExperience_WithNewImage_ShouldReturnUpdatedResponse() throws IOException {
        // Arrange
        MockMultipartFile mockFile = new MockMultipartFile(
                "image",
                "updated-image.jpg",
                "image/jpeg",
                "updated image content".getBytes()
        );
        request.setImage(mockFile);

        when(interviewService.updateInterviewExperience(eq(testId), any(InterviewExperienceRequest.class), any()))
                .thenReturn(response);

        // Act
        ResponseEntity<InterviewExperienceResponse> result = controller.updateInterviewExperience(testId, request);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
        verify(interviewService, times(1)).updateInterviewExperience(eq(testId), any(InterviewExperienceRequest.class), any());
    }

    @Test
    void updateInterviewExperience_WithException_ShouldThrowException() throws IOException {
        // Arrange
        when(interviewService.updateInterviewExperience(eq(testId), any(InterviewExperienceRequest.class), isNull()))
                .thenThrow(new RuntimeException("Update failed"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            controller.updateInterviewExperience(testId, request);
        });
    }

    @Test
    void deleteInterviewExperience_ShouldReturnSuccessMessage() {
        // Arrange
        doNothing().when(interviewService).deleteInterviewExperience(testId);

        // Act
        ResponseEntity<String> result = controller.deleteInterviewExperience(testId);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertTrue(result.getBody().contains("deleted successfully"));
        verify(interviewService, times(1)).deleteInterviewExperience(testId);
    }

    @Test
    void deleteInterviewExperience_NotFound_ShouldThrowException() {
        // Arrange
        doThrow(new RuntimeException("Interview Experience not found"))
                .when(interviewService).deleteInterviewExperience(testId);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            controller.deleteInterviewExperience(testId);
        });
        verify(interviewService, times(1)).deleteInterviewExperience(testId);
    }
}
