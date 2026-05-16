package org.studyplatform.userService.exception;

public class InvalidUserRoleException extends RuntimeException {
    public InvalidUserRoleException(String message) {
        super(message);
    }
}
