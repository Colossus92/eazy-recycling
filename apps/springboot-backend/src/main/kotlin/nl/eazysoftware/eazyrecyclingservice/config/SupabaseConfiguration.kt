package nl.eazysoftware.eazyrecyclingservice.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.storage.Storage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SupabaseConfiguration(
    private val supabaseProperties: SupabaseProperties,
) {

    private val logger: Logger = LoggerFactory.getLogger(SupabaseConfiguration::class.java)

    @Bean
    fun supabaseClient(): SupabaseClient {
        logger.info("Supabase URL: ${supabaseProperties.url}")
        return createSupabaseClient(
            supabaseUrl = supabaseProperties.url,
            supabaseKey = supabaseProperties.secret
        ) {
            install(Auth) {
                minimalSettings() //disables session saving and auto-refreshing
            }
            install(Functions)
            install(Storage)
        }
    }
}
