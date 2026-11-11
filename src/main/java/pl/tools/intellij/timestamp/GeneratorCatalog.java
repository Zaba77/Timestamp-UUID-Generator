package pl.tools.intellij.timestamp;

import java.util.*;

/** Platform-independent menu model. Entries are stored in pre-order for stable sibling ordering. */
public final class GeneratorCatalog {
    private GeneratorCatalog() { }

    public static List<GeneratorEntry> defaults() {
        return new ArrayList<>(List.of(
                builtin("sql", "Timestamp: SQL", "${date:yyyy-MM-dd HH:mm:ss.SSS}"),
                builtin("iso", "Timestamp: ISO 8601", "${date:yyyy-MM-dd'T'HH:mm:ss.SSSXXX}"),
                builtin("unix", "Timestamp: UNIX seconds", "${unix}"),
                builtin("unixms", "Timestamp: UNIX milliseconds", "${unix_ms}"),
                builtin("compact", "Timestamp: Compact", "${date:yyyyMMddHHmmssSSS}"),
                builtin("uuid", "UUID: Random v4", "${uuid}"),
                builtin("uuid32", "UUID: Random v4 (no hyphens)", "${uuid32}")
        ));
    }

    private static GeneratorEntry builtin(String id, String name, String template) {
        return new GeneratorEntry("builtin." + id, "", name, "generator", template);
    }

    /** Copy legacy entries to the root, keeping user-defined generators across 1.1.x -> 1.2.0. */
    public static List<GeneratorEntry> migrateLegacy(List<GeneratorDefinition> legacy) {
        List<GeneratorEntry> entries = defaults();
        if (legacy != null) for (GeneratorDefinition item : legacy) {
            if (item != null && item.name != null && item.template != null && !item.name.isBlank()) {
                entries.add(GeneratorEntry.generator("", item.name, item.template));
            }
        }
        return entries;
    }

    /** Returns direct children; storage list order is the menu order. */
    public static List<GeneratorEntry> children(List<GeneratorEntry> entries, String parentId) {
        String normalized = parentId == null ? "" : parentId;
        List<GeneratorEntry> result = new ArrayList<>();
        for (GeneratorEntry entry : entries) {
            if (entry != null && normalized.equals(entry.parentId)) result.add(entry);
        }
        return result;
    }

    /** Validate as a forest, including cycles and duplicate names within each folder. */
    public static void validate(List<GeneratorEntry> entries) {
        Map<String, GeneratorEntry> index = new HashMap<>();
        for (GeneratorEntry e : entries) {
            if (e == null || e.id == null || e.id.isBlank()) throw new IllegalArgumentException("Missing entry ID");
            if (index.putIfAbsent(e.id, e) != null) throw new IllegalArgumentException("Duplicate ID: " + e.id);
            if (e.name == null || e.name.isBlank()) throw new IllegalArgumentException("Empty menu item name");
            if (!"folder".equals(e.type) && !"generator".equals(e.type)) {
                throw new IllegalArgumentException("Unknown entry type for: " + e.name);
            }
            if (!e.isFolder()) TemplateEngine.validate(e.template);
        }
        Set<String> siblingNames = new HashSet<>();
        for (GeneratorEntry e : entries) {
            String parent = e.parentId == null ? "" : e.parentId;
            if (!parent.isEmpty()) {
                GeneratorEntry owner = index.get(parent);
                if (owner == null || !owner.isFolder()) throw new IllegalArgumentException("Invalid folder for: " + e.name);
            }
            String collisionKey = parent + "\u0000" + e.name.trim().toLowerCase(Locale.ROOT);
            if (!siblingNames.add(collisionKey)) throw new IllegalArgumentException("Duplicate name in the same folder: " + e.name);
            Set<String> ancestors = new HashSet<>();
            String cursor = e.id;
            while (!cursor.isEmpty()) {
                if (!ancestors.add(cursor)) throw new IllegalArgumentException("Folder cycle at: " + e.name);
                GeneratorEntry current = index.get(cursor);
                if (current == null) break;
                cursor = current.parentId == null ? "" : current.parentId;
            }
        }
    }

}
