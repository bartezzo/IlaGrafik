# IlaGrafik

Aplikacja na Androida napisana w Kotlinie z użyciem Jetpack Compose i Material 3.

Ekran startowy pokazuje obrazek z grafikiem. Można go przybliżać dwoma palcami
lub dwukrotnym stuknięciem. Przycisk **Zmień grafik** pozwala wybrać nowy obrazek
z pamięci telefonu; jest on kopiowany do pamięci aplikacji i zostaje po jej ponownym uruchomieniu.

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

## Struktura

```
app/src/main/java/pl/bartezzo/ilagrafik/
├── MainActivity.kt             # ekran startowy z grafikiem
├── data/GrafikRepository.kt    # zapis i odczyt obrazka z grafikiem
└── ui/
    ├── ZoomableImage.kt        # obrazek z przybliżaniem
    └── theme/Theme.kt          # motyw Material 3
```
