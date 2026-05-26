package org.example.util;

import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class AuthTokenParser {
    public static Long parseUserId(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.startsWith("Basic ")) {
            return null;
        }
        try {
            String encoded = authorization.substring(6);
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            String userIdStr = decoded.contains(":") ? decoded.substring(0, decoded.indexOf(':')) : decoded;
            return Long.parseLong(userIdStr.trim());
        } catch (Exception e) {
            return null;
        }
    }

}
