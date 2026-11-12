# GRETL Runtime Jobs

Drei ausführbare GRETL-6-Jobs für die Kubernetes Job Console. Jede Definition
liegt in `jobs/<id>/job.yaml` und verweist auf ein Groovy-Script im selben
Verzeichnis. Die generische GRETL-Runtime führt das Script aus.

| Job | Eingaben | Ergebnis |
| --- | --- | --- |
| `hello-gretl` | Nachricht, Wiederholungen, Grossbuchstaben | Nachricht im Log |
| `csv-analyzer` | UTF-8-CSV, Trennzeichen, Kopfzeile | JSON-Bericht mit Anzahl Datensätze und Spalten |
| `scheduled-report` | Optionale Nachricht | Nachricht und UTC-Zeitpunkt im Log |

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
