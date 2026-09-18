package com.lms.model;

/**
 * Represents a system user (Admin or Librarian).
 */
public record User(
        int id,
        String fullName,
        String username,
        String role,   // "ADMIN" or "LIBRARIAN"
        boolean mustChangePassword
) {}
