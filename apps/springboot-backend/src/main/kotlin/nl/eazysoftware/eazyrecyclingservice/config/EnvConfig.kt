package nl.eazysoftware.eazyrecyclingservice.config

import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

/**
 * Loads .env file before Spring processes @Value annotations.
 * This EnvironmentPostProcessor runs very early in the Spring boot lifecycle,
 * ensuring environment variables are available for all configuration classes.
 *
 * Only active for the 'local' profile.
 */
class DotenvEnvironmentPostProcessor : EnvironmentPostProcessor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        // Only load .env for local profile
        val activeProfiles = environment.activeProfiles
        if (!activeProfiles.contains("local")) {
            log.debug("Not loading .env file - 'local' profile not active. Active profiles: ${activeProfiles.joinToString()}")
            return
        }

        try {
            // Try to load .env from project root (two levels up from springboot-backend)
            val projectRoot = System.getProperty("user.dir")
            log.info("Current working directory: $projectRoot")

            // Check if we're in the apps/springboot-backend directory
            val envFile = if (projectRoot.endsWith("springboot-backend")) {
                java.io.File(projectRoot, "../../.env")
            } else {
                java.io.File(projectRoot, ".env")
            }

            log.info("Looking for .env file at: ${envFile.absolutePath}")

            val dotenv = if (envFile.exists()) {
                log.info(".env file found, loading environment variables")
                Dotenv.configure()
                    .directory(envFile.parent)
                    .load()
            } else {
                log.warn(".env file not found at ${envFile.absolutePath}, using system environment variables only")
                Dotenv.configure()
                    .ignoreIfMissing()
                    .load()
            }

            val propertySource = MapPropertySource(
                "dotenvProperties",
                dotenv.entries().associate { it.key to it.value }
            )

            environment.propertySources.addFirst(propertySource)
            log.info("Loaded ${dotenv.entries().size} environment variables from .env")
        } catch (e: Exception) {
            log.error("Failed to load .env file", e)
        }
    }
}
