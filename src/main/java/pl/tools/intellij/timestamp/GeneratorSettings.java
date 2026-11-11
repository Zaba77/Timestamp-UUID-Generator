package pl.tools.intellij.timestamp;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import java.util.ArrayList;
import java.util.List;

/** Preserve plugin ID, @State name and storage path for automatic upgrades from v1.1.x. */
@State(name = "ValueGeneratorSettings", storages = @Storage("value-generator.xml"))
public final class GeneratorSettings implements PersistentStateComponent<GeneratorSettings.StoredState> {
    public static final class StoredState {
        public int schemaVersion = 0;
        public List<GeneratorEntry> entries = new ArrayList<>();
        // Only needed for reading the old XML; not written in migrated state.
        public List<GeneratorDefinition> generators = new ArrayList<>();
    }

    private List<GeneratorEntry> entries = GeneratorCatalog.defaults();

    public static GeneratorSettings getInstance() {
        return ApplicationManager.getApplication().getService(GeneratorSettings.class);
    }

    @Override public synchronized StoredState getState() {
        StoredState result = new StoredState();
        result.schemaVersion = 2;
        result.entries = copy(entries);
        return result;
    }

    @Override public synchronized void loadState(StoredState old) {
        if (old == null) {
            entries = GeneratorCatalog.defaults();
        } else if (old.schemaVersion >= 2) {
            entries = copy(old.entries == null ? List.of() : old.entries);
        } else {
            entries = GeneratorCatalog.migrateLegacy(old.generators);
        }
    }

    public synchronized List<GeneratorEntry> getEntries() { return copy(entries); }

    public synchronized void setEntries(List<GeneratorEntry> value) {
        GeneratorCatalog.validate(value);
        entries = copy(value);
    }

    private static List<GeneratorEntry> copy(List<GeneratorEntry> values) {
        List<GeneratorEntry> result = new ArrayList<>();
        for (GeneratorEntry e : values) if (e != null) result.add(e.copy());
        return result;
    }
}
