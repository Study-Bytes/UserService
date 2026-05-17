package org.studyplatform.userService.dto;

import jakarta.validation.constraints.NotBlank;

public class TeacherRequestRejectRequest {
    @NotBlank
    private String reviewComment;

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
}
