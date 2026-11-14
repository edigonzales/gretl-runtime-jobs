parameter('message', description: 'message', required: true)
parameter('count', description: 'count', type: 'integer', defaultValue: 1)
parameter('uppercase', description: 'uppercase', type: 'boolean', defaultValue: false)

action('greet') {
    println 'J4 branch marker hello'
    int count = parameter('count')
    if (count < 1 || count > 1000) throw new IllegalArgumentException('count must be between 1 and 1000')
    String message = parameter('message')
    if (parameter('uppercase')) message = message.toUpperCase(Locale.ROOT)
    count.times { println message }
}

defaults 'greet'
