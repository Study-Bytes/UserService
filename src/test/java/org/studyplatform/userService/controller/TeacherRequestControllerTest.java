package org.studyplatform.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.studyplatform.userService.dto.TeacherRequestCreateRequest;
import org.studyplatform.userService.dto.TeacherRequestRejectRequest;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.TeacherRequest;
import org.studyplatform.userService.entity.TeacherRequestStatus;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.service.TeacherRequestService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeacherRequestControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TeacherRequestService teacherRequestService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TeacherRequestController(teacherRequestService, objectMapper))
                .setControllerAdvice(new RestExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void create_WhenAuthenticated_ShouldReturnCreatedTeacherRequest() throws Exception {
        TeacherRequestCreateRequest request = createRequest();
        when(teacherRequestService.create(org.mockito.Mockito.eq("student@example.com"), any(TeacherRequestCreateRequest.class)))
                .thenReturn(teacherRequest(10L, TeacherRequestStatus.PENDING));

        mockMvc.perform(post("/api/v1/teacher-requests")
                        .principal(new TestingAuthenticationToken("student@example.com", null))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.preferredTopics[0]").value("Java"));
    }

    @Test
    void me_WhenRequestExists_ShouldReturnCurrentTeacherRequest() throws Exception {
        when(teacherRequestService.findCurrent("student@example.com"))
                .thenReturn(Optional.of(teacherRequest(10L, TeacherRequestStatus.PENDING)));

        mockMvc.perform(get("/api/v1/teacher-requests/me")
                        .principal(new TestingAuthenticationToken("student@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void me_WhenNoRequestExists_ShouldReturnNoContent() throws Exception {
        when(teacherRequestService.findCurrent("student@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/teacher-requests/me")
                        .principal(new TestingAuthenticationToken("student@example.com", null)))
                .andExpect(status().isNoContent());
    }

    @Test
    void list_ShouldReturnPagedTeacherRequests() throws Exception {
        TeacherRequest request = teacherRequest(10L, TeacherRequestStatus.PENDING);
        when(teacherRequestService.list(TeacherRequestStatus.PENDING, 0, 20))
                .thenReturn(new PageImpl<>(List.of(request), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/teacher-requests")
                        .principal(new TestingAuthenticationToken("admin@example.com", null, "ROLE_ADMIN"))
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(10))
                .andExpect(jsonPath("$.items[0].userEmail").value("student@example.com"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void approve_ShouldReturnApprovedTeacherRequest() throws Exception {
        when(teacherRequestService.approve(10L, "admin@example.com"))
                .thenReturn(teacherRequest(10L, TeacherRequestStatus.APPROVED));

        mockMvc.perform(post("/api/v1/admin/teacher-requests/{requestId}/approve", 10L)
                        .principal(new TestingAuthenticationToken("admin@example.com", null, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void reject_ShouldReturnRejectedTeacherRequest() throws Exception {
        TeacherRequestRejectRequest request = new TeacherRequestRejectRequest();
        request.setReviewComment("Please add more details.");
        TeacherRequest rejected = teacherRequest(10L, TeacherRequestStatus.REJECTED);
        rejected.setReviewComment("Please add more details.");
        when(teacherRequestService.reject(10L, "admin@example.com", "Please add more details."))
                .thenReturn(rejected);

        mockMvc.perform(post("/api/v1/admin/teacher-requests/{requestId}/reject", 10L)
                        .principal(new TestingAuthenticationToken("admin@example.com", null, "ROLE_ADMIN"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewComment").value("Please add more details."));
    }

    @Test
    void adminEndpoints_ShouldBeRestrictedToAdminUsers() throws Exception {
        var list = TeacherRequestController.class.getMethod("list", org.studyplatform.userService.entity.TeacherRequestStatus.class, int.class, int.class);
        var approve = TeacherRequestController.class.getMethod("approve", Long.class, org.springframework.security.core.Authentication.class);
        var reject = TeacherRequestController.class.getMethod("reject", Long.class, org.springframework.security.core.Authentication.class, TeacherRequestRejectRequest.class);

        assertAdminOnly(list.getAnnotation(PreAuthorize.class));
        assertAdminOnly(approve.getAnnotation(PreAuthorize.class));
        assertAdminOnly(reject.getAnnotation(PreAuthorize.class));
    }

    private void assertAdminOnly(PreAuthorize preAuthorize) {
        assertNotNull(preAuthorize);
        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    private TeacherRequestCreateRequest createRequest() {
        TeacherRequestCreateRequest request = new TeacherRequestCreateRequest();
        request.setMotivation("I want to create Java courses.");
        request.setExperience("3 years of Java backend experience.");
        request.setPortfolioUrl("https://example.com");
        request.setPreferredTopics(List.of("Java", "Spring Boot"));
        return request;
    }

    private TeacherRequest teacherRequest(Long id, TeacherRequestStatus status) {
        User user = new User("student@example.com", "encoded-password", Role.STUDENT, "Student User");
        user.setId(1L);

        TeacherRequest request = new TeacherRequest();
        request.setId(id);
        request.setUser(user);
        request.setStatus(status);
        request.setMotivation("I want to create Java courses.");
        request.setExperience("3 years of Java backend experience.");
        request.setPortfolioUrl("https://example.com");
        request.setPreferredTopicsJson("[\"Java\",\"Spring Boot\"]");
        request.setCreatedAt(Instant.parse("2026-05-17T12:00:00Z"));
        return request;
    }
}
