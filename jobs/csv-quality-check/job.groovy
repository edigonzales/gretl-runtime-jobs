import ch.ehi.basics.settings.Settings
import ch.interlis.iom_j.csv.CsvReader
import ch.interlis.iox.ObjectEvent
import groovy.json.JsonOutput
import java.nio.file.Files

parameter('inputFile', type: 'path', required: true, description: 'UTF-8 CSV')
parameter('delimiter', defaultValue: ',', description: 'CSV delimiter')
parameter('hasHeader', type: 'boolean', defaultValue: true, description: 'Header row')
parameter('expectedYear', type: 'integer', defaultValue: 2025, description: 'Reporting year')
parameter('datasetName', defaultValue: 'Gemeindedaten', description: 'Dataset name')
parameter('strictMode', type: 'boolean', defaultValue: false, description: 'Fail on invalid records')
parameter('referenceDate', defaultValue: '2025-12-31', description: 'Reporting date')
parameter('notes', description: 'Multiline review notes')
parameter('reportFile', type: 'path', required: true, description: 'Quality report')
parameter('validatedFile', type: 'path', required: true, description: 'Valid records')
parameter('invalidFile', type: 'path', required: true, description: 'Invalid records when present')

String csv(List fields) {
    fields.collect { value -> '"' + (value ?: '').toString().replace('"', '""') + '"' }.join(',') + '\n'
}

action('inspect') {
    def columns = ['municipality_id', 'year', 'population']
    def report = [records: 0, blankValues: 0, duplicateKeys: 0, invalidRecords: 0]
    def keys = [] as Set
    def settings = new Settings()
    settings.setValue(CsvReader.ENCODING, 'UTF-8')
    String delimiter = parameter('delimiter')
    if (!(delimiter in [',', ';'])) throw new IllegalArgumentException('Unsupported delimiter')
    def reader = new CsvReader(parameter('inputFile').toFile(), settings)
    reader.setFirstLineIsHeader(parameter('hasHeader'))
    reader.setValueSeparator(delimiter.charAt(0))
    reader.setValueDelimiter('"' as char)
    try {
        def accepted = parameter('validatedFile')
        def rejected = parameter('invalidFile')
        Files.newBufferedWriter(accepted).withCloseable { valid ->
            valid.write(this.csv(columns))
            def event = reader.read()
            event = reader.read()
            def headers = reader.attributes?.toList()
            if (headers?.size() != columns.size() || (parameter('hasHeader') && headers != columns)) {
                throw new IllegalArgumentException("Expected columns: ${columns.join(', ')}")
            }
            while ((event = reader.read()) != null) {
                if (!(event instanceof ObjectEvent)) continue
                def row = headers.collect { event.iomObject.getattrvalue(it) }
                report.records++
                def blanks = row.count { !it?.trim() }
                report.blankValues += blanks
                boolean duplicate = row[0]?.trim() && !keys.add(row[0])
                if (duplicate) report.duplicateKeys++
                def problems = []
                if (blanks) problems << 'blank'
                if (duplicate) problems << 'duplicate key'
                if (!(row[0] ==~ /[0-9]{4}/)) problems << 'invalid municipality ID'
                if (row[1] != parameter('expectedYear').toString()) problems << 'unexpected year'
                if (!(row[2] ==~ /[0-9]{1,7}/)) problems << 'invalid population'
                if (problems) {
                    if (!report.invalidRecords++) Files.writeString(rejected, this.csv(columns + 'reason'))
                    Files.writeString(rejected, this.csv(row + problems.join('; ')), java.nio.file.StandardOpenOption.APPEND)
                } else {
                    valid.write(this.csv(row))
                }
            }
        }
    } finally {
        reader.close()
    }
    return report
}

action('report', dependsOn: 'inspect') {
    def summary = result('inspect')
    Files.writeString(parameter('reportFile'), JsonOutput.prettyPrint(JsonOutput.toJson(summary + [
        dataset: parameter('datasetName'), referenceDate: parameter('referenceDate'), notes: parameter('notes')
    ])) + '\n')
    if (parameter('strictMode') && summary.invalidRecords) {
        throw new IllegalArgumentException("Rejected ${summary.invalidRecords} records")
    }
}
defaults 'report'
