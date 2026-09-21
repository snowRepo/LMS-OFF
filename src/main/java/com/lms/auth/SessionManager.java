package com.lms.auth;

import com.lms.model.User;

/**
 * Global session manager to hold the currently authenticated user.
 */
public class SessionManager {

    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clearSession() {
        currentUser = null;
    }
}
