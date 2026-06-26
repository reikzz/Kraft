package org.kraft.server

/**
 * Entry point for starting the dedicated Kraft multiplayer server.
 * Runs headlessly without graphics context.
 */
fun main() {
    val server = GameServer(25565)
    
    Runtime.getRuntime().addShutdownHook(Thread({
        println("Shutting down dedicated server...")
        server.stop()
    }, "ServerShutdownHook"))

    server.start()
}
