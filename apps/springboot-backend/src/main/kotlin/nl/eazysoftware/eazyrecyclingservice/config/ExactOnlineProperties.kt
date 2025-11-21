package nl.eazysoftware.eazyrecyclingservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "exact.oauth")
data class ExactOnlineProperties(
    var clientId: String = "",
    var clientSecret: String ="",
    var redirectUri: String = "https://app.eazyrecycling.nl",
    var authorizationEndpoint: String = "https://start.exactonline.nl/api/oauth2/auth",
    var tokenEndpoint: String = "https://start.exactonline.nl/api/oauth2/token"
) {
  init {
      if (clientId.isEmpty() || clientSecret.isEmpty()) {
          throw IllegalArgumentException("Exact Online client ID and secret must be provided")
      }
  }
}
