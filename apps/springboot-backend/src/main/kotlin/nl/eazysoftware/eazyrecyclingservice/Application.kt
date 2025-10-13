package nl.eazysoftware.eazyrecyclingservice

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

@Component
class StartupLogger {
    private val logger = LoggerFactory.getLogger(StartupLogger::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun logApplicationStartup() {
        val version = "0.0.1"
        logger.info("========================================")
        logger.info("✅ Eazy Recycling Service v{} is ready!", version)
        logger.info("========================================")
    }
}
