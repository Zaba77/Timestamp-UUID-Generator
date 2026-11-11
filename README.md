# Timestamp & UUID Generator

![Timestamp & UUID Generator](src/main/resources/META-INF/pluginIcon.svg)

**Version:** 1.2.0 · IntelliJ IDEA plugin

**Updated version of the existing plugin**, retaining the `pl.tools.intellij.timestampgenerator` identifier.
It provides full control over the entries in `Alt+Insert → Generate`.

## What's new

- **All** generators, including the built-in SQL/ISO/UNIX/UUID generators, can be edited and removed in **one tree**.
- Generators can appear directly at the **root** of the `Generate` menu, as well as in “My generators”.
- Folders and nested subfolders can be created as submenus.
- Generators and folders can be moved between the root and folders using buttons or **drag and drop**.
- Available controls: **Up**, **Down**, **Move to...**, **Duplicate**, **Edit / Rename**, **Remove**, **Preview**.
- Clicking **Apply** updates the menu without restarting the IDE.
- Settings are saved globally for the IDE.
- Templates added in 1.1.x are migrated from the old `value-generator.xml` file.
- The corrected `Document.replaceString(int,int,CharSequence)` call is preserved.

## Usage

Open `Settings → Tools → Value Generator`:

1. In the `Generate (Alt+Insert) [root]` tree, click **+ Generator** to add an entry directly to `Alt+Insert`.
2. Click **+ Folder** to add a submenu. Select a folder to create further subfolders inside it.
3. Select a generator or folder and use **Move to...** to choose another folder or the root. You can also drag entries within the tree.
4. Use **Up / Down** to change the order. **Edit / Rename** changes the name or template, **Duplicate** copies a generator, and **Remove** deletes an entry.
5. Click **Apply** in the settings window to use the new layout in `Alt+Insert → Generate`.

**Note:** deleting a folder also deletes all generators and subfolders inside it and requires confirmation. Built-in generators can also be removed. To restore a deleted generator, add it again with the appropriate template.

Example menu layout:

| Menu path | Item |
|---|---|
| `Generate → UUID: Random v4` | Built-in generator at the root |
| `Generate → Timestamp: SQL` | Built-in generator at the root |
| `Generate → Test ID` | Custom generator at the root |
| `Generate → GLOBE → MessageRefID` | Generator in a folder |
| `Generate → GLOBE → XML IDs → DocumentRefID` | Generator in a subfolder |
| `Generate → Configure generators...` | Generator settings |

## Available templates

| Token | Output |
|---|---|
| `${uuid}` | UUID v4 with hyphens |
| `${uuid32}` | UUID v4 without hyphens |
| `${uuid_upper}` | Uppercase UUID v4 |
| `${unix}` | UNIX timestamp in seconds |
| `${unix_ms}` | UNIX timestamp in milliseconds |
| `${date:yyyyMMddHHmmss}` | Current time in the local time zone |
| `${utc:yyyy-MM-dd'T'HH:mm:ss'Z'}` | Current time in UTC |
| `${rand:8}` | Eight random digits |
| `${selection}` | Text selected in the editor |

A template can combine text and multiple tokens, for example `PL-${date:yyyyMMdd}-${uuid32}`. Prefixing `${...}` with a backslash (`\`) produces literal text.

The generated value is inserted at the caret or replaces the current selection. Use `Ctrl+Z` to undo the insertion.

## Building and installing on Windows

**Requirements:** JDK 21 and an internet connection to download the official JetBrains dependencies.

1. Clone the repository or select **Code → Download ZIP** on GitHub and extract the downloaded archive.
2. Double-click **`build-plugin.bat`** in the project directory.
3. The script downloads the official **Gradle 9.2.1** distribution, runs the tests, and invokes `buildPlugin` with IntelliJ Platform SDK **2024.2.6**.
4. After a successful build, the installable archive is available at `build/distributions/*.zip`.
5. In IntelliJ IDEA, open `Settings → Plugins → ⚙ → Install Plugin from Disk...` and select the ZIP from `build/distributions`.

If Gradle 9+ is already installed, you can also run `gradle test buildPlugin`.

When upgrading from 1.1.1, keep the `value-generator.xml` file: it is required to migrate existing generators.

## Tests and limitations

- Template logic, the default catalog, migration, folder validation, and Swing tree operations (moving, reordering, and cycle prevention) have been checked locally.
- **This archive contains project sources.** Build the installable plugin ZIP before installation.
- Build the distribution using the provided script, which uses the official IntelliJ SDK. Install the ZIP produced in `build/distributions`.
