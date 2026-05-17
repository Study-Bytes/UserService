package org.studyplatform.userService.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.studyplatform.userService.dto.TeacherRequestCreateRequest;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.TeacherRequest;
import org.studyplatform.userService.entity.TeacherRequestStatus;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.exception.ConflictException;
import org.studyplatform.userService.exception.UserNotFoundException;
import org.studyplatform.userService.repository.TeacherRequestRepository;
import org.studyplatform.userService.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TeacherRequestService {
    private final TeacherRequestRepository teacherRequestRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public TeacherRequestService(
            TeacherRequestRepository teacherRequestRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.teacherRequestRepository = teacherRequestRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TeacherRequest create(String email, TeacherRequestCreateRequest request) {
        User user = findUser(email);
        if (user.getRole() == Role.TEACHER || user.getRole() == Role.ADMIN) {
            throw new ConflictException("Teacher or admin users cannot create teacher requests");
        }
        if (teacherRequestRepository.existsByUserAndStatus(user, TeacherRequestStatus.PENDING)) {
            throw new ConflictException("Pending teacher request already exists");
        }

        TeacherRequest teacherRequest = new TeacherRequest();
        teacherRequest.setUser(user);
        teacherRequest.setStatus(TeacherRequestStatus.PENDING);
        teacherRequest.setMotivation(request.getMotivation());
        teacherRequest.setExperience(request.getExperience());
        teacherRequest.setPortfolioUrl(request.getPortfolioUrl());
        teacherRequest.setPreferredTopicsJson(writeTopics(request.getPreferredTopics()));
        teacherRequest.setCreatedAt(Instant.now());
        return teacherRequestRepository.save(teacherRequest);
    }

    @Transactional(readOnly = true)
    public Optional<TeacherRequest> findCurrent(String email) {
        return teacherRequestRepository.findFirstByUserOrderByCreatedAtDesc(findUser(email));
    }

    @Transactional(readOnly = true)
    public Page<TeacherRequest> list(TeacherRequestStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return status == null ? teacherRequestRepository.findAll(pageable) : teacherRequestRepository.findByStatus(status, pageable);
    }

    @Transactional
    public TeacherRequest approve(Long requestId, String adminEmail) {
        TeacherRequest request = findRequest(requestId);
        User admin = findUser(adminEmail);
        request.setStatus(TeacherRequestStatus.APPROVED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(admin);
        request.setReviewComment(null);
        request.getUser().setRole(Role.TEACHER);
        userRepository.save(request.getUser());
        return teacherRequestRepository.save(request);
    }

    @Transactional
    public TeacherRequest reject(Long requestId, String adminEmail, String reviewComment) {
        TeacherRequest request = findRequest(requestId);
        User admin = findUser(adminEmail);
        request.setStatus(TeacherRequestStatus.REJECTED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(admin);
        request.setReviewComment(reviewComment);
        return teacherRequestRepository.save(request);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private TeacherRequest findRequest(Long requestId) {
        return teacherRequestRepository.findById(requestId).orElseThrow(() -> new UserNotFoundException("Teacher request not found"));
    }

    private String writeTopics(List<String> topics) {
        try {
            return objectMapper.writeValueAsString(topics == null ? List.of() : topics);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid preferred topics");
        }
    }
}
