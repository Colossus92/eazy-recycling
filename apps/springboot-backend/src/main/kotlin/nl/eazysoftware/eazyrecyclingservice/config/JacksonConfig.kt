package nl.eazysoftware.eazyrecyclingservice.config

import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfig {

    /**
     * Register the Hibernate5JakartaModule to handle lazy-loaded objects.
     * This approach preserves Spring Boot's default Jackson configuration
     * while adding support for Hibernate proxies.
     */
    @Bean
    fun hibernate5Module(): Hibernate5JakartaModule {
        val hibernate5Module = Hibernate5JakartaModule()

        // Configure the module to handle lazy-loading
        // Force lazy-loaded properties to be serialized as null instead of failing
        hibernate5Module.configure(Hibernate5JakartaModule.Feature.FORCE_LAZY_LOADING, false)
        hibernate5Module.configure(Hibernate5JakartaModule.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true)

        return hibernate5Module
    }
}
