package org.studyplatform.userService.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.studyplatform.userService.entity.TeacherRequest;

import java.time.Instant;
import java.util.List;

public class TeacherRequestResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String userFullName;
    private String status;
    private String motivation;
    private String experience;
    private String portfolioUrl;
    private List<String> preferredTopics;
    private String reviewComment;
    private Instant createdAt;
    private Instant reviewedAt;
    private Long reviewedByUserId;

    public static TeacherRequestResponse from(TeacherRequest request, ObjectMapper objectMapper, boolean includeUser) {
        TeacherRequestResponse response = new TeacherRequestResponse();
        response.id = request.getId();
        response.userId = request.getUser().getId();
        if (includeUser) {
            response.userEmail = request.getUser().getEmail();
            response.userFullName = request.getUser().getFullName();
        }
        response.status = request.getStatus().name();
        response.motivation = request.getMotivation();
        response.experience = request.getExperience();
        response.portfolioUrl = request.getPortfolioUrl();
        response.preferredTopics = readTopics(request.getPreferredTopicsJson(), objectMapper);
        response.reviewComment = request.getReviewComment();
        response.createdAt = request.getCreatedAt();
        response.reviewedAt = request.getReviewedAt();
        response.reviewedByUserId = request.getReviewedBy() == null ? null : request.getReviewedBy().getId();
        return response;
    }

    private static List<String> readTopics(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public String getUserFullName() { return userFullName; }
    public String getStatus() { return status; }
    public String getMotivation() { return motivation; }
    public String getExperience() { return experience; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public List<String> getPreferredTopics() { return preferredTopics; }
    public String getReviewComment() { return reviewComment; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Long getReviewedByUserId() { return reviewedByUserId; }
}
