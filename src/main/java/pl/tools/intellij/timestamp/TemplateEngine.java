package pl.tools.intellij.timestamp;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

/**
 * A small, intentionally non-executable template language.
 * Supported: ${uuid}, ${uuid32}, ${uuid_upper}, ${unix}, ${unix_ms},
 * ${date:pattern}, ${utc:pattern}, ${rand:N}, ${selection}.
 * Escape a placeholder with a leading backslash (\${uuid}).
 */
public final class TemplateEngine {
    private static final SecureRandom RANDOM = new SecureRandom();
    private TemplateEngine() { }

    public static String generate(String template, Instant instant, ZoneId zone, String selectedText) {
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(selectedText, "selectedText");
        StringBuilder output = new StringBuilder();
        for (int pos = 0; pos < template.length();) {
            if (template.charAt(pos) == '\\' && template.startsWith("${", pos + 1)) {
                output.append("${");
                pos += 3;
                continue;
            }
            if (template.startsWith("${", pos)) {
                int end = template.indexOf('}', pos + 2);
                if (end < 0) throw new IllegalArgumentException("Unclosed placeholder at character " + (pos + 1));
                String token = template.substring(pos + 2, end);
                output.append(resolve(token, instant, zone, selectedText));
                pos = end + 1;
                continue;
            }
            output.append(template.charAt(pos));
            pos++;
        }
        return output.toString();
    }

    /** Validate with a fixed instant; never execute arbitrary expressions. */
    public static void validate(String template) {
        generate(template, Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC, "example");
    }

    private static String resolve(String token, Instant instant, ZoneId zone, String selectedText) {
        return switch (token) {
            case "uuid" -> UUID.randomUUID().toString();
            case "uuid32" -> UUID.randomUUID().toString().replace("-", "");
            case "uuid_upper" -> UUID.randomUUID().toString().toUpperCase(java.util.Locale.ROOT);
            case "unix" -> Long.toString(instant.getEpochSecond());
            case "unix_ms" -> Long.toString(instant.toEpochMilli());
            case "selection" -> selectedText;
            default -> {
                if (token.startsWith("date:")) {
                    yield date(token.substring(5), zone, instant);
                }
                if (token.startsWith("utc:")) {
                    yield date(token.substring(4), ZoneOffset.UTC, instant);
                }
                if (token.startsWith("rand:")) {
                    String countString = token.substring(5);
                    int count;
                    try { count = Integer.parseInt(countString); }
                    catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Invalid random digits count: " + countString);
                    }
                    if (count < 1 || count > 64) {
                        throw new IllegalArgumentException("${rand:N}: N must be between 1 and 64");
                    }
                    StringBuilder digits = new StringBuilder(count);
                    for (int i = 0; i < count; i++) digits.append(RANDOM.nextInt(10));
                    yield digits.toString();
                }
                throw new IllegalArgumentException("Unknown placeholder: ${" + token + "}");
            }
        };
    }

    private static String date(String pattern, ZoneId zone, Instant instant) {
        if (pattern.isEmpty()) throw new IllegalArgumentException("Date pattern must not be empty");
        try {
            return DateTimeFormatter.ofPattern(pattern).withZone(zone).format(instant);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid date pattern: " + pattern + " (" + ex.getMessage() + ")", ex);
        }
    }
}
