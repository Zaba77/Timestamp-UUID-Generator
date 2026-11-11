package pl.tools.intellij.timestamp;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;

import java.util.ArrayList;
import java.util.List;

/** A popup folder built dynamically from the saved hierarchy (arbitrarily nested). */
public final class GeneratorFolderGroup extends ActionGroup implements DumbAware {
    private final GeneratorEntry folder;
    private final List<GeneratorEntry> snapshot;

    public GeneratorFolderGroup(GeneratorEntry folder, List<GeneratorEntry> snapshot) {
        super(folder.name, true);
        this.folder = folder;
        this.snapshot = snapshot;
    }

    @Override public ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }

    @Override public AnAction[] getChildren(AnActionEvent event) {
        List<AnAction> result = new ArrayList<>();
        for (GeneratorEntry entry : GeneratorCatalog.children(snapshot, folder.id)) {
            result.add(entry.isFolder() ? new GeneratorFolderGroup(entry, snapshot) : new CustomGeneratorAction(entry));
        }
        return result.toArray(AnAction[]::new);
    }
}
