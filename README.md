# Timestamp & UUID Generator

![Timestamp & UUID Generator](src/main/resources/META-INF/pluginIcon.svg)

**Wersja:** 1.2.0 · Wtyczka do IntelliJ IDEA

**Nowa wersja dotychczasowej wtyczki**, nadal z identyfikatorem `pl.tools.intellij.timestampgenerator`.
Jej celem jest pełna kontrola nad pozycjami w `Alt+Insert → Generate`.

## Co się zmieniło

- **Wszystkie** generatory, także wbudowane SQL/ISO/UNIX/UUID, są edytowalne i usuwalne w **jednym drzewie**.
- Generator można umieścić bezpośrednio w **root** menu `Generate`, nie tylko w „My generators”.
- Można tworzyć foldery i dowolnie zagnieżdżone podfoldery, stanowiące podmenu.
- Można przenosić generator/folder między poziomem głównym i folderami, także metodą **przeciągnij i upuść**.
- Przyciski **Up**, **Down**, **Move to...**, **Duplicate**, **Edit / Rename**, **Remove**, **Preview**.
- Zmiany aktualizują menu po kliknięciu **Apply**, bez restartu.
- Konfiguracja zapisywana globalnie dla IDE, a nie dla konkretnego projektu.
- Migracja szablonów dodanych w 1.1.x (stary `value-generator.xml`).
- Poprawiona metoda `Document.replaceString(int,int,CharSequence)` nadal zachowana.

## Jak używać

`Settings → Tools → Value Generator`:

1. Na drzewie `Generate (Alt+Insert) [root]` kliknij **+ Generator**, aby utworzyć pozycję bezpośrednio w `Alt+Insert`.
2. Kliknij **+ Folder**, aby dodać rozwijane podmenu. Będąc wewnątrz folderu, możesz dodać następne podfoldery.
3. Zaznacz generator/folder i użyj **Move to...**, aby wybrać inny folder albo root. Możesz również przeciągać pozycje w drzewie.
4. Użyj **Up / Down** do zmiany kolejności. **Edit / Rename** zmienia nazwę/treść, **Duplicate** kopiuje generator, **Remove** usuwa.
5. Kliknij **Apply** w oknie ustawień. Od tej chwili nowy układ obowiązuje w menu `Alt+Insert → Generate`.

**Uwaga:** usunięcie folderu usuwa wszystkie generatory i podfoldery w jego wnętrzu (wymaga potwierdzenia). Można usuwać także generatory domyślne. Aby przywrócić usunięty generator, dodaj go ponownie z odpowiednim szablonem.

Przykładowy układ:

```text
Alt+Insert > Generate
  UUID: Random v4                         <-- root
  Timestamp: SQL                          <-- root
  Test ID                                 <-- root, własny
  GLOBE >                                 <-- folder
    MessageRefID                          <-- generator
    XML IDs >                             <-- podfolder
      DocumentRefID
  Configure generators...
```

## Dostępne szablony

| Token | Wynik |
|---|---|
| `${uuid}` | UUID v4 z myślnikami |
| `${uuid32}` | UUID v4 bez myślników |
| `${uuid_upper}` | UUID v4 wielkimi literami |
| `${unix}` | UNIX timestamp (sekundy) |
| `${unix_ms}` | UNIX timestamp (milisekundy) |
| `${date:yyyyMMddHHmmss}` | aktualny czas w lokalnej strefie |
| `${utc:yyyy-MM-dd'T'HH:mm:ss'Z'}` | aktualny czas w UTC |
| `${rand:8}` | osiem losowych cyfr |
| `${selection}` | tekst zaznaczony w edytorze |

Dowolny szablon może łączyć tekst i wiele tokenów, np. `PL-${date:yyyyMMdd}-${uuid32}`. Poprzedzenie `${...}` ukośnikiem `\` daje dosłowny tekst.

Wynik jest wstawiany pod kursorem albo zastępuje zaznaczenie w edytorze. `Ctrl+Z` cofa wstawienie.

## Instalacja przez samodzielną kompilację na Windows

**Wymagania:** JDK 21 (i dostęp do internetu do pobrania oficjalnych zależności JetBrains).

1. Sklonuj repozytorium lub wybierz **Code → Download ZIP** na GitHubie i rozpakuj pobrane archiwum.
2. W katalogu projektu uruchom dwuklikiem **`build-plugin.bat`**.
3. Skrypt automatycznie pobiera oficjalny **Gradle 9.2.1**, testuje kod i uruchamia `buildPlugin` z IntelliJ Platform SDK **2024.2.6**.
4. Po sukcesie plik instalacyjny znajdziesz w `build/distributions/*.zip`.
5. IntelliJ: `Settings → Plugins → ⚙ → Install Plugin from Disk...` → wskaż ZIP z `build/distributions`.

Alternatywnie, jeśli masz już Gradle 9+, uruchom `gradle test buildPlugin`.

Przy aktualizacji z 1.1.1 nie usuwaj ręcznie pliku `value-generator.xml`: potrzebny jest do migracji generatorów.

## Testy i ograniczenia

- Logika szablonów, lista startowa, migracja, walidacja folderów i testy Swing drzewa (przenoszenie, zmiana kolejności, odrzucanie zapętleń) zostały uruchomione lokalnie.
- **To jest archiwum projektu, nie gotowy plugin instalacyjny.** 
- Produkcyjny ZIP należy zbudować skryptem, który używa oficjalnego IntelliJ SDK. Nie instaluj ZIP ze źródłami jako wtyczki.
