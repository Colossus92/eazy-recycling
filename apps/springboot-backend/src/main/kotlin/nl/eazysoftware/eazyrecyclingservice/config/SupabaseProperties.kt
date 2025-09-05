package nl.eazysoftware.eazyrecyclingservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "supabase.api")
data class SupabaseProperties(
        var url: String = "",
        var secret: String = "",
        var publishable: String = ""
    )
