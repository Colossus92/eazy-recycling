package nl.eazysoftware.eazyrecyclingservice.config.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {

    companion object {
        const val COMPANIES_CACHE = "companies"
    }

    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = CaffeineCacheManager()
        cacheManager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(12*60, TimeUnit.MINUTES)
                .maximumSize(100) // Max 100 different query combinations cached
                .recordStats()
        )
        cacheManager.setCacheNames(listOf(COMPANIES_CACHE))
        return cacheManager
    }
}
