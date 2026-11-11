package pl.tools.intellij.timestamp;

import java.time.Instant;
import java.time.ZoneId;

/** Both built-in and user-defined generators use the same insertion action. */
public final class CustomGeneratorAction extends InsertValueAction {
    private final String template;

    public CustomGeneratorAction(GeneratorEntry definition) {
        super(definition.name);
        template = definition.template;
    }

    @Override protected String generate(String selectedText) {
        return TemplateEngine.generate(template, Instant.now(), ZoneId.systemDefault(), selectedText);
    }
}
