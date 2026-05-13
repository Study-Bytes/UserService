package org.studyplatform.userService.exception;

public class EmailAlreadyTakenException extends RuntimeException {
    public EmailAlreadyTakenException(String email) {
        super("User with email already exists: " + email);
    }
}