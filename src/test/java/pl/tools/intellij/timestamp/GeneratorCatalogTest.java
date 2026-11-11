package pl.tools.intellij.timestamp;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorCatalogTest {
    @Test void initialListContainsEveryBuiltInAtRootInOrder() {
        List<GeneratorEntry> all = GeneratorCatalog.defaults();
        assertEquals(7, all.size());
        assertEquals("Timestamp: SQL", all.get(0).name);
        assertEquals("UUID: Random v4 (no hyphens)", all.get(6).name);
        assertEquals(7, GeneratorCatalog.children(all, "").size());
        GeneratorCatalog.validate(all);
    }

    @Test void migrateOldCustomTemplatesWithoutLosingThem() {
        List<GeneratorEntry> all = GeneratorCatalog.migrateLegacy(List.of(
                new GeneratorDefinition("GLOBE ID", "PL-${uuid32}")));
        assertEquals(8, all.size());
        assertEquals("GLOBE ID", all.get(7).name);
        assertEquals("PL-${uuid32}", all.get(7).template);
        GeneratorCatalog.validate(all);
    }

    @Test void nestedFolderAndMovedGeneratorProduceCorrectChildren() {
        List<GeneratorEntry> entries = GeneratorCatalog.defaults();
        GeneratorEntry parent = GeneratorEntry.folder("", "Messages");
        GeneratorEntry child = GeneratorEntry.folder(parent.id, "GLOBE");
        GeneratorEntry generated = GeneratorEntry.generator(child.id, "GLOBE ref", "${date:yyyyMMdd}-${uuid32}");
        entries.add(parent); entries.add(child); entries.add(generated);
        entries.get(0).parentId = child.id;
        assertEquals(List.of(parent), GeneratorCatalog.children(entries, "").stream()
                .filter(GeneratorEntry::isFolder).toList());
        assertEquals(List.of(child), GeneratorCatalog.children(entries, parent.id));
        assertEquals(List.of(generated, entries.get(0)), GeneratorCatalog.children(entries, child.id));
        GeneratorCatalog.validate(entries);
    }

    @Test void allowsRemovingEveryGeneratorWithoutRestoringDefaults() {
        GeneratorCatalog.validate(List.of());
    }

    @Test void rejectsCyclesAndOrphanFolders() {
        GeneratorEntry a = GeneratorEntry.folder("", "A");
        GeneratorEntry b = GeneratorEntry.folder(a.id, "B");
        a.parentId = b.id;
        assertThrows(IllegalArgumentException.class, () -> GeneratorCatalog.validate(List.of(a,b)));
        b.parentId = "missing";
        assertThrows(IllegalArgumentException.class, () -> GeneratorCatalog.validate(List.of(b)));
    }

    @Test void rejectsDuplicateNamesInSameFolderButAllowsAcrossFolders() {
        GeneratorEntry folder = GeneratorEntry.folder("", "Folder");
        GeneratorEntry root = GeneratorEntry.generator("", "Example", "${uuid}");
        GeneratorEntry duplicate = GeneratorEntry.generator("", "example", "${uuid}");
        assertThrows(IllegalArgumentException.class, () -> GeneratorCatalog.validate(List.of(folder,root,duplicate)));
        duplicate.parentId = folder.id;
        GeneratorCatalog.validate(List.of(folder,root,duplicate));
    }

    @Test void invalidTemplateRejectedAtApplyValidation() {
        GeneratorEntry invalid = GeneratorEntry.generator("", "Broken", "${no_such_variable}");
        assertThrows(IllegalArgumentException.class, () -> GeneratorCatalog.validate(List.of(invalid)));
    }
}
