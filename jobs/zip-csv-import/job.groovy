import groovy.json.JsonOutput
import java.nio.file.Files
import java.util.zip.ZipFile

parameter('inputFile', type: 'path', required: true, description: 'Archive of two CSV tables')
parameter('mergedFile', type: 'path', required: true, description: 'Joined CSV')
parameter('reportFile', type: 'path', required: true, description: 'Import report')

action('unpack') {
    def names = ['municipalities.csv', 'population.csv']
    def folder = workPath('tables')
    Files.createDirectories(folder)
    new ZipFile(parameter('inputFile').toFile()).withCloseable { zip ->
        if (zip.size() != names.size()) throw new IllegalArgumentException('Expected exactly two CSV entries')
        names.each { name ->
            def entry = zip.getEntry(name)
            int limit = 2 * 1024 * 1024
            if (!entry || entry.directory || entry.size < 0 || entry.size > limit) {
                throw new IllegalArgumentException("Missing or oversized entry: ${name}")
            }
            zip.getInputStream(entry).withCloseable { input ->
                byte[] bytes = input.readNBytes(limit + 1)
                if (bytes.length > limit || bytes.length != entry.size) {
                    throw new IllegalArgumentException("Invalid uncompressed size: ${name}")
                }
                Files.write(folder.resolve(name), bytes)
            }
        }
    }
    return folder
}

Map table(path, header, valuePattern) {
    def rows = [:]
    Files.newBufferedReader(path).withCloseable { reader ->
        if (reader.readLine() != header) throw new IllegalArgumentException("Invalid header in ${path.fileName}")
        reader.eachLine { line ->
            def row = line.split(',', -1)
            if (row.length != 2 || !(row[0] ==~ /[0-9]{4}/) || !(row[1] ==~ valuePattern) ||
                    rows.putIfAbsent(row[0], row[1]) != null) {
                throw new IllegalArgumentException("Invalid or duplicate row in ${path.fileName}")
            }
        }
    }
    return rows
}

action('merge', dependsOn: 'unpack') {
    def folder = result('unpack')
    def municipalities = this.table(folder.resolve('municipalities.csv'), 'id,name', /[\p{L} .'-]{1,100}/)
    def population = this.table(folder.resolve('population.csv'), 'id,population', /[0-9]{1,7}/)
    if (!municipalities || municipalities.keySet() != population.keySet()) {
        throw new IllegalArgumentException('The two tables must contain the same non-empty set of IDs')
    }
    Files.newBufferedWriter(parameter('mergedFile')).withCloseable { out ->
        out.write('id,name,population\n')
        municipalities.each { id, name -> out.write("${id},${name},${population[id]}\n") }
    }
    Files.writeString(parameter('reportFile'), JsonOutput.toJson([records: municipalities.size(), tables: 2]) + '\n')
}
defaults 'merge'
