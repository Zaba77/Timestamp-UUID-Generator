package pl.tools.intellij.timestamp;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAware;

/** Opens the editable list of custom generator templates. */
public final class ManageGeneratorsAction extends AnAction implements DumbAware {
    public ManageGeneratorsAction() {
        super("Configure generators...");
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        ShowSettingsUtil.getInstance().showSettingsDialog(event.getProject(), GeneratorConfigurable.class);
    }
}
