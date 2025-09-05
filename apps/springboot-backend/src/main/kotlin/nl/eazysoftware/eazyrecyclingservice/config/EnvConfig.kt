package nl.eazysoftware.eazyrecyclingservice.config

import io.github.cdimascio.dotenv.Dotenv
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

@Profile("local")
@Configuration
class EnvConfig(private val environment: ConfigurableEnvironment) {

    private val log = org.slf4j.LoggerFactory.getLogger(EnvConfig::class.java)

    @PostConstruct
    fun init() {
            val dotenv = Dotenv.configure()
                .load()

            val propertySource = MapPropertySource(
                "dotenvProperties",
                dotenv.entries().associate { it.key to it.value }
            )

            environment.propertySources.addFirst(propertySource)
    }
}
