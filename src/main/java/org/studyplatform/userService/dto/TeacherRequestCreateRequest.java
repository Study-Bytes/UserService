package org.studyplatform.userService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class TeacherRequestCreateRequest {
    @NotBlank
    private String motivation;

    private String experience;

    @Size(max = 1000)
    private String portfolioUrl;

    private List<String> preferredTopics;

    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
    public List<String> getPreferredTopics() { return preferredTopics; }
    public void setPreferredTopics(List<String> preferredTopics) { this.preferredTopics = preferredTopics; }
}
