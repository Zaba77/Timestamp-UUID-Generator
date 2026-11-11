package pl.tools.intellij.timestamp;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Unified editable tree: built-ins and custom templates are treated identically. */
public final class GeneratorConfigurable implements Configurable {
    private JPanel panel;
    private JTree tree;
    private DefaultTreeModel model;
    private DefaultMutableTreeNode root;
    private JTextArea details;

    @Override public String getDisplayName() { return "Value Generator"; }

    @Override public JComponent createComponent() {
        if (panel != null) return panel;
        panel = new JPanel(new BorderLayout(0, 8));

        JTextArea help = new JTextArea("All Alt+Insert > Generate entries are managed below. " +
                "Root = directly in Generate; folders = popup submenus. " +
                "Drag and drop to move/reorder, or use Move to... and Up/Down. " +
                "Changes become active after Apply; no IDE restart needed.");
        help.setEditable(false);
        help.setFocusable(false);
        help.setOpaque(false);
        help.setLineWrap(true);
        help.setWrapStyleWord(true);
        help.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
        panel.add(help, BorderLayout.NORTH);

        root = new DefaultMutableTreeNode("Generate (Alt+Insert) [root]");
        model = new DefaultTreeModel(root);
        tree = new JTree(model);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRowHeight(Math.max(23, tree.getRowHeight()));
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                Object data = ((DefaultMutableTreeNode) value).getUserObject();
                if (data instanceof GeneratorEntry e) {
                    setText((e.isFolder() ? "[Folder] " : "") + e.name);
                }
                return this;
            }
        });
        // Searchable-options indexing creates this UI without a graphical display.
        tree.setDragEnabled(!GraphicsEnvironment.isHeadless());
        tree.setDropMode(DropMode.ON_OR_INSERT);
        tree.setTransferHandler(new GeneratorTransferHandler());
        tree.addTreeSelectionListener(e -> updateDetails());

        details = new JTextArea(10, 24);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setMargin(new Insets(7, 7, 7, 7));
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(tree), new JScrollPane(details));
        split.setResizeWeight(0.65);
        split.setPreferredSize(new Dimension(770, 430));
        panel.add(split, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 2));
        JButton addGenerator = new JButton("+ Generator");
        JButton addFolder = new JButton("+ Folder");
        JButton edit = new JButton("Edit / Rename");
        JButton duplicate = new JButton("Duplicate");
        JButton remove = new JButton("Remove");
        JButton up = new JButton("Up");
        JButton down = new JButton("Down");
        JButton moveTo = new JButton("Move to...");
        JButton preview = new JButton("Preview");
        JPanel firstRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        for (JButton b : List.of(addGenerator, addFolder, edit, duplicate, remove)) firstRow.add(b);
        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        for (JButton b : List.of(up, down, moveTo, preview)) secondRow.add(b);
        controls.add(firstRow);
        controls.add(secondRow);
        panel.add(controls, BorderLayout.SOUTH);

        addGenerator.addActionListener(e -> add(false));
        addFolder.addActionListener(e -> add(true));
        edit.addActionListener(e -> edit());
        duplicate.addActionListener(e -> duplicate());
        remove.addActionListener(e -> remove());
        up.addActionListener(e -> reorder(-1));
        down.addActionListener(e -> reorder(1));
        moveTo.addActionListener(e -> chooseDestination());
        preview.addActionListener(e -> preview());
        reset();
        return panel;
    }

    private DefaultMutableTreeNode selected() {
        if (tree == null || tree.getSelectionPath() == null) return root;
        return (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
    }

    private GeneratorEntry item(DefaultMutableTreeNode node) {
        return node != null && node.getUserObject() instanceof GeneratorEntry e ? e : null;
    }

    private DefaultMutableTreeNode insertionParent() {
        DefaultMutableTreeNode node = selected();
        GeneratorEntry e = item(node);
        return node == root || (e != null && e.isFolder()) ? node : (DefaultMutableTreeNode) node.getParent();
    }

    private void add(boolean folder) {
        DefaultMutableTreeNode parent = insertionParent();
        String parentId = parent == root ? "" : item(parent).id;
        GeneratorEntry entry = folder ? GeneratorEntry.folder(parentId, "New folder") :
                GeneratorEntry.generator(parentId, "New generator", "${uuid}");
        if (!editDialog(entry, folder ? "Create folder" : "Create generator")) return;
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(entry);
        model.insertNodeInto(node, parent, parent.getChildCount());
        focus(node);
    }

    private void edit() {
        DefaultMutableTreeNode node = selected();
        GeneratorEntry previous = item(node);
        if (previous == null) return;
        GeneratorEntry working = previous.copy();
        if (!editDialog(working, previous.isFolder() ? "Rename folder" : "Edit generator")) return;
        node.setUserObject(working);
        model.nodeChanged(node);
        focus(node);
    }

    private boolean editDialog(GeneratorEntry entry, String title) {
        JTextField name = new JTextField(entry.name, 33);
        JTextArea template = new JTextArea(entry.template, 6, 36);
        template.setLineWrap(true);
        template.setWrapStyleWord(true);
        JPanel content = new JPanel(new BorderLayout(0, 7));
        JPanel namePanel = new JPanel(new BorderLayout(0, 3));
        namePanel.add(new JLabel("Menu item name:"), BorderLayout.NORTH);
        namePanel.add(name, BorderLayout.CENTER);
        content.add(namePanel, BorderLayout.NORTH);
        if (!entry.isFolder()) {
            JPanel templatePanel = new JPanel(new BorderLayout(0, 3));
            templatePanel.add(new JLabel("Template (${uuid}, ${uuid32}, ${date:...}, ${unix}, ${rand:8}, ${selection}):"), BorderLayout.NORTH);
            templatePanel.add(new JScrollPane(template), BorderLayout.CENTER);
            content.add(templatePanel, BorderLayout.CENTER);
        }
        while (true) {
            if (JOptionPane.showConfirmDialog(panel, content, title,
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return false;
            String proposedName = name.getText().trim();
            if (proposedName.isBlank()) {
                JOptionPane.showMessageDialog(panel, "The name cannot be empty.", "Invalid name", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            try {
                if (!entry.isFolder()) TemplateEngine.validate(template.getText());
                entry.name = proposedName;
                if (!entry.isFolder()) entry.template = template.getText();
                return true;
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(panel, ex.getMessage(), "Invalid template", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void duplicate() {
        DefaultMutableTreeNode node = selected();
        GeneratorEntry original = item(node);
        if (original == null || original.isFolder()) return;
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        String parentId = parent == root ? "" : item(parent).id;
        GeneratorEntry copy = GeneratorEntry.generator(parentId, original.name + " (copy)", original.template);
        if (!editDialog(copy, "Duplicate generator")) return;
        DefaultMutableTreeNode inserted = new DefaultMutableTreeNode(copy);
        model.insertNodeInto(inserted, parent, parent.getIndex(node) + 1);
        focus(inserted);
    }

    private void remove() {
        DefaultMutableTreeNode node = selected();
        GeneratorEntry e = item(node);
        if (e == null) return;
        String message = e.isFolder() ?
                "Delete folder '" + e.name + "' and ALL its nested generators and subfolders?" :
                "Delete generator '" + e.name + "'?";
        if (JOptionPane.showConfirmDialog(panel, message, "Remove item",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return;
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        model.removeNodeFromParent(node);
        focus(parent);
    }

    private void reorder(int delta) {
        DefaultMutableTreeNode node = selected();
        if (node == root) return;
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        int position = parent.getIndex(node);
        int target = position + delta;
        if (target < 0 || target >= parent.getChildCount()) return;
        model.removeNodeFromParent(node);
        model.insertNodeInto(node, parent, target);
        focus(node);
    }

    private void chooseDestination() {
        DefaultMutableTreeNode node = selected();
        if (node == root) return;
        List<FolderOption> destinations = new ArrayList<>();
        destinations.add(new FolderOption("[Root] Generate (Alt+Insert)", root));
        collectFolders(root, "", destinations);
        FolderOption choice = (FolderOption) JOptionPane.showInputDialog(panel,
                "Move the selected item into:", "Move to folder/root", JOptionPane.PLAIN_MESSAGE,
                null, destinations.toArray(), destinations.get(0));
        if (choice == null) return;
        if (!canMove(node, choice.node)) {
            JOptionPane.showMessageDialog(panel, "Cannot move an item into itself or its descendant.");
            return;
        }
        place(node, choice.node, choice.node.getChildCount());
    }

    private void collectFolders(DefaultMutableTreeNode parent, String indent, List<FolderOption> target) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            GeneratorEntry e = item(child);
            if (e.isFolder()) {
                target.add(new FolderOption(indent + e.name, child));
                collectFolders(child, indent + "  /  ", target);
            }
        }
    }

    private record FolderOption(String label, DefaultMutableTreeNode node) {
        @Override public String toString() { return label; }
    }

    private boolean canMove(DefaultMutableTreeNode source, DefaultMutableTreeNode destParent) {
        return source != root && source != destParent && !destParent.isNodeAncestor(source);
    }

    private boolean place(DefaultMutableTreeNode node, DefaultMutableTreeNode parent, int index) {
        if (!canMove(node, parent)) return false;
        DefaultMutableTreeNode oldParent = (DefaultMutableTreeNode) node.getParent();
        if (oldParent == parent && oldParent.getIndex(node) < index) index--;
        model.removeNodeFromParent(node);
        model.insertNodeInto(node, parent, Math.min(index, parent.getChildCount()));
        focus(node);
        return true;
    }

    private void preview() {
        GeneratorEntry entry = item(selected());
        if (entry == null || entry.isFolder()) return;
        try {
            String value = TemplateEngine.generate(entry.template, java.time.Instant.now(),
                    java.time.ZoneId.systemDefault(), "SELECTED_TEXT");
            JTextArea display = new JTextArea(value, 5, 48);
            display.setEditable(false);
            display.setWrapStyleWord(true);
            display.setLineWrap(true);
            JOptionPane.showMessageDialog(panel, new JScrollPane(display), "Preview: " + entry.name,
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(panel, ex.getMessage(), "Invalid generator", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void focus(DefaultMutableTreeNode node) {
        TreePath path = new TreePath(node.getPath());
        tree.expandPath(path.getParentPath());
        tree.setSelectionPath(path);
        tree.scrollPathToVisible(path);
        updateDetails();
    }

    private void updateDetails() {
        if (details == null) return;
        DefaultMutableTreeNode node = selected();
        GeneratorEntry e = item(node);
        if (e == null) {
            details.setText("ROOT\n\nAll entries directly beneath this node appear in Alt+Insert > Generate.\n\n" +
                    "Add generators or subfolders, or move an existing entry here.");
        } else if (e.isFolder()) {
            details.setText("FOLDER: " + e.name + "\n\nThis becomes a popup submenu. " +
                    "Drag entries into the folder, or use Move to...\n\n" +
                    "You can nest folders to any depth.");
        } else {
            details.setText("GENERATOR: " + e.name + "\n\nTEMPLATE:\n" + e.template +
                    "\n\nAvailable placeholders:\n${uuid}, ${uuid32}, ${uuid_upper}\n" +
                    "${date:FORMAT}, ${utc:FORMAT}, ${unix}, ${unix_ms}\n" +
                    "${rand:N}, ${selection}\n\nSelect Preview to generate a sample.");
        }
        details.setCaretPosition(0);
    }

    private List<GeneratorEntry> editedEntries() {
        List<GeneratorEntry> result = new ArrayList<>();
        if (root != null) flatten(root, "", result);
        return result;
    }

    private void flatten(DefaultMutableTreeNode parent, String parentId, List<GeneratorEntry> result) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            GeneratorEntry entry = item(child).copy();
            entry.parentId = parentId;
            result.add(entry);
            if (entry.isFolder()) flatten(child, entry.id, result);
        }
    }

    @Override public boolean isModified() {
        return panel != null && !editedEntries().equals(GeneratorSettings.getInstance().getEntries());
    }

    @Override public void apply() throws ConfigurationException {
        List<GeneratorEntry> entries = editedEntries();
        try {
            GeneratorCatalog.validate(entries);
            GeneratorSettings.getInstance().setEntries(entries);
        } catch (IllegalArgumentException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
    }

    @Override public void reset() {
        if (model == null) return;
        root.removeAllChildren();
        List<GeneratorEntry> entries = GeneratorSettings.getInstance().getEntries();
        Map<String, DefaultMutableTreeNode> byId = new HashMap<>();
        for (GeneratorEntry entry : entries) {
            byId.put(entry.id, new DefaultMutableTreeNode(entry.copy()));
        }
        // Resolve parents in any persisted order, preserving sibling order.
        for (GeneratorEntry entry : entries) {
            DefaultMutableTreeNode node = byId.get(entry.id);
            DefaultMutableTreeNode parent = entry.parentId == null || entry.parentId.isEmpty()
                    ? root : byId.get(entry.parentId);
            if (parent == null || parent == node) parent = root;
            parent.add(node);
        }
        model.reload();
        tree.expandRow(0);
        tree.setSelectionPath(new TreePath(root.getPath()));
        updateDetails();
    }

    @Override public void disposeUIResources() {
        panel = null;
        tree = null;
        model = null;
        root = null;
        details = null;
    }

    private final class GeneratorTransferHandler extends TransferHandler {
        @Override public int getSourceActions(JComponent c) { return MOVE; }

        @Override protected Transferable createTransferable(JComponent c) {
            DefaultMutableTreeNode node = selected();
            GeneratorEntry entry = item(node);
            return entry == null ? null : new StringSelection(entry.id);
        }

        @Override public boolean canImport(TransferSupport support) {
            if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
            try {
                String id = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                DefaultMutableTreeNode dragged = findById(root, id);
                JTree.DropLocation loc = (JTree.DropLocation) support.getDropLocation();
                if (loc.getPath() == null || dragged == null) return false;
                Destination dest = destination(loc);
                return dest != null && canMove(dragged, dest.parent);
            } catch (Exception ex) { return false; }
        }

        @Override public boolean importData(TransferSupport support) {
            if (!canImport(support)) return false;
            try {
                String id = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                DefaultMutableTreeNode dragged = findById(root, id);
                Destination dest = destination((JTree.DropLocation) support.getDropLocation());
                return dest != null && place(dragged, dest.parent, dest.index);
            } catch (Exception ex) { return false; }
        }

        private Destination destination(JTree.DropLocation drop) {
            DefaultMutableTreeNode target = (DefaultMutableTreeNode) drop.getPath().getLastPathComponent();
            if (drop.getChildIndex() >= 0) return new Destination(target, drop.getChildIndex());
            GeneratorEntry data = item(target);
            if (target == root || (data != null && data.isFolder())) {
                return new Destination(target, target.getChildCount());
            }
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) target.getParent();
            return new Destination(parent, parent.getIndex(target) + 1);
        }
    }

    private record Destination(DefaultMutableTreeNode parent, int index) { }

    private DefaultMutableTreeNode findById(DefaultMutableTreeNode node, String id) {
        GeneratorEntry e = item(node);
        if (e != null && e.id.equals(id)) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode found = findById((DefaultMutableTreeNode) node.getChildAt(i), id);
            if (found != null) return found;
        }
        return null;
    }
}
