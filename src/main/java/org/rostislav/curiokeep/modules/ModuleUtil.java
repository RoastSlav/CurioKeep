package org.rostislav.curiokeep.modules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public class ModuleUtil {
    private ModuleUtil() {
        // static class
    }

    public static String readUtf8(Resource r) throws Exception {
        try (var in = r.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void ensureUnique(java.util.List<String> values, String message) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String v : values) {
            if (!seen.add(v)) {
                throw new IllegalStateException(message + " (duplicate: " + v + ")");
            }
        }
    }

    public static PGobject jsonb(String json) {
        if (json == null) return null;
        try {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(json);
            return pg;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create jsonb PGobject", e);
        }
    }

    public String toJson(Object value, ObjectMapper objectMapper) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }
}
