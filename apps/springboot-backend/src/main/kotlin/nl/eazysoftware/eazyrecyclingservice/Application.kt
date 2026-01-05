package nl.eazysoftware.eazyrecyclingservice

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
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
@SpringBootApplication(
    exclude = [org.springframework.boot.autoconfigure.webservices.WebServicesAutoConfiguration::class]
)
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
