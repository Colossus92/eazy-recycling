package nl.eazysoftware.eazyrecyclingservice.adapters.out.pricingapp

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@ConfigurationProperties(prefix = "pricing-app")
data class PricingAppProperties(
    val baseUrl: String,
    val bearerToken: String
)

@Configuration
class PricingAppConfig {

    @Bean
    fun pricingAppRestTemplate(): RestTemplate {
        return RestTemplate()
    }
}
