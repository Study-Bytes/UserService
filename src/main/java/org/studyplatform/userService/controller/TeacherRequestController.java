package org.studyplatform.userService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.studyplatform.userService.dto.TeacherRequestCreateRequest;
import org.studyplatform.userService.dto.TeacherRequestPageResponse;
import org.studyplatform.userService.dto.TeacherRequestRejectRequest;
import org.studyplatform.userService.dto.TeacherRequestResponse;
import org.studyplatform.userService.entity.TeacherRequest;
import org.studyplatform.userService.entity.TeacherRequestStatus;
import org.studyplatform.userService.service.TeacherRequestService;

import java.util.List;

@RestController
public class TeacherRequestController {
    private final TeacherRequestService teacherRequestService;
    private final ObjectMapper objectMapper;

    public TeacherRequestController(TeacherRequestService teacherRequestService, ObjectMapper objectMapper) {
        this.teacherRequestService = teacherRequestService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/v1/teacher-requests")
    public ResponseEntity<TeacherRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody TeacherRequestCreateRequest request
    ) {
        TeacherRequest created = teacherRequestService.create(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TeacherRequestResponse.from(created, objectMapper, false));
    }

    @GetMapping("/api/v1/teacher-requests/me")
    public ResponseEntity<TeacherRequestResponse> me(Authentication authentication) {
        return teacherRequestService.findCurrent(authentication.getName())
                .map(request -> ResponseEntity.ok(TeacherRequestResponse.from(request, objectMapper, false)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/api/v1/admin/teacher-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherRequestPageResponse list(
            @RequestParam(required = false) TeacherRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<TeacherRequest> requests = teacherRequestService.list(status, page, size);
        List<TeacherRequestResponse> items = requests.getContent().stream()
                .map(request -> TeacherRequestResponse.from(request, objectMapper, true))
                .toList();
        return new TeacherRequestPageResponse(items, requests.getNumber(), requests.getSize(), requests.getTotalElements(), requests.getTotalPages());
    }

    @PostMapping("/api/v1/admin/teacher-requests/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherRequestResponse approve(@PathVariable Long requestId, Authentication authentication) {
        return TeacherRequestResponse.from(teacherRequestService.approve(requestId, authentication.getName()), objectMapper, false);
    }

    @PostMapping("/api/v1/admin/teacher-requests/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public TeacherRequestResponse reject(
            @PathVariable Long requestId,
            Authentication authentication,
            @Valid @RequestBody TeacherRequestRejectRequest request
    ) {
        return TeacherRequestResponse.from(
                teacherRequestService.reject(requestId, authentication.getName(), request.getReviewComment()),
                objectMapper,
                false
        );
    }
}
