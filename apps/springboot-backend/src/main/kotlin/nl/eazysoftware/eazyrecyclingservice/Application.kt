package nl.eazysoftware.eazyrecyclingservice

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import java.util.*

@SecurityScheme(
  name = "bearerAuth",
  type = SecuritySchemeType.HTTP,
  bearerFormat = "JWT",
  `in` = SecuritySchemeIn.HEADER,
  paramName = "Authorization",
  scheme = "bearer"
)
@OpenAPIDefinition(
  info = Info(title = "Eazy Recycling Backend Service", version = "0.0.1"),
  security = [SecurityRequirement(name = "bearerAuth")]
)
@EnableScheduling
@EnableRetry
@SpringBootApplication
class Application {
    @PostConstruct
    fun init() {
        // Set JVM default timezone to UTC for consistency
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }
}

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
