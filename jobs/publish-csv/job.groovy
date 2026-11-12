import groovy.json.JsonOutput
import java.nio.file.Files

parameter('inputFile', description: 'Validated CSV', type: 'path', required: true)
parameter('publicationNote', description: 'Publication note', required: true)
parameter('publishedFile', description: 'Published CSV', type: 'path', required: true)
parameter('manifestFile', description: 'Publication manifest', type: 'path', required: true)

action('publish') {
    Files.copy(parameter('inputFile'), parameter('publishedFile'))
    Files.writeString(parameter('manifestFile'), JsonOutput.toJson([note: parameter('publicationNote')]) + '\n')
}

defaults 'publish'
