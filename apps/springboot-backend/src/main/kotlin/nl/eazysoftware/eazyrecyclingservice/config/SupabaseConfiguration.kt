package nl.eazysoftware.eazyrecyclingservice.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SupabaseConfiguration(
    private val supabaseProperties: SupabaseProperties,
) {

    @Bean
    fun supabaseClient(): SupabaseClient {
        val supabase = createSupabaseClient(
            supabaseUrl = supabaseProperties.url,
            supabaseKey = supabaseProperties.publishable
        ) {
            install(Auth) {
                minimalSettings() //disables session saving and auto-refreshing
            }
            install(Functions)
            install(Storage)
        }

        runBlocking {
            supabase.auth.importAuthToken(supabaseProperties.secret)
        }
        
        return supabase
    }
}