import java.nio.file.Files
import java.time.Instant

parameter('indexFile', description: 'Index result', type: 'path', required: true)

action('index') {
    Files.writeString(parameter('indexFile'), Instant.now().toString() + '\n')
}

defaults 'index'
