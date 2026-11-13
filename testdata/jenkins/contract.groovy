import java.nio.file.Files

parameter('mode', description: 'Synthetic acceptance scenario')
parameter('output', type: 'path', description: 'Optional test output')
parameter('firstFile', type: 'path', description: 'First synthetic upload')
parameter('secondFile', type: 'path', description: 'Second synthetic upload')

action('fixture') {
    switch (parameter('mode')) {
        case 'fail':
            throw new IllegalArgumentException('Intentional acceptance fixture failure')
        case 'wait':
            println 'ACCEPTANCE_PROCESS_STARTED'
            Thread.sleep(120_000L)
            println 'ACCEPTANCE_PROCESS_FINISHED'
            break
        case 'files':
            Files.writeString(parameter('output'),
                Files.readString(parameter('firstFile')) + Files.readString(parameter('secondFile')))
            break
        default:
            println 'ACCEPTANCE_SUCCESS'
    }
}
defaults 'fixture'
