package nl.eazysoftware.eazyrecyclingservice.config

import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    /**
     * Register the Hibernate6Module to handle lazy-loaded objects.
     * This approach preserves Spring Boot's default Jackson configuration
     * while adding support for Hibernate proxies.
     */
    @Bean
    fun hibernate6Module(): Hibernate6Module {
        val hibernate6Module = Hibernate6Module()

        // Configure the module to handle lazy-loading
        // Force lazy-loaded properties to be serialized as null instead of failing
        hibernate6Module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false)
        hibernate6Module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true)

        return hibernate6Module
    }
}
