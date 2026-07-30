# Änderungsprotokoll

## In Arbeit

### Behoben

- Das erste Tippen auf eine Ziffer ersetzt die anfängliche `0` nun auch dann, wenn Android den Cursor vor die `0` gesetzt hat.
- Führende Nullen werden beim Tippen korrekt ersetzt: Aus `0` plus `7` wird `7`, auch direkt hinter einem Operator.
- Wiederholtes Tippen auf `0` erzeugt keine ungültigen Zahlen wie `00` oder `000`.
- Ein Operator kann an jeder Stelle seines sichtbaren Drei-Zeichen-Bereichs sicher ersetzt werden, ohne die vorherige Zahl zu beschädigen.
- Die Rücktaste entfernt einen Operator mitsamt seinen Leerzeichen als eine Einheit.
- Das Prozentzeichen wird nur an einer gültigen Position am Ende der aktuellen Zahl eingefügt.
- Unvollständige oder beschädigte Ausdrücke werden vom Rechenkern ausdrücklich abgelehnt.

### Technisch

- Eingabebearbeitung und Rechenlogik aus `MainActivity` in `CalculatorCore` ausgelagert.
- Automatische Tests für führende Nullen vor und hinter dem Cursor, Cursorbearbeitung, Operatoren, Prozentrechnung, Rechenrangfolge, Dezimalgenauigkeit und Division durch null hinzugefügt.
- GitHub Actions führt die Tests vor dem APK-Build aus.
- Öffentliche Projektbeschreibung ergänzt.
- Kleine Versionsanzeige oben ergänzt.
- Klickbare Signatur von Serge Rosberg mit Kontaktadresse am unteren Rand ergänzt.
- Dauerhafte Release-Signierung für zukünftige, direkt installierbare Updates vorbereitet.

## 1.x – April bis Mai 2026

- Präzise Berechnungen mit `BigDecimal`
- Live-Ergebnisvorschau
- Bearbeitbarer Ausdruck mit rotem Cursor
- Mehrzeilige Anzeige für lange Ausdrücke
- Deutsche Darstellung mit Dezimalkomma
