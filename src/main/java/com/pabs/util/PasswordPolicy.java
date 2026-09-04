package com.pabs.util;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    public static String validateNewPassword(String password, String confirmPassword) {
        if (isBlank(password) || isBlank(confirmPassword)) {
            return "Password and confirmation are required.";
        }
        if (!password.equals(confirmPassword)) {
            return "Password and confirmation do not match.";
        }
        if (password.length() < MIN_LENGTH) {
            return "Password must be at least " + MIN_LENGTH + " characters long.";
        }
        if (password.length() > MAX_LENGTH) {
            return "Password must be " + MAX_LENGTH + " characters or fewer.";
        }
        if (password.chars().allMatch(Character::isWhitespace)) {
            return "Password cannot contain only whitespace.";
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
