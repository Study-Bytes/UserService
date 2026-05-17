package org.studyplatform.userService.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.studyplatform.userService.entity.TeacherRequest;
import org.studyplatform.userService.entity.TeacherRequestStatus;
import org.studyplatform.userService.entity.User;

import java.util.Optional;

public interface TeacherRequestRepository extends JpaRepository<TeacherRequest, Long> {
    boolean existsByUserAndStatus(User user, TeacherRequestStatus status);
    Optional<TeacherRequest> findFirstByUserOrderByCreatedAtDesc(User user);
    Page<TeacherRequest> findByStatus(TeacherRequestStatus status, Pageable pageable);
}
