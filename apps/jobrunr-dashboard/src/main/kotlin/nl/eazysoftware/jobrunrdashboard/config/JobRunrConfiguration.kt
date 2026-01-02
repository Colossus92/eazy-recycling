package nl.eazysoftware.jobrunrdashboard.config

import org.jobrunr.utils.mapper.JsonMapper
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for JobRunr to use Jackson for job serialization.
 *
 * This overrides the default kotlinx.serialization auto-detection from jobrunr-kotlin-2.2-support.
 * The kotlinx.serialization mapper has a known issue where it incorrectly sets the class discriminator
 * to "kotlin.collections.LinkedHashMap" for polymorphic types like JobState, causing deserialization failures.
 *
 * By providing a JsonMapper bean, we force JobRunr to use Jackson instead.
 * See: https://www.jobrunr.io/en/documentation/serialization/jackson2/
 */
@Configuration
class JobRunrConfiguration {

    /**
     * Provide a JsonMapper bean to override JobRunr's auto-detection.
     * This forces JobRunr to use Jackson instead of kotlinx.serialization.
     */
    @Bean
    fun jsonMapper(): JsonMapper {
        return JacksonJsonMapper()
    }
}
