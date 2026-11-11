package pl.tools.intellij.timestamp;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class TemplateEngineTest {
    private static final Instant T = Instant.parse("2026-10-30T20:42:15.123Z");
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private String format(String input, String selection) {
        return TemplateEngine.generate(input, T, WARSAW, selection);
    }

    @Test void timestampsInLocalZoneAndUtc() {
        assertEquals("2026-10-30 21:42:15.123", format("${date:yyyy-MM-dd HH:mm:ss.SSS}", ""));
        assertEquals("2026-10-30T20:42:15Z", format("${utc:yyyy-MM-dd'T'HH:mm:ss'Z'}", ""));
        assertEquals("1793392935", format("${unix}", ""));
        assertEquals("1793392935123", format("${unix_ms}", ""));
    }

    @Test void uuidV4Formats() {
        assertTrue(format("${uuid}", "").matches("[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}"));
        assertTrue(format("${uuid32}", "").matches("[a-f0-9]{12}4[a-f0-9]{3}[89ab][a-f0-9]{15}"));
        assertTrue(format("${uuid_upper}", "").matches("[A-F0-9-]{36}"));
    }

    @Test void compositeAndSelection() {
        String result = format("TEST-${date:yyyyMMdd}-${rand:6}-${selection}", "original value");
        assertTrue(result.matches("TEST-20261030-[0-9]{6}-original value"), result);
        assertEquals("<x>hello</x>", format("<x>${selection}</x>", "hello"));
        assertEquals("x", format("${selection}x", ""));
    }

    @Test void escapeIsLiteral() {
        assertEquals("${uuid}", format("\\${uuid}", ""));
        assertEquals("AA${date:yyyy}BB", format("AA\\${date:yyyy}BB", ""));
    }

    @Test void rejectsInvalidTokensAndPatterns() {
        assertThrows(IllegalArgumentException.class, () -> TemplateEngine.validate("${unknown}"));
        assertThrows(IllegalArgumentException.class, () -> TemplateEngine.validate("${rand:0}"));
        assertThrows(IllegalArgumentException.class, () -> TemplateEngine.validate("${rand:65}"));
        assertThrows(IllegalArgumentException.class, () -> TemplateEngine.validate("${rand:abc}"));
        assertThrows(IllegalArgumentException.class, () -> TemplateEngine.validate("${date:foobarrr}"));
        assertThrows(IllegalArgumentException.class, () -> TemplateEngine.validate("${uuid"));
    }

    @Test void actualTimeIsCapturedByCaller() {
        String result = TemplateEngine.generate("${date:yyyy} ${unix}", T, ZoneOffset.UTC, "");
        assertEquals("2026 1793392935", result);
    }
}
