import groovy.transform.CompileStatic
import java.nio.file.Files

parameter('inputFile', description: 'inputFile', type: 'path', required: true)
parameter('delimiter', description: 'delimiter', defaultValue: ',')
parameter('hasHeader', description: 'hasHeader', type: 'boolean', defaultValue: true)

@CompileStatic
class CsvShape {
    long rows
    int columns

    void read(BufferedReader reader, int delimiter) {
        int fields = 1
        boolean fieldStart = true
        boolean quoted = false
        boolean quoteClosed = false
        boolean recordStarted = false
        boolean skipLF = false
        int c
        while ((c = reader.read()) != -1) {
            if (skipLF) {
                skipLF = false
                if (c == 10) continue
            }
            if (!recordStarted && (c == 10 || c == 13)) {
                skipLF = c == 13
                continue
            }
            recordStarted = true
            if (quoted) {
                if (c == 34) {
                    quoted = false
                    quoteClosed = true
                }
                continue
            }
            if (quoteClosed && c == 34) {
                quoted = true
                quoteClosed = false
                continue
            }
            if (c == delimiter) {
                fields++
                fieldStart = true
                quoteClosed = false
            } else if (c == 10 || c == 13) {
                record(fields)
                fields = 1
                fieldStart = true
                quoteClosed = false
                recordStarted = false
                skipLF = c == 13
            } else if (c == 34 && fieldStart) {
                quoted = true
                fieldStart = false
            } else {
                if (quoteClosed || c == 34) throw new IllegalArgumentException('Invalid CSV quoting')
                fieldStart = false
            }
        }
        if (quoted) throw new IllegalArgumentException('Unclosed CSV quote')
        if (recordStarted) record(fields)
        if (rows == 0) throw new IllegalArgumentException('Empty CSV')
    }

    private void record(int fields) {
        if (rows == 0) columns = fields
        if (columns != fields) throw new IllegalArgumentException("Invalid column count at record ${rows + 1}")
        rows++
    }
}

action('analyze') {
    String delimiter = parameter('delimiter')
    if (!(delimiter in [',', ';', '\t'])) throw new IllegalArgumentException('Invalid delimiter')
    def shape = new CsvShape()
    Files.newBufferedReader(parameter('inputFile')).withCloseable { reader ->
        shape.read(reader, (int) delimiter.charAt(0))
    }
    long records = shape.rows - (parameter('hasHeader') ? 1 : 0)
    println "records=${records} columns=${shape.columns}"
}

defaults 'analyze'
