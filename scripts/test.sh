#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
runtime="${GRETL_BIN:-../gretl-runtime/gretl-dist/target/dist/bin/gretl}"
temp=$(mktemp -d)
trap 'rm -rf "$temp"' EXIT
run() { "$runtime" run "$@" > "$temp/run.log" 2>&1 || { cat "$temp/run.log" >&2; return 1; }; }
expect_failure() {
  if "$runtime" run "$@" > "$temp/run.log" 2>&1; then
    echo 'Expected GRETL to reject the input' >&2
    exit 1
  fi
}
run jobs/hello-gretl/job.groovy --param message=Hello
[[ $(grep -Ec 'Script message.*: Hello phase=script' "$temp/run.log") == 1 ]]
run jobs/hello-gretl/job.groovy --param message=Hello --param count=2 --param uppercase=true
[[ $(grep -Ec 'Script message.*: HELLO phase=script' "$temp/run.log") == 2 ]]
expect_failure jobs/hello-gretl/job.groovy --param message=Hello --param count=invalid
grep -Eqi 'count' "$temp/run.log"
expect_failure jobs/hello-gretl/job.groovy
grep -Eqi 'message' "$temp/run.log"
run jobs/csv-analyzer/job.groovy --param "inputFile=$PWD/testdata/sample.csv"
grep -Eq 'records=3 columns=2' "$temp/run.log"
printf '\r\na;b\r\n\r\n"x;y";z\r\n\r\n' > "$temp/semicolon.csv"
run jobs/csv-analyzer/job.groovy --param "inputFile=$temp/semicolon.csv" --param 'delimiter=;' --param hasHeader=false
grep -Eq 'records=2 columns=2' "$temp/run.log"
printf 'a,b\n"broken\n' > "$temp/invalid.csv"
expect_failure jobs/csv-analyzer/job.groovy --param "inputFile=$temp/invalid.csv"
grep -Eq 'Unclosed CSV quote' "$temp/run.log"
run jobs/scheduled-report/job.groovy
grep -Eq '[0-9]{4}-[0-9]{2}-[0-9]{2}T[^ ]+Z Zeitbericht gestartet' "$temp/run.log"
run jobs/prepare-csv/job.groovy --param "inputFile=$PWD/testdata/publication.csv" \
  --param "validatedFile=$temp/validated.csv" --param "reportFile=$temp/review.json"
python3 -c 'import json,sys; assert json.load(open(sys.argv[1]))["records"] == 3' "$temp/review.json"
run jobs/publish-csv/job.groovy --param "inputFile=$temp/validated.csv" --param 'publicationNote=Geprüft' \
  --param "publishedFile=$temp/published.csv" --param "manifestFile=$temp/publication.json"
cmp testdata/publication.csv "$temp/published.csv"
python3 -c 'import json,sys; assert json.load(open(sys.argv[1]))["note"] == "Geprüft"' "$temp/publication.json"
run jobs/update-index/job.groovy --param "indexFile=$temp/index.txt"
test -s "$temp/index.txt"
echo 'GRETL example jobs passed.'
