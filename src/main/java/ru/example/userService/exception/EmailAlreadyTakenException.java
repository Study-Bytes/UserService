package ru.example.userService.exception;

public class EmailAlreadyTakenException extends RuntimeException {
    public EmailAlreadyTakenException(String email) {
        super("User with email already exists: " + email);
    }
}