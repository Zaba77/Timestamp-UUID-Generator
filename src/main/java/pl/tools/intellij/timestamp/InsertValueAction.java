package pl.tools.intellij.timestamp;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

/** Common editor insertion behavior for built-in and user-defined generators. */
public abstract class InsertValueAction extends AnAction implements DumbAware {

    protected InsertValueAction() { super(); }
    protected InsertValueAction(String text) { super(text); }

    /** A single invocation generates one value, using the previous selection if necessary. */
    protected abstract String generate(String selectedText);

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        event.getPresentation().setEnabledAndVisible(editor != null
                && event.getProject() != null
                && !editor.isViewer() && editor.getDocument().isWritable());
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        Project project = event.getProject();
        if (editor == null || project == null || editor.isViewer() || !editor.getDocument().isWritable()) {
            return;
        }

        Caret caret = editor.getCaretModel().getPrimaryCaret();
        int start = caret.getSelectionStart();
        int end = caret.getSelectionEnd();
        Document document = editor.getDocument();
        String selectedText = document.getText().substring(start, end);
        final String value;
        try {
            value = generate(selectedText);
        } catch (IllegalArgumentException ex) {
            Messages.showErrorDialog(project, ex.getMessage(), "Generator error");
            return;
        }
        WriteCommandAction.runWriteCommandAction(project, () -> {
            // The IntelliJ Platform Document API expects CharSequence (not String).
            // Keep the cast to prevent accidentally compiling against incompatible API stubs.
            document.replaceString(start, end, (CharSequence) value);
            caret.moveToOffset(start + value.length());
            caret.removeSelection();
        });
    }
}
