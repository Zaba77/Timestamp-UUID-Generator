package pl.tools.intellij.timestamp;

import java.util.Objects;
import java.util.UUID;

/** Persisted generator or virtual menu folder. A null/empty parentId means root of Generate. */
public final class GeneratorEntry {
    public String id = "";
    public String parentId = "";
    public String name = "";
    public String type = "generator";
    public String template = "";

    public GeneratorEntry() { }

    public GeneratorEntry(String id, String parentId, String name, String type, String template) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.type = type;
        this.template = template;
    }

    public static GeneratorEntry generator(String parentId, String name, String template) {
        return new GeneratorEntry(UUID.randomUUID().toString(), parentId, name, "generator", template);
    }

    public static GeneratorEntry folder(String parentId, String name) {
        return new GeneratorEntry(UUID.randomUUID().toString(), parentId, name, "folder", "");
    }

    public boolean isFolder() { return "folder".equals(type); }
    public GeneratorEntry copy() { return new GeneratorEntry(id, parentId, name, type, template); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GeneratorEntry that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(parentId, that.parentId) &&
                Objects.equals(name, that.name) && Objects.equals(type, that.type) &&
                Objects.equals(template, that.template);
    }

    @Override public int hashCode() { return Objects.hash(id, parentId, name, type, template); }
    @Override public String toString() { return name; }
}
