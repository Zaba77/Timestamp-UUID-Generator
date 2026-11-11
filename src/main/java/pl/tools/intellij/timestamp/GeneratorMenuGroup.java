package pl.tools.intellij.timestamp;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;

import java.util.ArrayList;
import java.util.List;

/** Non-popup group expands in-place: saved root generators appear directly in Generate. */
public final class GeneratorMenuGroup extends ActionGroup implements DumbAware {
    public GeneratorMenuGroup() { super("Value generators", false); }

    @Override public ActionUpdateThread getActionUpdateThread() { return ActionUpdateThread.BGT; }

    @Override public AnAction[] getChildren(AnActionEvent event) {
        List<GeneratorEntry> snapshot = GeneratorSettings.getInstance().getEntries();
        List<AnAction> actions = new ArrayList<>();
        for (GeneratorEntry entry : GeneratorCatalog.children(snapshot, "")) {
            actions.add(entry.isFolder() ? new GeneratorFolderGroup(entry, snapshot) : new CustomGeneratorAction(entry));
        }
        actions.add(new ManageGeneratorsAction());
        return actions.toArray(AnAction[]::new);
    }
}
