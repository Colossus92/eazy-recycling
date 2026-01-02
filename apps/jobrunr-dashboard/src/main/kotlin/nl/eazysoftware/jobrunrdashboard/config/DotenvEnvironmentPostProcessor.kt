package nl.eazysoftware.jobrunrdashboard.config

import io.github.cdimascio.dotenv.Dotenv
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import java.io.File

/**
 * Loads .env file before Spring processes configuration.
 * This EnvironmentPostProcessor runs very early in the Spring boot lifecycle,
 * ensuring environment variables are available for all configuration classes.
 */
class DotenvEnvironmentPostProcessor : EnvironmentPostProcessor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        try {
            val projectRoot = System.getProperty("user.dir")
            log.info("Current working directory: $projectRoot")
            val envFile = File(projectRoot, ".env")

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
