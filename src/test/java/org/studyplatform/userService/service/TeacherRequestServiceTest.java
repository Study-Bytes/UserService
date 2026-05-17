package org.studyplatform.userService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.studyplatform.userService.dto.TeacherRequestCreateRequest;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.TeacherRequest;
import org.studyplatform.userService.entity.TeacherRequestStatus;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.exception.ConflictException;
import org.studyplatform.userService.repository.TeacherRequestRepository;
import org.studyplatform.userService.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherRequestServiceTest {
    @Mock
    private TeacherRequestRepository teacherRequestRepository;

    @Mock
    private UserRepository userRepository;

    private TeacherRequestService teacherRequestService;

    @BeforeEach
    void setUp() {
        teacherRequestService = new TeacherRequestService(teacherRequestRepository, userRepository, new ObjectMapper());
    }

    @Test
    void create_ForStudent_ShouldCreatePendingRequest() {
        User user = user(1L, "student@example.com", Role.STUDENT);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(teacherRequestRepository.existsByUserAndStatus(user, TeacherRequestStatus.PENDING)).thenReturn(false);
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> {
            TeacherRequest request = invocation.getArgument(0);
            request.setId(10L);
            return request;
        });

        TeacherRequestCreateRequest request = createRequest();
        TeacherRequest result = teacherRequestService.create("student@example.com", request);

        assertEquals(10L, result.getId());
        assertEquals(user, result.getUser());
        assertEquals(TeacherRequestStatus.PENDING, result.getStatus());
        assertEquals("I want to create Java courses.", result.getMotivation());
        assertEquals("[\"Java\",\"Spring Boot\"]", result.getPreferredTopicsJson());
        assertNotNull(result.getCreatedAt());
        verify(teacherRequestRepository).save(any(TeacherRequest.class));
    }

    @Test
    void create_WithDuplicatePendingRequest_ShouldReturnConflict() {
        User user = user(1L, "student@example.com", Role.STUDENT);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(teacherRequestRepository.existsByUserAndStatus(user, TeacherRequestStatus.PENDING)).thenReturn(true);

        assertThrows(ConflictException.class, () -> teacherRequestService.create("student@example.com", createRequest()));

        verify(teacherRequestRepository, never()).save(any(TeacherRequest.class));
    }

    @Test
    void create_ForTeacherOrAdmin_ShouldReturnConflict() {
        when(userRepository.findByEmail("teacher@example.com")).thenReturn(Optional.of(user(2L, "teacher@example.com", Role.TEACHER)));

        assertThrows(ConflictException.class, () -> teacherRequestService.create("teacher@example.com", createRequest()));

        verify(teacherRequestRepository, never()).save(any(TeacherRequest.class));
    }

    @Test
    void approve_ShouldApproveRequestAndPromoteUserToTeacher() {
        User student = user(1L, "student@example.com", Role.STUDENT);
        User admin = user(99L, "admin@example.com", Role.ADMIN);
        TeacherRequest teacherRequest = teacherRequest(10L, student, TeacherRequestStatus.PENDING);
        when(teacherRequestRepository.findById(10L)).thenReturn(Optional.of(teacherRequest));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequest result = teacherRequestService.approve(10L, "admin@example.com");

        assertEquals(TeacherRequestStatus.APPROVED, result.getStatus());
        assertEquals(Role.TEACHER, student.getRole());
        assertEquals(admin, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
        verify(userRepository).save(student);
        verify(teacherRequestRepository).save(teacherRequest);
    }

    @Test
    void reject_ShouldSaveReviewComment() {
        User student = user(1L, "student@example.com", Role.STUDENT);
        User admin = user(99L, "admin@example.com", Role.ADMIN);
        TeacherRequest teacherRequest = teacherRequest(10L, student, TeacherRequestStatus.PENDING);
        when(teacherRequestRepository.findById(10L)).thenReturn(Optional.of(teacherRequest));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(teacherRequestRepository.save(any(TeacherRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRequest result = teacherRequestService.reject(10L, "admin@example.com", "Please add more details.");

        assertEquals(TeacherRequestStatus.REJECTED, result.getStatus());
        assertEquals("Please add more details.", result.getReviewComment());
        assertEquals(Role.STUDENT, student.getRole());
        assertEquals(admin, result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
    }

    private TeacherRequestCreateRequest createRequest() {
        TeacherRequestCreateRequest request = new TeacherRequestCreateRequest();
        request.setMotivation("I want to create Java courses.");
        request.setExperience("3 years of Java backend experience.");
        request.setPortfolioUrl("https://example.com");
        request.setPreferredTopics(List.of("Java", "Spring Boot"));
        return request;
    }

    private TeacherRequest teacherRequest(Long id, User user, TeacherRequestStatus status) {
        TeacherRequest request = new TeacherRequest();
        request.setId(id);
        request.setUser(user);
        request.setStatus(status);
        request.setMotivation("I want to create Java courses.");
        return request;
    }

    private User user(Long id, String email, Role role) {
        User user = new User(email, "encoded-password", role, "User Name");
        user.setId(id);
        return user;
    }
}
