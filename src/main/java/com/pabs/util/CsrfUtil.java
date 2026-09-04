package com.pabs.util;

import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class CsrfUtil {

    public static final String SESSION_ATTRIBUTE = "csrfToken";
    public static final String PARAMETER_NAME = "csrfToken";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CsrfUtil() {
    }

    public static String getToken(HttpSession session) {
        if (session == null) {
            return "";
        }
        Object existing = session.getAttribute(SESSION_ATTRIBUTE);
        if (existing instanceof String && !((String) existing).isBlank()) {
            return (String) existing;
        }
        String token = newToken();
        session.setAttribute(SESSION_ATTRIBUTE, token);
        return token;
    }

    public static void rotateToken(HttpSession session) {
        if (session != null) {
            session.setAttribute(SESSION_ATTRIBUTE, newToken());
        }
    }

    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_ATTRIBUTE);
        String provided = request.getParameter(PARAMETER_NAME);
        return expected instanceof String
                && provided != null
                && constantTimeEquals((String) expected, provided);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String first, String second) {
        byte[] a = first.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = second.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
