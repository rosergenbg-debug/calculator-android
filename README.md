# Taschenrechner für Android

Ein schlanker, werbefreier Taschenrechner für Android. Die App arbeitet vollständig offline und benötigt keine Internetberechtigung.

## Funktionen

- Grundrechenarten mit korrekter Operatorrangfolge
- Prozentrechnung wie bei einem klassischen Taschenrechner
- Exakte Dezimalrechnung mit `BigDecimal`
- Live-Vorschau des Ergebnisses
- Bearbeiten einer Zahl über den sichtbaren Cursor
- Deutsche Dezimalkommas und deutsche Fehlermeldung
- Bis zu 50 Ziffern pro Zahl und 30 signifikante Stellen bei Berechnungen
- Android 7.0 oder neuer

## Installation

Die jeweils geprüfte APK wird über GitHub Actions gebaut. Bei Android muss für ein Update sowohl die `applicationId` als auch der Signaturschlüssel mit der bereits installierten Version übereinstimmen.

## Entwicklung

```text
MainActivity.kt       Android-Oberfläche und Ereignisse
CalculatorCore.kt     Eingabeprüfung und Berechnungslogik
CalculatorCoreTest.kt automatische Tests
```

Lokale Prüfung mit installiertem Gradle 8.4 und JDK 17:

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Datenschutz

Die App verarbeitet keine persönlichen Daten, zeigt keine Werbung und greift nicht auf das Internet zu.

## Projektstatus

Das Projekt ist klein und bewusst übersichtlich gehalten. Aktuelle Änderungen und behobene Fehler stehen in [CHANGELOG.md](CHANGELOG.md).
