package io.oneqa.agent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final JdbcTemplate jdbc;

    public AgentService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public Registration register(UUID organizationId, String name, List<String> capabilities) {
        if (name == null || name.isBlank() || name.trim().length() > 160)
            throw new IllegalArgumentException("Agent name must be 1..160 characters");
        String normalized = encodeCapabilities(capabilities);
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO agents(id, organization_id, name, capabilities, token_hash)
            VALUES (?, ?, ?, ?, ?)
            """, id, organizationId, name.trim(), normalized, sha256(token));
        return new Registration(id, token);
    }

    @Transactional(readOnly = true)
    public List<AgentView> list(UUID organizationId) {
        return jdbc.query("""
            SELECT id, name, capabilities, registered_at, last_seen_at, revoked_at
            FROM agents WHERE organization_id = ? ORDER BY registered_at DESC, id DESC
            """, (rs, row) -> new AgentView(rs.getObject("id", UUID.class),
                rs.getString("name"), decodeCapabilities(rs.getString("capabilities")),
                rs.getObject("registered_at", OffsetDateTime.class),
                rs.getObject("last_seen_at", OffsetDateTime.class),
                rs.getObject("revoked_at", OffsetDateTime.class)), organizationId);
    }

    @Transactional
    public boolean heartbeat(UUID agentId, String bearerToken) {
        if (agentId == null || bearerToken == null || bearerToken.isBlank()) return false;
        return jdbc.update("""
            UPDATE agents SET last_seen_at = now()
            WHERE id = ? AND token_hash = ? AND revoked_at IS NULL
            """, agentId, sha256(bearerToken)) == 1;
    }

    @Transactional
    public boolean revoke(UUID organizationId, UUID agentId) {
        return jdbc.update("""
            UPDATE agents SET revoked_at = now() WHERE organization_id = ?
            AND id = ? AND revoked_at IS NULL
            """, organizationId, agentId) == 1;
    }

    private static String encodeCapabilities(List<String> capabilities) {
        if (capabilities == null) throw new IllegalArgumentException("Capabilities required");
        if (capabilities.size() > 32) throw new IllegalArgumentException("Too many capabilities");
        for (String capability : capabilities) {
            if (capability == null || !capability.matches("[a-zA-Z0-9._-]{1,64}"))
                throw new IllegalArgumentException("Invalid capability");
        }
        return String.join(",", capabilities.stream().distinct().sorted().toList());
    }

    private static List<String> decodeCapabilities(String encoded) {
        return encoded.isEmpty() ? List.of() : List.of(encoded.split(","));
    }

    private static String sha256(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }

    public record Registration(UUID id, String token) {}
    public record AgentView(UUID id, String name, List<String> capabilities,
                            OffsetDateTime registeredAt, OffsetDateTime lastSeenAt,
                            OffsetDateTime revokedAt) {}
}
