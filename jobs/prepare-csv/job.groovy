import groovy.json.JsonOutput
import java.nio.file.Files

parameter('inputFile', description: 'CSV input', type: 'path', required: true)
parameter('validatedFile', description: 'Validated CSV', type: 'path', required: true)
parameter('reportFile', description: 'Review report', type: 'path', required: true)

action('prepare') {
    println 'J4 branch marker prepare'
    def input = parameter('inputFile')
    long records = Files.newBufferedReader(input).withCloseable { reader ->
        if (reader.readLine() != 'id,name') throw new IllegalArgumentException('Expected CSV header id,name')
        long count = 0
        while (reader.readLine() != null) count++
        if (count == 0) throw new IllegalArgumentException('Expected at least one CSV record')
        count
    }
    Files.copy(input, parameter('validatedFile'))
    Files.writeString(parameter('reportFile'), JsonOutput.toJson([records: records, bytes: Files.size(input)]) + '\n')
}

defaults 'prepare'
