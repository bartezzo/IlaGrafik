# IlaGrafik

Aplikacja na Androida napisana w Kotlinie z użyciem Jetpack Compose i Material 3.

Ekran startowy pokazuje obrazek z grafikiem. Można go przybliżać dwoma palcami
lub dwukrotnym stuknięciem. Przycisk **Zmień grafik** pozwala wybrać nowy obrazek
z pamięci telefonu; jest on kopiowany do pamięci aplikacji i zostaje po jej ponownym uruchomieniu.

Po wczytaniu grafiku aplikacja rozpoznaje na nim tekst (Google ML Kit, bez internetu),
odszukuje nazwę dzisiejszego dnia tygodnia (np. „Poniedziałek”) i automatycznie przybliża
widok na jego kafelek, czyli wiersz tabeli między poziomymi liniami. Przycisk z kalendarzem
wraca do dzisiejszego dnia po ręcznym przesuwaniu.

## Wymagania

- Android Studio (Ladybug lub nowsze)
- JDK 17
- Android SDK 35, minimalna wersja systemu: Android 8.0 (API 26)

## Uruchomienie

1. Otwórz folder projektu w Android Studio.
2. Poczekaj na synchronizację Gradle.
3. Uruchom konfigurację `app` na emulatorze lub telefonie.

Z wiersza poleceń:

```bash
./gradlew assembleDebug
```

Plik APK pojawi się w `app/build/outputs/apk/debug/`.

## CI

Każdy PR i push do `main` uruchamia workflow **Android build** (GitHub Actions),
który uruchamia testy jednostkowe i buduje wersję debug. Gotowy plik APK można pobrać z zakładki **Actions**
→ wybrane uruchomienie → sekcja **Artifacts** (przechowywany 14 dni).

## Struktura

```
app/src/main/java/pl/bartezzo/ilagrafik/
├── MainActivity.kt             # ekran startowy z grafikiem
├── data/GrafikRepository.kt    # zapis i odczyt obrazka z grafikiem
├── data/DayTileLocator.kt      # wyszukiwanie kafelka dnia (nazwy dni, linie tabeli)
├── data/TodayTileFinder.kt     # rozpoznawanie tekstu ML Kit + DayTileLocator
└── ui/
    ├── ZoomableImage.kt        # obrazek z przybliżaniem i przybliżaniem na kafelek
    └── theme/Theme.kt          # motyw Material 3
```
