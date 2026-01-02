package nl.eazysoftware.jobrunrdashboard

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.sql.DataSource

@SpringBootApplication
class JobrunrDashboardApplication

@Component
class DataSourceLogger(private val dataSource: DataSource) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun logDataSourceUrl() {
        val connection = dataSource.connection
        val url = connection.metaData.url
        connection.close()
        logger.info("Connected to database: $url")
    }
}

fun main(args: Array<String>) {
    runApplication<JobrunrDashboardApplication>(*args)
}
