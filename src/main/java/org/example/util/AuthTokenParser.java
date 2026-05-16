package org.example.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class AuthTokenParser {

    private AuthTokenParser() {
    }

    public static Long parseUserId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String token = authorization.trim();
        if (token.startsWith("simple:token:")) {
            try {
                return Long.parseLong(token.substring("simple:token:".length()));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (token.startsWith("simple:")) {
            String[] parts = token.split(":");
            if (parts.length >= 3) {
                try {
                    return Long.parseLong(parts[parts.length - 1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public static String parseBasicUsername(String authorization) {
        if (authorization == null || !authorization.startsWith("Basic ")) {
            return null;
        }
        try {
            String encoded = authorization.substring(6);
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            return colon > 0 ? decoded.substring(0, colon) : decoded;
        } catch (Exception e) {
            return null;
        }
    }
}
