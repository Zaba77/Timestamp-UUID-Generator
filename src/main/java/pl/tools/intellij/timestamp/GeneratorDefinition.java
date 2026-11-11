package pl.tools.intellij.timestamp;

import java.util.Objects;

/** Public bean fields allow IntelliJ's XML persistence to serialize the definition. */
public final class GeneratorDefinition {
    public String name = "";
    public String template = "";

    public GeneratorDefinition() { }

    public GeneratorDefinition(String name, String template) {
        this.name = name;
        this.template = template;
    }

    public GeneratorDefinition copy() {
        return new GeneratorDefinition(name, template);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GeneratorDefinition that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, template);
    }
}
