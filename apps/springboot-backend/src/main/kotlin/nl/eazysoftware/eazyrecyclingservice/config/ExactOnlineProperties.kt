package nl.eazysoftware.eazyrecyclingservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "exact.oauth")
data class ExactOnlineProperties(
    var clientId: String = "",
    var clientSecret: String ="",
    var redirectUri: String = "https://app.eazyrecycling.nl/api/admin/exact/callback",
    var authorizationEndpoint: String = "https://start.exactonline.nl/api/oauth2/auth",
    var tokenEndpoint: String = "https://start.exactonline.nl/api/oauth2/token",
    var backfillVatNumbersEnabled: Boolean = true
)
