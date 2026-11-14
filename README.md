# GRETL Runtime Jobs

Ausführbare GRETL-6-Beispiele für die Kubernetes Job Console. Jede Definition
liegt in `jobs/<id>/job.yaml` und verweist auf ein Groovy-Script im selben
Verzeichnis. Die generische GRETL-Runtime führt das Script aus.

| Job | Eingaben | Ergebnis |
| --- | --- | --- |
| `hello-gretl` | Nachricht, Wiederholungen, Grossbuchstaben | Nachricht im Log |
| `csv-analyzer` | UTF-8-CSV, Trennzeichen, Kopfzeile | JSON-Bericht mit Anzahl Datensätze und Spalten |
| `scheduled-report` | Optionale Nachricht | UTC-Zeitpunkt und Indexmarkierung aus einem Folgejob |
| `csv-publication` | Beispiel-CSV, Publikationshinweis bei Freigabe | Prüfbericht und publizierte Datei |

Für einen weiteren Job kopiere eine passende Definition und vergebe eine
eindeutige ID. `schemaVersion: 2`, `runtime: gretl` und `script: job.groovy`
beschreiben das Format. In `parameters` erhält jeder Eintrag `name`, `label`
und `type`, bei Bedarf `required` oder `default`. Unterstützt sind `text`,
`multiline`, `integer`, `boolean`, `date` (JJJJ-MM-TT), `enum` (mit
`options`) und `file` (mit `accept`, etwa `[.csv]`). Die Scriptparameter
müssen dieselben Namen haben. Dateiparameter sind in der GRETL-DSL vom Typ
`path`. `outputs` deklariert Ergebnisdateien relativ zu `/outputs` und
deren systemseitige Pfadparameter. `access` enthält pro Aktion exakte
Benutzer- oder Gruppenregeln. Hinterlege fachliche Secrets über die GRETL-Secret-Konfiguration.

Ein Zeitplan verwendet fünf Cron-Felder. Die Job Console löst `H`,
`H(1-3)` und `H/15` in Minuten und Stunden stabil anhand der Job-ID auf.
Geplante Jobs brauchen Vorgaben für alle Pflichtparameter. Dateiuploads sind
manuellen Ausführungen vorbehalten. `Europe/Zurich` ist die Standardzeitzone.

Der Ablauf `flows/csv-publication/flow.yaml` nutzt
`testdata/publication.csv` mit der Kopfzeile `id,name`. Er speichert den
Prüfbericht und die geprüfte Datei pro Ausführung. Die Publikation
verwendet genau diese Datei und den vom Reviewer angegebenen Hinweis.
`prepare-csv`, `publish-csv` und `update-index` sind interne Schritte.
`scheduled-report` startet `update-index` nach Erfolg im selben Workflow.

```sh
# Mit einer gebauten GRETL-Distribution im Schwesterverzeichnis
./scripts/test.sh

# Beispiel direkt aufrufen
../gretl-runtime/gretl-dist/target/dist/bin/gretl run jobs/csv-analyzer/job.groovy \
  --param "inputFile=$PWD/testdata/sample.csv"
```

`GRETL_BIN` kann auf eine andere gebaute GRETL-6-Distribution zeigen.
Für die Bereitstellung siehe die
[Kubernetes Job Console](https://github.com/edigonzales/k8s-job-console).

### Weitere Beispiele

| Job | Eingabe | Ergebnis |
| --- | --- | --- |
| `csv-quality-check` | `testdata/municipalities.csv`, Trennzeichen, Berichtsjahr, Stichtag und Bemerkungen | Qualitätsbericht, gültige Zeilen und bei Bedarf beanstandete Zeilen |
| `zip-csv-import` | ZIP mit `municipalities.csv` (`id,name`) und `population.csv` (`id,population`) | Verknüpfte Tabelle und Importbericht |
| `xtf-validation` | `testdata/valid.xtf` oder `testdata/invalid.xtf` und lokales Modell | Validierungsbericht bei Erfolg, Runtime-Runbericht mit Diagnose bei Fehler |

`./scripts/test.sh` enthält reproduzierbare direkte CLI-Aufrufe für alle
Beispiele und erzeugt das ZIP als kleine synthetische Testdatei. Beim
CSV-Qualitätscheck erzeugt der strikte Modus zuerst den Qualitätsbericht und
beendet danach die Ausführung mit einem Fehler, falls Zeilen beanstandet wurden.
Modell und XTF-Fixtures stammen aus dem GRETL-Runtime-Beispiel
`ili-validator` an der in der CI gepinnten Revision.

Im [Jenkins-Labor](https://github.com/edigonzales/gretl-jenkins-lab) sind die
öffentlichen Beispiele nach dem Katalog-Deployment als `gretl-<id>` verfügbar.
Wähle **Build with Parameters**, lade die Beispieldatei hoch und öffne die
Ergebnisse unter **Build Artifacts**. Bei `gretl-csv-publication` liest der
Reviewer vor der Freigabe `prepare/review.json` und erfasst den Hinweis über
**Paused for Input**. Das publizierte Ergebnis liegt unter `publish/`.
