import java.time.Instant

parameter('message', description: 'message', defaultValue: 'Zeitbericht gestartet')

action('report') {
    println "${Instant.now()} ${parameter('message')}"
}

defaults 'report'
