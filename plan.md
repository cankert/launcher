# Minimalistischer Android-Launcher — Implementierungsplan

## Ziel
Ein extrem minimalistischer Android-Launcher (Vorbild: „minimalist phone"), der den System-Launcher ersetzen kann.

### Kernfeatures (MVP)
1. **Default-Launcher-fähig** (HOME-Intent Filter, First-Run-Setup-Dialog).
2. **Home-Screen**:
   - Uhrzeit groß zentriert im oberen Drittel.
   - Klick auf Uhrzeit → öffnet System-Wecker.
   - Kreis um die Uhrzeit als **Akku-Ring** (Füllgrad = Akkustand in %).
   - Darunter eine vertikale Liste **gepinnter Apps** (nur Textnamen, kein Icon).
3. **App-Drawer** (Swipe nach links → Drawer kommt von rechts):
   - Suchfeld oben.
   - Alphabetische Liste aller installierten Launcher-Apps.
   - Rechts A–Z-Quick-Scroll-Leiste.
   - Long-Press auf App → Kontextmenü (Pin/Uninstall/App-Info).
4. **Fixe Shortcuts am unteren Bildschirmrand** (auf Home-Screen):
   - Links unten: Telefon-Icon → öffnet Dialer.
   - Rechts unten: Kamera-Icon → öffnet Default-Kamera.

### Bewusst NICHT enthalten
- Keine App-Icons in der Liste (nur die zwei System-Shortcuts unten, sonst rein Text-basiert).
- Keine Widgets.
- Keine Wallpaper-Konfiguration.
- Keine Ordner.
- Keine Notifications-Dots.
- Keine Hidden-Apps-Funktion.

---

## Technologie-Entscheidung

**Empfehlung: Native Android mit Kotlin + Jetpack Compose** (statt React Native/Expo wie bei worktime).

### Begründung
- Ein Launcher braucht enge System-Integration (`PackageManager`, `BatteryManager`, HOME-Intent, App-Launch-Intents, Package-Visibility ab API 30). In React Native müssten alle diese Bridges als Native Modules gebaut werden → mehr Custom-Native-Code als wenn man von Anfang an nativ ist.
- **Performance**: Der Launcher startet bei jedem Home-Button-Druck. React-Native-Bridge-Overhead ist hier unerwünscht.
- **APK-Größe**: Native ~3–5 MB vs. RN ~15–25 MB — relevant für minimalistische App.
- **Compose** ist deklarativ → mentales Modell ähnlich React. Lernkurve gering, wenn React Native bereits bekannt ist.
- Distribution via APK-Sideload funktioniert identisch wie bei worktime (kein Play-Store-Account nötig).

### Alternative (falls bewusst RN gewünscht)
Expo Bare Workflow + `react-native-launcher-kit` (community) + eigene Native Module für Battery/Pinning. Nicht empfohlen — mehr Aufwand, weniger sauber.

### Stack
- **Sprache**: Kotlin 2.x
- **UI**: Jetpack Compose (Material 3)
- **Min SDK**: 26 (Android 8.0) — deckt >95 % ab
- **Target SDK**: 34
- **Persistence**: Jetpack DataStore (Preferences) für Pinned/Hidden Apps
- **Build**: Gradle Kotlin DSL
- **IDE**: Android Studio

---

## Projekt-Struktur

```
launcher/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/de/dm/launcher/
│       │   ├── MainActivity.kt
│       │   ├── ui/
│       │   │   ├── theme/Theme.kt
│       │   │   ├── home/HomeScreen.kt
│       │   │   ├── home/ClockWithBatteryRing.kt
│       │   │   ├── home/PinnedAppsList.kt
│       │   │   ├── home/SystemShortcutsBar.kt
│       │   │   ├── drawer/AppDrawerScreen.kt
│       │   │   ├── drawer/AppListItem.kt
│       │   │   ├── drawer/AzScrollBar.kt
│       │   │   └── drawer/AppActionSheet.kt
│       │   ├── data/
│       │   │   ├── AppRepository.kt          (PackageManager-Wrapper)
│       │   │   ├── PreferencesRepository.kt  (DataStore: pinned/hidden)
│       │   │   └── BatteryProvider.kt        (BroadcastReceiver → Flow)
│       │   └── domain/
│       │       ├── AppInfo.kt
│       │       └── LaunchAppUseCase.kt
│       └── res/
│           └── values/
│               ├── colors.xml  (Pure-Black-Theme)
│               └── strings.xml (de-DE)
├── build.gradle.kts
├── settings.gradle.kts
├── Makefile           (Convenience: `make install`, `make release`)
└── plan.md
```

---

## Phasenplan

### Phase 1 — Projekt-Setup (2–3 h)
1. Android Studio: neues Projekt „Empty Activity (Compose)", Min SDK 26.
2. Dependencies (`app/build.gradle.kts`):
   - `androidx.compose.bom`
   - `androidx.compose.material3`
   - `androidx.compose.foundation` (für `HorizontalPager`)
   - `androidx.datastore:datastore-preferences`
   - `androidx.activity:activity-compose`
3. `AndroidManifest.xml`:
   - MainActivity mit Intent-Filters:
     ```xml
     <intent-filter>
       <action android:name="android.intent.action.MAIN" />
       <category android:name="android.intent.category.HOME" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.LAUNCHER" />
     </intent-filter>
     ```
   - Permission `<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />` (Launcher ist legitimer Use Case).
   - `<queries><intent><action android:name="android.intent.action.MAIN" /></intent></queries>` als Fallback für API 30+.
4. Theme: Material3 Pure-Black-Dark (Background `#000000`, OnBackground `#FFFFFF`).
5. Window-Flags: Fullscreen + `WindowCompat.setDecorFitsSystemWindows(false)`.

### Phase 2 — Home-Screen + Uhr + Akku-Ring (3–4 h)
1. **`BatteryProvider`**: `BroadcastReceiver` für `ACTION_BATTERY_CHANGED`, exponiert als `StateFlow<Int>` (0–100).
2. **`ClockWithBatteryRing` Composable**:
   - State via `produceState` mit `BroadcastReceiver` auf `ACTION_TIME_TICK` (feuert minütlich).
   - Format: `HH:mm` (24h, locale-abhängig) + Zeile darunter `EEEE, d. MMM` (z. B. „Freitag, 29. Mai").
   - `Canvas`: Kreis mit `drawArc`, `sweepAngle = batteryPct/100f * 360f`, `startAngle = -90f`, Stroke ~2 dp.
   - Ring-Farbe: weiß bei ≥20 %, rot/orange bei <20 % (optional).
   - Klickbar → `startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))`.
3. **`HomeScreen` Layout**: `Box` mit drei Slots:
   - oben zentriert: `ClockWithBatteryRing`
   - links oberhalb der Mitte: `PinnedAppsList` (linksbündig, große Schrift wie im Vorbild)
   - unten via `Modifier.align(BottomStart/BottomEnd)`: `SystemShortcutsBar` (Phone links, Kamera rechts)

### Phase 2b — System-Shortcuts (Phone / Kamera) (0.5 h)
1. **`SystemShortcutsBar` Composable**: zwei `IconButton`s mit Material-Icons (`Icons.Default.Call`, `Icons.Default.PhotoCamera`), tint weiß, Größe ~32 dp, Padding ~16 dp zum Rand.
2. Click-Actions via `LaunchAppUseCase.launchIntent`:
   - Phone: `Intent(Intent.ACTION_DIAL)` (öffnet Dialer ohne `CALL_PHONE`-Permission).
   - Kamera: `Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)`.
3. Fallback bei `ActivityNotFoundException`: kurzer Toast „App nicht gefunden".

### Phase 3 — Pinned Apps (1–2 h)
1. **`PreferencesRepository`**: DataStore mit `Set<String>` für pinned Package-Names, Reihenfolge in separater `List<String>`.
2. **`PinnedAppsList` Composable**: lädt aus DataStore + auflöst Labels via `PackageManager.getApplicationLabel()`.
3. Klick → `LaunchAppUseCase`: holt `getLaunchIntentForPackage`, `startActivity`.
4. Edge Case: Pinned App wurde deinstalliert → automatisch aus DataStore entfernen.

### Phase 4 — App-Drawer (Swipe) (3–4 h)
1. Root-Layout: `HorizontalPager(pageCount = 2)`, initial page = 0 (Home).
   - Page 0: `HomeScreen`.
   - Page 1: `AppDrawerScreen`.
   - Swipe nach links wechselt zu Page 1 (Drawer).
2. **`AppRepository.loadLaunchableApps()`**: `queryIntentActivities(Intent(ACTION_MAIN).addCategory(CATEGORY_LAUNCHER), 0)` → Map auf `AppInfo(packageName, label)`. Alphabetisch sortieren (Locale-aware Collator).
3. **`AppDrawerScreen`**:
   - `OutlinedTextField` oben für Suchstring (substring, case-insensitive).
   - `LazyColumn` mit allen passenden Apps.
4. **`AppActionSheet`** (ModalBottomSheet) bei Long-Press auf `AppListItem`:
   - „Zu Favoriten hinzufügen" → Pin (toggle).
   - „Deinstallieren" → `Intent(ACTION_DELETE, Uri.parse("package:$pkg"))`.
   - „App-Info" → `Intent(ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))`.

### Phase 5 — A–Z Quick-Scroll (1–2 h)
1. Eigenes Composable rechts an Drawer: vertikale `Column` mit Buchstaben A–Z.
2. `pointerInput { detectDragGestures }` → berechne Letter aus Y-Position, `LaunchedEffect(letter) { listState.scrollToItem(firstIndexOfLetter) }`.

### Phase 6 — Default-Launcher-Setup (1 h)
1. Beim Start prüfen: `getDefaultHome() == packageName`?
2. Wenn nicht: AlertDialog „Als Standard-Launcher setzen" → `Intent(Settings.ACTION_HOME_SETTINGS)`.
3. Dialog merken sich „nicht wieder fragen" via DataStore-Flag.

### Phase 7 — Polish (1–2 h)
- Haptic Feedback bei Long-Press (`LocalHapticFeedback.current.performHapticFeedback(LongPress)`).
- Sanfte Pager-Animation.
- Status-/Navbar transparent.
- Locale: System-Locale respektieren (Wochentag).
- Empty-States im Drawer (kein Suchtreffer).

### Phase 8 — Build & Distribution (1 h)
1. Debug-APK: `./gradlew installDebug` → Sideload via adb.
2. Release-APK signed: Keystore lokal generieren, `./gradlew assembleRelease`.
3. APK direkt per WhatsApp/USB an Testgeräte verteilen.
4. `Makefile` mit Targets: `make install`, `make release`, `make uninstall`.

---

## Aufwandsschätzung
| Phase | Stunden |
|---|---|
| 1 Setup | 2–3 |
| 2 Clock + Battery | 3–4 |
| 2b System-Shortcuts | 0.5 |
| 3 Pinned Apps | 1–2 |
| 4 App-Drawer | 3–4 |
| 5 A–Z Scroll | 1–2 |
| 6 Default-Setup | 1 |
| 7 Polish | 1–2 |
| 8 Build/Dist | 1 |
| **Summe** | **~13.5–19.5 h** |

---

## Offene Fragen
- [ ] Soll der Akku-Ring bei Ladevorgang animiert pulsieren / Farbe wechseln?
- [ ] Geste für „zurück zu Home" aus dem Drawer — nur Swipe oder auch System-Back?
- [ ] Sprache: nur Deutsch oder mehrsprachig?
- [ ] Schrift: System-Default oder eine spezifische Schriftart (z. B. dünner Sans-Serif wie im Screenshot)?

| Akzeptanzkriterien |
| --- |
| App kann via Android-Settings als Standard-Launcher ausgewählt werden. |
| Beim Home-Button-Druck erscheint zuverlässig die App. |
| Uhrzeit aktualisiert sich minütlich ohne sichtbares Stocken. |
| Akku-Ring zeigt aktuellen Stand innerhalb von 1 s nach Änderung. |
| Klick auf Uhrzeit öffnet System-Wecker. |
| Mind. 5 gepinnte Apps können angeklickt und gestartet werden. |
| Swipe nach links öffnet App-Drawer, Swipe nach rechts schließt ihn. |
| Suchfeld filtert die Liste live. |
| Long-Press öffnet Kontextmenü mit Pin/Uninstall/Info. |
| Telefon-Icon links unten öffnet Dialer, Kamera-Icon rechts unten öffnet Kamera. |
| A–Z-Leiste rechts springt zur jeweiligen Buchstabengruppe. |
| Pinned/Hidden Apps überleben App-Neustart. |
| Release-APK < 8 MB. |
